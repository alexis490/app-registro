package com.example.app_registro.ui

import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.app_registro.data.AppUser
import com.example.app_registro.data.LocalStore

abstract class BaseActivity : AppCompatActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    protected fun currentUser(): AppUser? = LocalStore.getCurrentUser()

    protected fun requireUser(): AppUser {
        return currentUser() ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            throw IllegalStateException("No active user session")
        }
    }

    protected fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
