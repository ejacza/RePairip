package com.repairip.app

import android.os.Environment
import com.antik.DexPatcher.DexPatcher
import com.antik.crc32.crc32
import com.antik.manifest.manifestP
import com.antik.utils.deleteDir
import com.antik.utils.loading
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

        if (isSplit) {
            val tmpDir = createTempDir("rmg_")
            try {
                com.antik.AntikUtils.ex_apks(input, tmpDir)
                val bundle = ApkBundle()
                bundle.loadApkDirectory(tmpDir)
                val module = bundle.mergeModules()
                manifestP.patch(module)

                val merged = File(appOut, "${pkg}_merged.apk")
                loading.progress(module, merged)

                val clean = File(appOut, "${pkg}_clean.apk")
                merged.copyTo(clean, overwrite = true)

                DexPatcher.patch(module)
                val logging = File(appOut, "${pkg}_pairip.apk")
                output.write(module, logging)
                crc32.patch(merged, logging)

                return@withContext logging.absolutePath
            } finally {
                deleteDir.del_dir(tmpDir)
            }
        } else {
            val clean = File(appOut, "${pkg}.apk")
            input.copyTo(clean, overwrite = true)

            try {
                val module = ApkModule.loadApkFile(input)
                DexPatcher.patch(module)
                val logging = File(appOut, "${pkg}_pairip.apk")
                output.write(module, logging)
                crc32.patch(input, logging)
            } catch (_: Exception) { }

            return@withContext clean.absolutePath
        }
    }

    private fun createTempDir(prefix: String): File {
        return try {
            java.nio.file.Files.createTempDirectory(prefix).toFile()
        } catch (_: Exception) {
            val f = File.createTempFile(prefix, "")
            f.delete()
            f.mkdirs()
            f
        }
    }
}
