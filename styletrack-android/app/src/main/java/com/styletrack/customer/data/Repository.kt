package com.styletrack.customer.data

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** An error with a message that is safe to show the customer as-is. */
class ApiException(message: String, val code: Int = 0) : Exception(message)

fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Something went wrong."
    is IOException -> "Can't reach the salon right now. Check your internet connection and try again."
    else -> "Something went wrong. Please try again."
}

class Repository(private val api: StyleTrackApi, private val session: SessionManager, private val tokens: TokenStore) {

    /** Turns HTTP and network failures into [ApiException]s with readable messages. */
    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        throw ApiException(errorMessage(e), e.code())
    } catch (e: IOException) {
        throw ApiException(e.userMessage())
    }

    private fun errorMessage(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let { runCatching { AppJson.decodeFromString<ApiError>(it) }.getOrNull() }
        val fields = parsed?.fieldErrors?.values?.firstOrNull()
        return when {
            fields != null -> fields
            !parsed?.message.isNullOrBlank() -> parsed?.message!!
            e.code() == 401 -> "Your session has ended. Please sign in again."
            e.code() >= 500 -> "The salon server had a problem. Please try again in a moment."
            else -> "Something went wrong (${e.code()})."
        }
    }

    // ------------------------------------------------------------------ session

    /** Signs in with the mobile number; returns normally on success. */
    suspend fun login(phone: String, password: String) {
        val response = call { api.login(LoginRequest(normalizePhone(phone), password)) }
        requireCustomer(response)
        session.signedIn(response.token, response.user.fullName)
    }

    suspend fun register(fullName: String, phone: String, email: String?, password: String) {
        val response = call {
            api.register(RegisterRequest(fullName.trim(), normalizePhone(phone), email?.trim()?.ifBlank { null }, password))
        }
        session.signedIn(response.token, response.user.fullName)
    }

    /** On app start: if a token is saved, check that it still works. */
    suspend fun restoreSession() {
        if (tokens.token == null) {
            session.signedOut()
            return
        }
        try {
            session.restored(call { api.profile() }.fullName)
        } catch (e: ApiException) {
            // 401 already signs out through the HTTP layer; any other failure with no network keeps the saved login
            if (e.code == 401 || e.code == 403) session.signedOut() else session.restored("")
        }
    }

    fun signOut() = session.signedOut()

    private fun requireCustomer(response: LoginResponse) {
        if (response.user.role != "CUSTOMER") {
            // staff accounts belong in the web app; don't keep their token on this device
            throw ApiException("This app is for customers. Staff please use the StyleTrack web app.")
        }
    }

    // ---------------------------------------------------------------- customer

    suspend fun profile() = call { api.profile() }
    suspend fun updateProfile(name: String, email: String?, address: String?) =
        call { api.updateProfile(UpdateProfileRequest(name.trim(), email?.trim()?.ifBlank { null }, address?.trim()?.ifBlank { null })) }
    suspend fun changePassword(current: String, new: String) = call { api.changePassword(ChangePasswordRequest(current, new)) }

    suspend fun services() = call { api.services() }
    suspend fun stylists() = call { api.stylists() }
    suspend fun availability(stylistId: Long, date: String, serviceId: Long, home: Boolean) =
        call { api.availability(stylistId, date, serviceId, home) }.availableStartTimes

    suspend fun appointments() = call { api.appointments() }.content
    suspend fun appointment(id: Long) = call { api.appointment(id) }
    suspend fun book(request: BookRequest) = call { api.book(request) }
    suspend fun cancel(id: Long) = call { api.cancel(id) }
    suspend fun reschedule(id: Long, startTime: String, stylistId: Long?) =
        call { api.reschedule(id, RescheduleRequest(startTime, stylistId)) }

    suspend fun payOptions() = call { api.paymentOptions() }
    suspend fun startPayment(appointmentId: Long, method: String) = call { api.pay(appointmentId, PayRequest(method)) }
    suspend fun paymentStatus(appointmentId: Long) = call { api.onlinePayment(appointmentId) }

    suspend fun loyalty() = call { api.loyalty() }
    suspend fun loyaltyHistory() = call { api.loyaltyHistory() }.content
    suspend fun recommendations() = call { api.recommendations() }

    suspend fun notifications() = call { api.notifications() }.content
    suspend fun unreadCount() = call { api.unreadCount() }.unread
    suspend fun markAllSeen() = call { api.markAllSeen() }
}

/** Mobile numbers are digits only; +63 917... and 0917... are the same number (matches the server). */
fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 12 && digits.startsWith("63")) "0" + digits.substring(2) else digits
}
