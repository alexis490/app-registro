package com.example.app_registro.ui

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore
import com.example.app_registro.data.Product
import com.example.app_registro.data.UserRole
import com.example.app_registro.databinding.ActivityProductManagementBinding
import com.example.app_registro.databinding.DialogProductBinding
import com.example.app_registro.ui.adapters.ProductAdapter
import java.io.BufferedReader
import java.io.InputStreamReader

class ProductManagementActivity : BaseActivity() {
    private lateinit var binding: ActivityProductManagementBinding
    private lateinit var adapter: ProductAdapter

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) importProducts(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = requireUser()
        if (user.role != UserRole.ADMIN) {
            finish()
            return
        }

        binding = ActivityProductManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProductAdapter(
            onEdit = { showProductDialog(it) },
            onDelete = {
                LocalStore.deleteProduct(it.code)
                refreshProducts()
            }
        )

        binding.productsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.productsRecyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.addProductButton.setOnClickListener { showProductDialog(null) }
        binding.importButton.setOnClickListener {
            importLauncher.launch(
                arrayOf(
                    "text/*",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
        }

        refreshProducts()
    }

    override fun onResume() {
        super.onResume()
        refreshProducts()
    }

    private fun refreshProducts() {
        val products = LocalStore.getProducts()
        adapter.submitList(products)
        binding.emptyStateText.isVisible = products.isEmpty()
    }

    private fun showProductDialog(existingProduct: Product?) {
        val dialogBinding = DialogProductBinding.inflate(layoutInflater)
        dialogBinding.codeEditText.setText(existingProduct?.code.orEmpty())
        dialogBinding.nameEditText.setText(existingProduct?.name.orEmpty())

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existingProduct == null) R.string.add_product else R.string.edit_product)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = dialogBinding.codeEditText.text.toString().trim()
                val name = dialogBinding.nameEditText.text.toString().trim()
                if (code.isBlank() || name.isBlank()) {
                    Toast.makeText(this, R.string.complete_fields, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                LocalStore.upsertProduct(Product(code, name))
                refreshProducts()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun importProducts(uri: Uri) {
        val fileName = queryFileName(uri)
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension == "xlsx" || extension == "xls") {
            Toast.makeText(this, R.string.xlsx_not_supported_yet, Toast.LENGTH_LONG).show()
            return
        }

        val importedProducts = mutableListOf<Product>()
        contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed
                    val parts = line.split(",", ";", "\t")
                        .map { it.trim().trim('"') }
                        .filter { it.isNotBlank() }
                    if (parts.size < 2) return@forEachIndexed
                    if (index == 0 && parts[0].equals("codigo", true)) return@forEachIndexed
                    importedProducts.add(Product(parts[0], parts[1]))
                }
            }
        }

        val importedCount = LocalStore.importProducts(importedProducts)
        Toast.makeText(this, getString(R.string.import_success, importedCount), Toast.LENGTH_LONG)
            .show()
        refreshProducts()
    }

    private fun queryFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) return cursor.getString(nameIndex)
        }
        return uri.lastPathSegment.orEmpty()
    }
}
