package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.AppNotification;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Loadable;

import java.util.List;

public class NotificationsViewModel extends AndroidViewModel {
    public final Loadable<List<AppNotification>> notifications;
    private final RepositoryCore repo;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        repo = ((StyleTrackApp) application).getRepoCore();
        notifications = new Loadable<>(this, repo::notifications);
    }

    /** Fire-and-forget; not critical if it fails - the items just stay unread and show again next time. */
    public void markAllSeen() {
        repo.markAllSeen();
    }
}
