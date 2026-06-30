package com.repairip.app

import android.graphics.drawable.Drawable

data class AppInfo(
    val pkg: String,
    val name: String,
    val icon: Drawable?,
    val sourceDir: String,
    val hasPairIP: Boolean = false,
    val splitDirs: Array<String>? = null
)
