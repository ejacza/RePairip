package com.repairip.app

import java.io.File
import java.util.zip.ZipFile

object PairipDetector {
    fun isPairip(sourceDir: String, splitDirs: Array<String>?, nativeLibDir: String?): Boolean {
        // fastest: stat call, no i/o
        if (nativeLibDir != null && File(nativeLibDir, "libpairipcore.so").exists()) return true
        if (scanApkFast(File(sourceDir))) return true
        if (splitDirs != null) for (s in splitDirs) if (scanApkFast(File(s))) return true
        return false
    }

    private fun scanApkFast(f: File): Boolean {
        if (!f.exists()) return false
        return try {
            ZipFile(f).use { zip ->
                var checkedDex = false
                val en = zip.entries()
                while (en.hasMoreElements()) {
                    val name = en.nextElement().name
                    if (name.contains("libpairipcore.so")) return@use true
                    if (!checkedDex && name.startsWith("classes") && name.endsWith(".dex")) {
                        checkedDex = true
                        try {
                            val hdr = ByteArray(65536)
                            val n = zip.getInputStream(zip.getEntry(name))?.read(hdr) ?: 0
                            if (hdr.decodeToString(0, n).contains("Lcom/pairip/")) return@use true
                        } catch (_: Exception) { }
                    }
                }
                false
            }
        } catch (_: Exception) { false }
    }
}
