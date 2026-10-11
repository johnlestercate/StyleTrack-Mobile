package com.styletrack.customer.data;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** All paths are relative to BuildConfig.API_BASE_URL (which ends in /api/). */
public interface Api {

    @POST("auth/login")
    CompletableFuture<LoginResponse> login(@Body LoginRequest body);

    @POST("auth/register")
    CompletableFuture<LoginResponse> register(@Body RegisterRequest body);

    @GET("me")
    CompletableFuture<Profile> profile();

    @PUT("me")
    CompletableFuture<Profile> updateProfile(@Body UpdateProfileRequest body);

    @PUT("me/password")
    CompletableFuture<Void> changePassword(@Body ChangePasswordRequest body);

    @GET("me/services")
    CompletableFuture<List<SalonService>> services();

    @GET("me/stylists")
    CompletableFuture<List<Stylist>> stylists();

    @GET("me/stylists/{id}/availability")
    CompletableFuture<Availability> availability(
            @Path("id") long stylistId,
            @Query("date") String date,
            @Query("serviceId") long serviceId,
            @Query("homeService") boolean homeService);

    @GET("me/appointments")
    CompletableFuture<Page<Appointment>> appointments(@Query("size") int size);

    @GET("me/appointments/{id}")
    CompletableFuture<Appointment> appointment(@Path("id") long id);

    @POST("me/appointments")
    CompletableFuture<Appointment> book(@Body BookRequest body);

    @PUT("me/appointments/{id}/cancel")
    CompletableFuture<Appointment> cancel(@Path("id") long id);

    @PUT("me/appointments/{id}/reschedule")
    CompletableFuture<Appointment> reschedule(@Path("id") long id, @Body RescheduleRequest body);

    @GET("me/payment-options")
    CompletableFuture<PayOptions> paymentOptions();

    @POST("me/appointments/{id}/pay")
    CompletableFuture<PayStarted> pay(@Path("id") long id, @Body PayRequest body);

    @GET("me/appointments/{id}/online-payment")
    CompletableFuture<PayStatus> onlinePayment(@Path("id") long id);

    @GET("me/loyalty")
    CompletableFuture<LoyaltyBalance> loyalty();

    @GET("me/loyalty/transactions")
    CompletableFuture<Page<LoyaltyTx>> loyaltyHistory(@Query("size") int size);

    @GET("me/recommendations")
    CompletableFuture<List<Recommendation>> recommendations();

    @GET("me/notifications")
    CompletableFuture<Page<AppNotification>> notifications(@Query("size") int size);

    @GET("me/notifications/unread-count")
    CompletableFuture<UnreadCount> unreadCount();

    @PUT("me/notifications/seen-all")
    CompletableFuture<Void> markAllSeen();
}
