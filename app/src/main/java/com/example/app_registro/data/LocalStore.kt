package com.example.app_registro.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object LocalStore {
    private const val PREFS_NAME = "app_registro_prefs"
    private const val KEY_USERS = "users"
    private const val KEY_PRODUCTS = "products"
    private const val KEY_RECORDS = "records"
    private const val KEY_SESSION_USER = "session_user"
    private const val KEY_KEEP_SESSION = "keep_session"
    private const val KEY_SEEDED = "seeded"

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            seedDefaults()
        }
    }

    private fun seedDefaults() {
        saveUsers(
            listOf(
                AppUser("admin", "admin123", UserRole.ADMIN, "Central"),
                AppUser("vendedor", "venta123", UserRole.USER, "Tienda Norte")
            )
        )
        saveProducts(
            listOf(
                Product("750100000001", "Arroz Premium"),
                Product("750100000002", "Aceite Familiar"),
                Product("750100000003", "Azucar Blanca")
            )
        )
        saveRecords(emptyList())
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    fun authenticate(username: String, password: String): AppUser? {
        return getUsers().firstOrNull { it.username == username && it.password == password }
    }

    fun getUsers(): List<AppUser> = readArray(KEY_USERS) { json ->
        AppUser(
            json.getString("username"),
            json.getString("password"),
            UserRole.valueOf(json.getString("role")),
            json.getString("storeName")
        )
    }

    fun saveUsers(users: List<AppUser>) {
        writeArray(KEY_USERS, users) { user ->
            JSONObject()
                .put("username", user.username)
                .put("password", user.password)
                .put("role", user.role.name)
                .put("storeName", user.storeName)
        }
    }

    fun getProducts(): List<Product> = readArray(KEY_PRODUCTS) { json ->
        Product(json.getString("code"), json.getString("name"))
    }.sortedBy { it.name.lowercase() }

    fun saveProducts(products: List<Product>) {
        writeArray(KEY_PRODUCTS, products.distinctBy { it.code.trim() }) { product ->
            JSONObject()
                .put("code", product.code)
                .put("name", product.name)
        }
    }

    fun upsertProduct(product: Product) {
        val products = getProducts().toMutableList()
        val index = products.indexOfFirst { it.code.equals(product.code, ignoreCase = true) }
        if (index >= 0) {
            products[index] = product
        } else {
            products.add(product)
        }
        saveProducts(products)
    }

    fun deleteProduct(code: String) {
        saveProducts(getProducts().filterNot { it.code == code })
    }

    fun findProductByCode(code: String): Product? {
        return getProducts().firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }

    fun importProducts(products: List<Product>): Int {
        val merged = getProducts().associateBy { it.code }.toMutableMap()
        var imported = 0
        products.forEach { product ->
            if (product.code.isNotBlank() && product.name.isNotBlank()) {
                merged[product.code] = product
                imported++
            }
        }
        saveProducts(merged.values.toList())
        return imported
    }

    fun getRecords(): List<Record> = readArray(KEY_RECORDS) { json ->
        Record(
            id = json.getString("id"),
            productName = json.getString("productName"),
            code = json.getString("code"),
            quantity = json.getInt("quantity"),
            responsible = json.getString("responsible"),
            storeName = json.getString("storeName"),
            createdAtMillis = json.getLong("createdAtMillis"),
            paymentStatus = PaymentStatus.fromStorage(json.getString("paymentStatus")),
            createdBy = json.getString("createdBy")
        )
    }.sortedByDescending { it.createdAtMillis }

    fun saveRecord(record: Record) {
        val records = getRecords().toMutableList()
        val index = records.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            records[index] = record
        } else {
            records.add(record)
        }
        saveRecords(records)
    }

    fun deleteRecord(recordId: String) {
        saveRecords(getRecords().filterNot { it.id == recordId })
    }

    fun createRecord(
        productName: String,
        code: String,
        quantity: Int,
        responsible: String,
        storeName: String,
        paymentStatus: PaymentStatus,
        createdBy: String,
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis()
    ): Record {
        return Record(
            id = existingId ?: UUID.randomUUID().toString(),
            productName = productName,
            code = code,
            quantity = quantity,
            responsible = responsible,
            storeName = storeName,
            createdAtMillis = createdAtMillis,
            paymentStatus = paymentStatus,
            createdBy = createdBy
        )
    }

    fun getPendingRecordsForReminder(): List<Record> {
        return getRecords().filter {
            it.paymentStatus == PaymentStatus.PENDIENTE ||
                it.paymentStatus == PaymentStatus.PAGADO_PARCIAL
        }
    }

    fun saveSession(username: String?, keepSession: Boolean) {
        prefs.edit()
            .putString(KEY_SESSION_USER, username)
            .putBoolean(KEY_KEEP_SESSION, keepSession)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_USER)
            .putBoolean(KEY_KEEP_SESSION, false)
            .apply()
    }

    fun getCurrentUser(): AppUser? {
        val username = prefs.getString(KEY_SESSION_USER, null) ?: return null
        return getUsers().firstOrNull { it.username == username }
    }

    private fun saveRecords(records: List<Record>) {
        writeArray(KEY_RECORDS, records) { record ->
            JSONObject()
                .put("id", record.id)
                .put("productName", record.productName)
                .put("code", record.code)
                .put("quantity", record.quantity)
                .put("responsible", record.responsible)
                .put("storeName", record.storeName)
                .put("createdAtMillis", record.createdAtMillis)
                .put("paymentStatus", record.paymentStatus.storageValue)
                .put("createdBy", record.createdBy)
        }
    }

    private fun <T> readArray(key: String, mapper: (JSONObject) -> T): List<T> {
        val serialized = prefs.getString(key, null) ?: return emptyList()
        val array = JSONArray(serialized)
        return buildList {
            for (index in 0 until array.length()) {
                add(mapper(array.getJSONObject(index)))
            }
        }
    }

    private fun <T> writeArray(key: String, items: List<T>, serializer: (T) -> JSONObject) {
        val jsonArray = JSONArray()
        items.forEach { item -> jsonArray.put(serializer(item)) }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }
}
