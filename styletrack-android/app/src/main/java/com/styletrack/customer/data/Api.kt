package com.styletrack.customer.data

import com.styletrack.customer.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** All paths are relative to BuildConfig.API_BASE_URL (which ends in /api/). */
interface StyleTrackApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @GET("me")
    suspend fun profile(): Profile

    @PUT("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Profile

    @PUT("me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @GET("me/services")
    suspend fun services(): List<SalonService>

    @GET("me/stylists")
    suspend fun stylists(): List<Stylist>

    @GET("me/stylists/{id}/availability")
    suspend fun availability(
        @Path("id") stylistId: Long,
        @Query("date") date: String,
        @Query("serviceId") serviceId: Long,
        @Query("homeService") homeService: Boolean,
    ): Availability

    @GET("me/appointments")
    suspend fun appointments(@Query("size") size: Int = 100): Page<Appointment>

    @GET("me/appointments/{id}")
    suspend fun appointment(@Path("id") id: Long): Appointment

    @POST("me/appointments")
    suspend fun book(@Body body: BookRequest): Appointment

    @PUT("me/appointments/{id}/cancel")
    suspend fun cancel(@Path("id") id: Long): Appointment

    @PUT("me/appointments/{id}/reschedule")
    suspend fun reschedule(@Path("id") id: Long, @Body body: RescheduleRequest): Appointment

    @GET("me/loyalty")
    suspend fun loyalty(): LoyaltyBalance

    @GET("me/loyalty/transactions")
    suspend fun loyaltyHistory(@Query("size") size: Int = 50): Page<LoyaltyTx>

    @GET("me/recommendations")
    suspend fun recommendations(): List<Recommendation>

    @GET("me/notifications")
    suspend fun notifications(@Query("size") size: Int = 50): Page<AppNotification>

    @GET("me/notifications/unread-count")
    suspend fun unreadCount(): UnreadCount

    @PUT("me/notifications/seen-all")
    suspend fun markAllSeen()
}

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/** Builds the Retrofit client. [token] supplies the current login token, [onUnauthorized] fires on a 401. */
fun buildApi(token: () -> String?, onUnauthorized: () -> Unit): StyleTrackApi {
    val auth = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        token()?.let { builder.header("Authorization", "Bearer $it") }
        val response = chain.proceed(builder.build())
        // a rejected token on any call except sign-in/registration means the session is over
        val path = chain.request().url.encodedPath
        if (response.code == 401 && !path.endsWith("/auth/login") && !path.endsWith("/auth/register")) {
            onUnauthorized()
        }
        response
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(auth)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(StyleTrackApi::class.java)
}
