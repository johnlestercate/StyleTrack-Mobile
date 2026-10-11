package com.styletrack.customer.notify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.ApiException;
import com.styletrack.customer.data.AppNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Checks for new booking updates in the background and shows them as phone notifications.
 * Android allows background checks at most every 15 minutes, so alerts can arrive a little late; instant
 * delivery needs Firebase Cloud Messaging (see the README).
 */
public class NotificationWorker extends Worker {
    private static final String PERIODIC = "styletrack-notification-check";
    private static final String NOW = "styletrack-notification-check-now";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        StyleTrackApp app = (StyleTrackApp) getApplicationContext();
        if (app.getTokens().getToken() == null) return Result.success();
        try {
            List<AppNotification> items = app.getRepoCore().notifications().get(); // newest first
            long newest = 0L;
            for (AppNotification n : items) newest = Math.max(newest, n.id);
            long last = app.getTokens().getLastNotifiedId();
            if (last < 0) {
                // first run after signing in: remember where we are, don't announce old items
                app.getTokens().setLastNotifiedId(newest);
                return Result.success();
            }
            List<AppNotification> toShow = new ArrayList<>();
            for (AppNotification n : items) {
                if (!n.seen && n.id > last) toShow.add(n);
            }
            toShow.sort((a, b) -> Long.compare(a.id, b.id));
            for (AppNotification n : toShow) Notifier.show(getApplicationContext(), n);
            if (newest > last) app.getTokens().setLastNotifiedId(newest);
            return Result.success();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApiException) {
                int code = ((ApiException) cause).getCode();
                return (code == 401 || code == 403) ? Result.success() : Result.retry();
            }
            return Result.retry();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    public static void start(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        WorkManager manager = WorkManager.getInstance(context);
        manager.enqueueUniqueWork(
                NOW, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(NotificationWorker.class).setConstraints(constraints).build());
        manager.enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES).setConstraints(constraints).build());
    }

    public static void stop(Context context) {
        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelUniqueWork(PERIODIC);
        manager.cancelUniqueWork(NOW);
    }
}
