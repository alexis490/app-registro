package com.example.app_registro.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore
import com.example.app_registro.data.PaymentStatus
import com.example.app_registro.data.Product
import com.example.app_registro.databinding.ActivityRegisterBinding
import com.example.app_registro.databinding.DialogProductBinding
import com.example.app_registro.notifications.ReminderScheduler
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var statusOptions: List<PaymentStatus>

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            binding.codeEditText.setText(contents)
            findProduct()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = requireUser()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statusOptions = PaymentStatus.entries.toList()
        binding.paymentStatusSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusOptions.map { statusLabel(it) }
        )

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.responsibleEditText.setText(user.username)
        binding.storeEditText.setText(user.storeName)
        binding.dateText.text = getString(R.string.current_date_label, System.currentTimeMillis())
        binding.closeWarningText.text =
            getString(R.string.close_warning_message, ReminderScheduler.formatCloseTime())

        binding.findProductButton.setOnClickListener { findProduct() }
        binding.scanButton.setOnClickListener { startScan() }
        binding.saveButton.setOnClickListener { saveRecord() }
    }

    private fun startScan() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            .setPrompt(getString(R.string.scan_prompt))
            .setBeepEnabled(false)
            .setOrientationLocked(false)
        scanLauncher.launch(options)
    }

    private fun findProduct() {
        val code = binding.codeEditText.text.toString().trim()
        if (code.isBlank()) {
            Toast.makeText(this, R.string.enter_code_first, Toast.LENGTH_SHORT).show()
            return
        }

        val product = LocalStore.findProductByCode(code)
        if (product != null) {
            binding.productNameEditText.setText(product.name)
            binding.unknownProductBanner.isVisible = false
        } else {
            binding.productNameEditText.setText("")
            binding.unknownProductBanner.isVisible = true
            showUnknownProductDialog(code)
        }
    }

    private fun showUnknownProductDialog(code: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.product_not_found)
            .setMessage(R.string.product_not_found_message)
            .setPositiveButton(R.string.create_product) { _, _ -> showCreateProductDialog(code) }
            .setNeutralButton(R.string.save_code_only) { _, _ ->
                binding.productNameEditText.setText(getString(R.string.unregistered_product))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreateProductDialog(code: String) {
        val dialogBinding = DialogProductBinding.inflate(layoutInflater)
        dialogBinding.codeEditText.setText(code)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.create_product)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val finalCode = dialogBinding.codeEditText.text.toString().trim()
                val name = dialogBinding.nameEditText.text.toString().trim()
                if (finalCode.isBlank() || name.isBlank()) {
                    Toast.makeText(this, R.string.complete_fields, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                LocalStore.upsertProduct(Product(finalCode, name))
                binding.codeEditText.setText(finalCode)
                binding.productNameEditText.setText(name)
                binding.unknownProductBanner.isVisible = false
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun saveRecord() {
        val code = binding.codeEditText.text.toString().trim()
        val productName = binding.productNameEditText.text.toString().trim()
        val quantity = binding.quantityEditText.text.toString().toIntOrNull()
        val responsible = binding.responsibleEditText.text.toString().trim()
        val storeName = binding.storeEditText.text.toString().trim()
        val user = requireUser()
        val status = statusOptions[binding.paymentStatusSpinner.selectedItemPosition]

        if (
            code.isBlank() || productName.isBlank() || quantity == null || quantity <= 0 ||
            responsible.isBlank() || storeName.isBlank()
        ) {
            Toast.makeText(this, R.string.complete_fields, Toast.LENGTH_SHORT).show()
            return
        }

        val record = LocalStore.createRecord(
            productName = productName,
            code = code,
            quantity = quantity,
            responsible = responsible,
            storeName = storeName,
            paymentStatus = status,
            createdBy = user.username
        )
        LocalStore.saveRecord(record)
        ReminderScheduler.scheduleNextReminder(this)

        Toast.makeText(this, R.string.record_saved, Toast.LENGTH_SHORT).show()
        binding.codeEditText.text?.clear()
        binding.productNameEditText.text?.clear()
        binding.quantityEditText.text?.clear()
        binding.paymentStatusSpinner.setSelection(0)
        binding.unknownProductBanner.isVisible = false
        binding.codeEditText.requestFocus()
    }

    private fun statusLabel(status: PaymentStatus): String {
        return when (status) {
            PaymentStatus.PENDIENTE -> getString(R.string.pending)
            PaymentStatus.PAGADO_PARCIAL -> getString(R.string.partial_paid)
            PaymentStatus.PAGADO -> getString(R.string.paid)
        }
    }
}
