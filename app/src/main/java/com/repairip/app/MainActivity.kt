package com.repairip.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var tvStatus: TextView
    private val adapter = AppAdapter(mutableListOf()) { onAppTap(it) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        rv = findViewById(R.id.rvApps)
        tvStatus = findViewById(R.id.tvStatus)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        if (!hasStoragePerms()) {
            requestPerms()
        } else {
            scan()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)
        if (hasStoragePerms()) scan()
        else tvStatus.text = "No storage permission"
    }

    override fun onResume() {
        super.onResume()
        if (hasStoragePerms() && adapter.itemCount == 0) scan()
    }

    private fun hasStoragePerms(): Boolean {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager()
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPerms() {
        if (Build.VERSION.SDK_INT >= 30) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            })
        } else {
            requestPermissions(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1)
        }
    }

    private fun scan() {
        tvStatus.text = "Scanning for PairIP..."
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                installed.mapNotNull { ai ->
                    try {
                        val name = pm.getApplicationLabel(ai).toString()
                        val icon = pm.getApplicationIcon(ai)
                        val hasPP = PairipDetector.isPairip(ai.sourceDir, ai.splitSourceDirs, ai.nativeLibraryDir)
                        if (!hasPP) return@mapNotNull null
                        AppInfo(ai.packageName, name, icon, ai.sourceDir, true)
                    } catch (_: Exception) { null }
                }
            }
            adapter.update(apps)
            tvStatus.text = "${apps.size} PairIP apps found"
        }
    }

    private fun onAppTap(app: AppInfo) {
        val dir = File(Environment.getExternalStorageDirectory(), "RePairip/${app.pkg}")
        if (dir.exists() && dir.listFiles()?.any { it.name.endsWith(".apk") } == true) {
            AlertDialog.Builder(this)
                .setTitle("Already dumped")
                .setMessage("${dir.absolutePath}")
                .setPositiveButton("Dump again") { _, _ -> doDump(app) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            doDump(app)
        }
    }

    private fun doDump(app: AppInfo) {
        val dlg = AlertDialog.Builder(this)
            .setTitle("Dumping ${app.name}")
            .setMessage("Processing...")
            .setCancelable(false)
            .show()
        scope.launch {
            try {
                val path = Dumper.dump(app.sourceDir, app.pkg)
                dlg.setMessage("Done!\n$path")
                dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ -> dlg.dismiss() }
                Toast.makeText(this@MainActivity, "Saved to $path", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                dlg.setMessage("Failed: ${e.message}")
                dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ -> dlg.dismiss() }
            }
        }
    }
}
