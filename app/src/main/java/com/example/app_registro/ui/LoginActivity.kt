package com.example.app_registro.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore
import com.example.app_registro.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStore.getCurrentUser()?.let {
            goToDashboard()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.demoCredentialsText.text = getString(
            R.string.demo_credentials,
            "admin / admin123",
            "vendedor / venta123"
        )

        binding.loginButton.setOnClickListener { login() }
    }

    private fun login() {
        val username = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val keepSession = binding.keepSessionCheckBox.isChecked

        val user = LocalStore.authenticate(username, password)
        if (user == null) {
            Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show()
            return
        }

        LocalStore.saveSession(user.username, keepSession)
        goToDashboard()
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
