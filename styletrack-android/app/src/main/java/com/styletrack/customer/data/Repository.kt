package com.styletrack.customer.data

import com.google.gson.Gson
import kotlinx.coroutines.future.await

/**
 * Compose screens call this as plain suspend functions. All real networking, error-handling and
 * session logic lives in [RepositoryCore] (Java) - this class only adapts CompletableFuture to
 * suspend so the rest of the Kotlin/Compose UI layer never has to deal with futures directly.
 */
class Repository(private val core: RepositoryCore) {

    // ------------------------------------------------------------------ session
    suspend fun login(phone: String, password: String) = core.login(phone, password).await()
    suspend fun register(fullName: String, phone: String, email: String?, password: String) =
        core.register(fullName, phone, email, password).await()
    suspend fun restoreSession() = core.restoreSession().await()
    fun signOut() = core.signOut()

    // ---------------------------------------------------------------- customer
    suspend fun profile() = core.profile().await()
    suspend fun updateProfile(name: String, email: String?, address: String?) = core.updateProfile(name, email, address).await()
    suspend fun changePassword(current: String, new: String) = core.changePassword(current, new).await()

    suspend fun services() = core.services().await()
    suspend fun stylists() = core.stylists().await()
    suspend fun availability(stylistId: Long, date: String, serviceId: Long, home: Boolean) =
        core.availability(stylistId, date, serviceId, home).await()

    suspend fun appointments() = core.appointments().await()
    suspend fun appointment(id: Long) = core.appointment(id).await()
    suspend fun book(request: BookRequest) = core.book(request).await()
    suspend fun cancel(id: Long) = core.cancel(id).await()
    suspend fun reschedule(id: Long, startTime: String, stylistId: Long?) = core.reschedule(id, startTime, stylistId).await()

    suspend fun payOptions() = core.payOptions().await()
    suspend fun startPayment(appointmentId: Long, method: String) = core.startPayment(appointmentId, method).await()
    suspend fun paymentStatus(appointmentId: Long) = core.paymentStatus(appointmentId).await()

    suspend fun loyalty() = core.loyalty().await()
    suspend fun loyaltyHistory() = core.loyaltyHistory().await()
    suspend fun recommendations() = core.recommendations().await()

    suspend fun notifications() = core.notifications().await()
    suspend fun unreadCount() = core.unreadCount().await()
    suspend fun markAllSeen() = core.markAllSeen().await()
}

fun Throwable.userMessage(): String = RepositoryCore.userMessage(this)

fun normalizePhone(raw: String): String = Phones.normalizePhone(raw)

/** Gson-backed shim so BookingsScreen can keep saving/restoring PayStarted across rotation with the same call syntax as before. */
object AppJson {
    @PublishedApi
    internal val gson = Gson()
    fun encodeToString(value: Any): String = gson.toJson(value)
    inline fun <reified T> decodeFromString(json: String): T = gson.fromJson(json, T::class.java)
}
