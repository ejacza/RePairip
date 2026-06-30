package com.repairip.app

import android.os.Environment
import com.antik.DexPatcher.DexPatcher
import com.antik.DexPatcher.Translation.TranslationPatcher
import com.antik.crc32.crc32
import com.antik.manifest.manifestP
import com.antik.utils.deleteDir
import com.reandroid.apk.ApkBundle
import com.reandroid.apk.ApkModule
import com.reandroid.archive.Archive
import com.reandroid.archive.InputSource
import com.reandroid.archive.RenamedInputSource
import com.reandroid.archive.WriteProgress
import com.reandroid.archive.io.ArchiveFileEntrySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

object Dumper {
    private val outDir = File(Environment.getExternalStorageDirectory(), "RePairip")

    fun ensureDir() { outDir.mkdirs() }

    suspend fun dump(apkPath: String, pkg: String, splits: Array<String>?, onProgress: (Int) -> Unit = {}): String = withContext(Dispatchers.IO) {
        ensureDir()
        val appOut = File(outDir, pkg)
        appOut.mkdirs()
        val input = File(apkPath)
        val isSplit = apkPath.endsWith(".apks") || apkPath.endsWith(".xapk") || (splits != null && splits.isNotEmpty())

        onProgress(0)

        // step 1: load / merge
        val (module, originalFile) = if (isSplit) {
            val tmpDir = createTempDir("rmg_")
            try {
                if (apkPath.endsWith(".apks") || apkPath.endsWith(".xapk")) {
                    com.antik.AntikUtils.ex_apks(input, tmpDir)
                } else {
                    // installed app with splits: copy base + split APKs
                    input.copyTo(File(tmpDir, "base.apk"), overwrite = true)
                    splits?.forEach { sp ->
                        File(sp).copyTo(File(tmpDir, File(sp).name), overwrite = true)
                    }
                }
                val bundle = ApkBundle()
                bundle.loadApkDirectory(tmpDir)

                // write merged original for CRC reference
                val m = bundle.mergeModules()
                manifestP.patch(m)
                val mergedOri = File(appOut, "${pkg}_merged.apk")
                m.writeApk(mergedOri, WriteProgress { _, _, _ -> })
                Pair(m, mergedOri)
            } finally {
                deleteDir.del_dir(tmpDir)
            }
        } else {
            Pair(ApkModule.loadApkFile(input), input)
        }
        onProgress(15)

        // step 2: DexPatcher — add logging hooks
        try {
            DexPatcher.patch(module)
        } catch (_: Exception) { }
        onProgress(25)

        // step 3: TranslationPatcher — rewrite PairIP classes, remove lib, extract assets
        val json = File(appOut, ".pairip_empty.json")
        json.writeText("{}")
        try {
            TranslationPatcher.patch(module, json)
        } catch (_: Exception) { }
        json.delete()
        onProgress(40)

        // step 4: write with real-time progress from antik's writeApk callback
        val outFile = File(appOut, "${pkg}_clean.apk")
        writeWithProgress(module, outFile) { pct -> onProgress(40 + (pct * 55 / 100)) }

        // step 5: CRC32 fix — patch checksums to match original
        try {
            crc32.patch(originalFile, outFile)
        } catch (_: Exception) { }
        onProgress(100)

        outFile.absolutePath
    }

    private fun writeWithProgress(module: ApkModule, out: File, onProgress: (Int) -> Unit) {
        // estimate total like loading.java does
        var passBytes = 0L; var workBytes = 0L; var headerBytes = 64L
        val sources = module.zipEntryMap.toArray(true)
        for (s in sources) {
            headerBytes += 92 + ((s.alias?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0) * 2L)
            val old = when (s) {
                is ArchiveFileEntrySource -> s
                is RenamedInputSource<*> -> s.getParentInputSource(ArchiveFileEntrySource::class.java)
                else -> null
            }
            val len = maxOf(0L, s.length)
            if (old != null && old.method == s.method) passBytes += len
            else if (s.method != Archive.DEFLATED) workBytes += len
            else workBytes += maxOf(len, old?.length ?: 0)
        }
        val totalEst = maxOf(1L, passBytes + workBytes + headerBytes)
        val writeStarted = java.util.concurrent.atomic.AtomicLong(0)

        module.writeApk(out, WriteProgress { _, _, bytes ->
            writeStarted.set(bytes)
            // calculate percentage
            val pct = when {
                workBytes <= 0 -> (minOf(out.length().coerceAtMost(totalEst), totalEst) * 99 / totalEst).toInt()
                out.length() > 0 -> {
                    val dyn = maxOf(passBytes + maxOf(writeStarted.get(), workBytes) + headerBytes, out.length())
                    72 + (minOf(out.length(), dyn) * 27 / dyn).toInt()
                }
                else -> (minOf(writeStarted.get(), workBytes) * 72 / workBytes).toInt()
            }
            onProgress(pct.coerceIn(0, 99))
        })
    }

    private fun createTempDir(prefix: String): File {
        return try {
            java.nio.file.Files.createTempDirectory(prefix).toFile()
        } catch (_: Exception) {
            val f = File.createTempFile(prefix, "")
            f.delete().also { f.mkdirs() }
            f
        }
    }
}
