package com.example.app_registro.data

enum class UserRole {
    ADMIN,
    USER
}

enum class PaymentStatus(val storageValue: String) {
    PENDIENTE("pendiente"),
    PAGADO_PARCIAL("pagado_parcial"),
    PAGADO("pagado");

    companion object {
        fun fromStorage(value: String): PaymentStatus {
            return entries.firstOrNull { it.storageValue == value } ?: PENDIENTE
        }
    }
}

data class AppUser(
    val username: String,
    val password: String,
    val role: UserRole,
    val storeName: String
)

data class Product(
    val code: String,
    val name: String
)

data class Record(
    val id: String,
    val productName: String,
    val code: String,
    val quantity: Int,
    val responsible: String,
    val storeName: String,
    val createdAtMillis: Long,
    val alarmAtMillis: Long?,
    val paymentStatus: PaymentStatus,
    val createdBy: String
)
