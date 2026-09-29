package com.music.bitchord.ui.classipod

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity

class ClassipodActivity : ComponentActivity() {
    private var isAskingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndStart()
    }

    override fun onResume() {
        super.onResume()
        if (isAskingPermission) {
            isAskingPermission = false
            checkAndStart()
        }
    }

    private fun checkAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            isAskingPermission = true
            Toast.makeText(this, "Please allow 'Display over other apps' for the Pod widget", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            startService(Intent(this, FloatingPodService::class.java))
            finish()
        }
    }
}
