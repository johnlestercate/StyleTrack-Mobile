package com.styletrack.customer.data;

import retrofit2.HttpException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** All of the app's networking and session logic. [Repository] (Kotlin) is the thin suspend-facing wrapper Compose screens call. */
public class RepositoryCore {
    private final Api api;
    private final SessionManager session;
    private final TokenStore tokens;

    public RepositoryCore(Api api, SessionManager session, TokenStore tokens) {
        this.api = api;
        this.session = session;
        this.tokens = tokens;
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }

    /** Turns HTTP and network failures into ApiExceptions with readable messages. */
    private <T> CompletableFuture<T> call(CompletableFuture<T> future) {
        return future.handle((value, error) -> {
            if (error == null) return value;
            Throwable cause = unwrap(error);
            if (cause instanceof HttpException) {
                HttpException e = (HttpException) cause;
                throw new ApiException(errorMessage(e), e.code());
            }
            if (cause instanceof IOException) {
                throw new ApiException(userMessage(cause));
            }
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new ApiException("Something went wrong. Please try again.");
        });
    }

    private String errorMessage(HttpException e) {
        String body = null;
        try {
            if (e.response() != null && e.response().errorBody() != null) {
                body = e.response().errorBody().string();
            }
        } catch (Exception ignored) {
            // unreadable body: fall through to the generic messages below
        }
        ApiError parsed = null;
        if (body != null) {
            try {
                parsed = ApiClient.GSON.fromJson(body, ApiError.class);
            } catch (Exception ignored) {
                // not JSON we understand: fall through
            }
        }
        String fields = (parsed != null && parsed.fieldErrors != null && !parsed.fieldErrors.isEmpty())
                ? parsed.fieldErrors.values().iterator().next() : null;
        if (fields != null) return fields;
        if (parsed != null && parsed.message != null && !parsed.message.isBlank()) return parsed.message;
        if (e.code() == 401) return "Your session has ended. Please sign in again.";
        if (e.code() >= 500) return "The salon server had a problem. Please try again in a moment.";
        return "Something went wrong (" + e.code() + ").";
    }

    /** Message safe to show the customer as-is for any failure the repository can throw. */
    public static String userMessage(Throwable t) {
        if (t instanceof ApiException) {
            String m = t.getMessage();
            return m != null ? m : "Something went wrong.";
        }
        if (t instanceof IOException) {
            return "Can't reach the salon right now. Check your internet connection and try again.";
        }
        return "Something went wrong. Please try again.";
    }

    // ------------------------------------------------------------------ session

    /** Signs in with the mobile number; the returned future completes normally on success. */
    public CompletableFuture<Void> login(String phone, String password) {
        LoginRequest body = new LoginRequest(Phones.normalizePhone(phone), password);
        return call(api.login(body)).thenApply(response -> {
            requireCustomer(response);
            session.signedIn(response.token, response.user.fullName);
            return null;
        });
    }

    public CompletableFuture<Void> register(String fullName, String phone, String email, String password) {
        String trimmedEmail = blankToNull(trimOrNull(email));
        RegisterRequest body = new RegisterRequest(fullName.trim(), Phones.normalizePhone(phone), trimmedEmail, password);
        return call(api.register(body)).thenApply(response -> {
            session.signedIn(response.token, response.user.fullName);
            return null;
        });
    }

    /** On app start: if a token is saved, check that it still works. */
    public CompletableFuture<Void> restoreSession() {
        if (tokens.getToken() == null) {
            session.signedOut();
            return CompletableFuture.completedFuture(null);
        }
        return call(api.profile()).handle((profile, error) -> {
            if (error == null) {
                session.restored(profile.fullName);
                return null;
            }
            Throwable cause = unwrap(error);
            // 401 already signs out through the HTTP layer; any other failure with no network keeps the saved login
            if (cause instanceof ApiException) {
                int code = ((ApiException) cause).getCode();
                if (code == 401 || code == 403) session.signedOut(); else session.restored("");
                return null;
            }
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new CompletionException(cause);
        });
    }

    public void signOut() {
        session.signedOut();
    }

    private void requireCustomer(LoginResponse response) {
        if (!"CUSTOMER".equals(response.user.role)) {
            // staff accounts belong in the web app; don't keep their token on this device
            throw new ApiException("This app is for customers. Staff please use the StyleTrack web app.");
        }
    }

    // ---------------------------------------------------------------- customer

    public CompletableFuture<Profile> profile() {
        return call(api.profile());
    }

    public CompletableFuture<Profile> updateProfile(String name, String email, String address) {
        UpdateProfileRequest body = new UpdateProfileRequest(name.trim(), blankToNull(trimOrNull(email)), blankToNull(trimOrNull(address)));
        return call(api.updateProfile(body));
    }

    public CompletableFuture<Void> changePassword(String current, String newPassword) {
        return call(api.changePassword(new ChangePasswordRequest(current, newPassword)));
    }

    public CompletableFuture<List<SalonService>> services() {
        return call(api.services());
    }

    public CompletableFuture<List<Stylist>> stylists() {
        return call(api.stylists());
    }

    public CompletableFuture<List<String>> availability(long stylistId, String date, long serviceId, boolean home) {
        return call(api.availability(stylistId, date, serviceId, home)).thenApply(a -> a.availableStartTimes);
    }

    public CompletableFuture<List<Appointment>> appointments() {
        return call(api.appointments(100)).thenApply(p -> p.content);
    }

    public CompletableFuture<Appointment> appointment(long id) {
        return call(api.appointment(id));
    }

    public CompletableFuture<Appointment> book(BookRequest request) {
        return call(api.book(request));
    }

    public CompletableFuture<Appointment> cancel(long id) {
        return call(api.cancel(id));
    }

    public CompletableFuture<Appointment> reschedule(long id, String startTime, Long stylistId) {
        return call(api.reschedule(id, new RescheduleRequest(startTime, stylistId)));
    }

    public CompletableFuture<PayOptions> payOptions() {
        return call(api.paymentOptions());
    }

    public CompletableFuture<PayStarted> startPayment(long appointmentId, String method) {
        return call(api.pay(appointmentId, new PayRequest(method)));
    }

    public CompletableFuture<PayStatus> paymentStatus(long appointmentId) {
        return call(api.onlinePayment(appointmentId));
    }

    public CompletableFuture<LoyaltyBalance> loyalty() {
        return call(api.loyalty());
    }

    public CompletableFuture<List<LoyaltyTx>> loyaltyHistory() {
        return call(api.loyaltyHistory(50)).thenApply(p -> p.content);
    }

    public CompletableFuture<List<Recommendation>> recommendations() {
        return call(api.recommendations());
    }

    public CompletableFuture<List<AppNotification>> notifications() {
        return call(api.notifications(50)).thenApply(p -> p.content);
    }

    public CompletableFuture<Long> unreadCount() {
        return call(api.unreadCount()).thenApply(u -> u.unread);
    }

    public CompletableFuture<Void> markAllSeen() {
        return call(api.markAllSeen());
    }

    private static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
