package com.repairip.app

import android.graphics.drawable.Drawable

data class AppInfo(
    val pkg: String,
    val name: String,
    val icon: Drawable?,
    val sourceDir: String,
    var hasPairIP: Boolean = false
)
