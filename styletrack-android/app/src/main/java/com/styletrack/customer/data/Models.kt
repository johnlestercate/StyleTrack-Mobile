package com.styletrack.customer.data

import kotlinx.serialization.Serializable

// Shapes of the StyleTrack API. Unknown fields from the server are ignored (see Json config in Api.kt).

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val fullName: String, val phone: String, val email: String? = null, val password: String)

@Serializable
data class UserInfo(val id: Long, val username: String, val fullName: String, val role: String)

@Serializable
data class LoginResponse(val token: String, val user: UserInfo)

@Serializable
data class Profile(
    val id: Long,
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val loyaltyPoints: Int = 0,
    val completedVisits: Int = 0,
    val tier: String = "REGULAR",
    val visitsToNextTier: Int = 0,
)

@Serializable
data class UpdateProfileRequest(val fullName: String, val email: String? = null, val address: String? = null)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class SalonService(
    val id: Long,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val price: Double,
    val durationMinutes: Int,
    val homeServiceAvailable: Boolean = false,
    val pointsAwarded: Int? = null,
)

@Serializable
data class Stylist(
    val id: Long,
    val fullName: String,
    val specialization: String? = null,
    val bio: String? = null,
)

@Serializable
data class Availability(val availableStartTimes: List<String> = emptyList())

@Serializable
data class Appointment(
    val id: Long,
    val stylistId: Long,
    val stylistName: String,
    val serviceId: Long,
    val serviceName: String,
    val startTime: String,
    val endTime: String,
    val type: String,
    val status: String,
    val address: String? = null,
    val notes: String? = null,
    val rejectionReason: String? = null,
    val homeServiceFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "UNPAID",
    val paymentMethod: String? = null,
    val pointsEarned: Int = 0,
) {
    val isHomeService get() = type == "HOME_SERVICE"
    val isOpen get() = status == "PENDING" || status == "ACCEPTED"
}

@Serializable
data class BookRequest(
    val stylistId: Long,
    val serviceId: Long,
    val type: String,
    val startTime: String,
    val address: String? = null,
    val notes: String? = null,
)

@Serializable
data class RescheduleRequest(val startTime: String, val stylistId: Long? = null)

@Serializable
data class Page<T>(val content: List<T> = emptyList(), val totalElements: Long = 0)

@Serializable
data class LoyaltyBalance(
    val balance: Int = 0,
    val completedVisits: Int = 0,
    val tier: String = "REGULAR",
    val visitsToNextTier: Int = 0,
)

@Serializable
data class LoyaltyTx(
    val id: Long,
    val points: Int,
    val balanceAfter: Int,
    val type: String,
    val description: String? = null,
    val createdAt: String,
)

@Serializable
data class Recommendation(
    val id: Long,
    val serviceId: Long,
    val serviceName: String,
    val stylistName: String? = null,
    val message: String,
)

@Serializable
data class AppNotification(
    val id: Long,
    val type: String,
    val title: String,
    val message: String,
    val appointmentId: Long? = null,
    val seen: Boolean = false,
    val createdAt: String,
)

@Serializable
data class PayOptions(val enabled: Boolean = false, val methods: List<String> = emptyList())

@Serializable
data class PayRequest(val method: String)

/** What to do next after starting an online payment: open [redirectUrl] (GCash) or show [qrImage] (QR Ph). */
@Serializable
data class PayStarted(
    val paymentId: Long,
    val method: String,
    val status: String = "PENDING",
    val amount: Double = 0.0,
    val redirectUrl: String? = null,
    val qrImage: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class PayStatus(val status: String = "NONE", val method: String? = null, val expiresAt: String? = null)

@Serializable
data class UnreadCount(val unread: Long = 0)

@Serializable
data class ApiError(val message: String? = null, val fieldErrors: Map<String, String>? = null)
