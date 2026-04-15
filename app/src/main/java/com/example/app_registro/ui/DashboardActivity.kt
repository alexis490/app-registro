package com.example.app_registro.ui

import android.content.Intent
import android.os.Bundle
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore
import com.example.app_registro.data.UserRole
import com.example.app_registro.databinding.ActivityDashboardBinding
import com.example.app_registro.notifications.ReminderScheduler

class DashboardActivity : BaseActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = requireUser()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestNotificationPermissionIfNeeded()

        binding.welcomeText.text = getString(R.string.welcome_message, user.username, user.storeName)
        binding.closeTimeText.text =
            getString(R.string.close_time_message, ReminderScheduler.formatCloseTime())

        binding.scanRegisterCard.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.recordsCard.setOnClickListener {
            startActivity(Intent(this, RecordsActivity::class.java))
        }

        binding.manageProductsCard.isEnabled = user.role == UserRole.ADMIN
        binding.manageProductsCard.alpha = if (user.role == UserRole.ADMIN) 1f else 0.45f
        binding.manageProductsCard.setOnClickListener {
            if (user.role == UserRole.ADMIN) {
                startActivity(Intent(this, ProductManagementActivity::class.java))
            }
        }

        binding.logoutButton.setOnClickListener {
            LocalStore.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
