# StyleTrack Customer (Android)

Customer mobile app for Jonathan Hair Studio's StyleTrack system. Kotlin + Jetpack Compose (Material 3).
It talks to the StyleTrack Spring Boot backend through the customer API under `/api/me/**`.

**Features:** register / sign in with a mobile number, browse services and stylists, book salon appointments
and home visits, pick a date and an available time, see booking status, cancel or reschedule, view loyalty
points and tier, view appointment history, receive notifications, and see personalised service recommendations.

> **Status: written but not yet built.** This project was produced without an Android SDK available, so it has
> never been compiled or run. Expect to fix a few compile errors on the first Gradle sync (typically a missing
> import or a Compose API that moved between versions).

## Run it

1. Start the backend (the version that includes the customer API, see the backend README, "Customer mobile app API").
   It must be reachable on port 8081 if you use the default below.
2. Open this folder in **Android Studio** (Koala or newer, JDK 17+). Let Gradle sync.
   The wrapper properties pin Gradle 8.9; Android Studio downloads it. If you build from a terminal,
   generate the wrapper jar once with `gradle wrapper` (the jar is not included).
3. Pick the API address in `gradle.properties`:
   - Emulator (default): `apiBaseUrl=http://10.0.2.2:8081/api/`
   - Real phone on the same Wi-Fi: `apiBaseUrl=http://<your computer's LAN IP>:8081/api/`
   - Production: `apiBaseUrl=https://your-domain/api/` (must be HTTPS)
   The URL must end with `/api/`.
4. Run the `app` configuration.

Plain-HTTP traffic is allowed in **debug builds only** (`src/debug/res/xml/network_security_config.xml`);
release builds require HTTPS.

## Accounts

- New customers tap **Create account**. Their mobile number is their username (`0917…` and `+63 917…` are the same number).
- If the salon already has the customer on file, staff can give them an app login with
  `POST /api/customers/{id}/app-account` (there is no button for this in the web app yet), and the customer signs in with that.
- Staff / owner accounts are refused by the app on purpose; they use the web app.

## Notifications

The backend stores a notification when a booking is accepted, rejected, completed, cancelled or moved.
The app checks for new ones with WorkManager roughly **every 15 minutes** (Android's minimum for periodic work)
and shows a phone notification, plus a badge on the Home bell. So updates can arrive up to ~15 minutes late,
and the phone may delay them further in battery-saver mode. True instant push needs Firebase Cloud Messaging
(a Firebase project, `google-services.json`, and a device-token endpoint on the backend), which is not included.

## Project layout

```
app/src/main/java/com/styletrack/customer/
  data/    Models, Retrofit API, Repository, token + session storage
  notify/  Notification channel and the polling worker
  ui/      Theme, shared components, Auth, Home, Browse, Book, Bookings, Rewards, Notifications, Profile
```

## Not included

Appointment reminders before the visit, phone-number verification (OTP), registration rate limiting,
paying through the app (GCash / QR Ph are recorded by the salon at the counter), and push notifications.
