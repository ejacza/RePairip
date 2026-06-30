package com.repairip.app

import android.os.Environment
import com.antik.DexPatcher.Translation.TranslationPatcher
import com.antik.manifest.manifestP
import com.antik.utils.deleteDir
import com.antik.utils.output
import com.reandroid.apk.ApkBundle
import com.reandroid.apk.ApkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Dumper {
    private val outDir = File(Environment.getExternalStorageDirectory(), "RePairip")

    fun ensureDir() { outDir.mkdirs() }

    suspend fun dump(apkPath: String, pkg: String): String = withContext(Dispatchers.IO) {
        ensureDir()
        val appOut = File(outDir, pkg)
        appOut.mkdirs()
        val input = File(apkPath)
        val isSplit = apkPath.endsWith(".apks") || apkPath.endsWith(".xapk")

        val module = if (isSplit) {
            val tmpDir = createTempDir("rmg_")
            try {
                com.antik.AntikUtils.ex_apks(input, tmpDir)
                val bundle = ApkBundle()
                bundle.loadApkDirectory(tmpDir)
                val m = bundle.mergeModules()
                manifestP.patch(m)
                m
            } finally {
                deleteDir.del_dir(tmpDir)
            }
        } else {
            ApkModule.loadApkFile(input)
        }

        // run RePairip translation — rewrites PairIP classes, removes libpairipcore.so
        val json = File(appOut, ".empty_pairip.json")
        json.writeText("{}")
        try {
            TranslationPatcher.patch(module, json)
        } catch (_: Exception) { }
        json.delete()

        val outFile = File(appOut, "${pkg}_clean.apk")
        output.write(module, outFile)
        outFile.absolutePath
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
