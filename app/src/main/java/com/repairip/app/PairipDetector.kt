package com.repairip.app

import java.io.File
import java.util.zip.ZipFile

object PairipDetector {
    private val PAIRIP_ASSET_HEADER = byteArrayOf(0x00, 0x49, 0x41, 0x50, 0x02)

    fun isPairip(apkPath: String): Boolean {
        val f = File(apkPath)
        if (!f.exists()) return false
        return scanApkZip(f)
    }

    fun isPairip(sourceDir: String, splitDirs: Array<String>?, nativeLibDir: String?): Boolean {
        if (scanApkZip(File(sourceDir))) return true
        if (splitDirs != null) {
            for (s in splitDirs) {
                if (scanApkZip(File(s))) return true
            }
        }
        if (nativeLibDir != null) {
            val libf = File(nativeLibDir, "libpairipcore.so")
            if (libf.exists()) return true
        }
        return false
    }

    private fun scanApkZip(f: File): Boolean {
        if (!f.exists()) return false
        return try {
            ZipFile(f).use { zip ->
                val entries = zip.entries()
                var hasCoreLib = false
                val assets = mutableListOf<String>()

                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    val name = e.name
                    if (name.contains("libpairipcore.so")) {
                        hasCoreLib = true
                    }
                    if (name.startsWith("assets/") && name.indexOf('/', 7) < 0 && !name.endsWith("/")) {
                        assets.add(name)
                    }
                }

                if (!hasCoreLib && assets.isEmpty()) return@use false

                // confirm via asset header
                for (an in assets) {
                    try {
                        zip.getInputStream(zip.getEntry(an))?.use { ins ->
                            val hdr = ByteArray(5)
                            if (ins.read(hdr) == 5 && hdr.contentEquals(PAIRIP_ASSET_HEADER)) {
                                return@use true
                            }
                        }
                    } catch (_: Exception) { }
                }

                // check DEX for PairIP classes
                val dexEntries = zip.entries().asSequence().filter { it.name.startsWith("classes") && it.name.endsWith(".dex") }.take(2).toList()
                for (de in dexEntries) {
                    try {
                        val raw = zip.getInputStream(de)?.readAllBytes() ?: continue
                        val str = raw.decodeToString()
                        if (str.contains("Lcom/pairip/")) return@use true
                        if (str.contains("com/pairip")) return@use true
                    } catch (_: Exception) { }
                }

                hasCoreLib
            }
        } catch (_: Exception) {
            false
        }
    }
}
