package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.Profile;
import com.styletrack.customer.data.Recommendation;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Loadable;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    public final Loadable<Profile> profile;
    public final Loadable<List<Appointment>> appointments;
    public final Loadable<List<Recommendation>> recommendations;
    public final Loadable<Long> unreadCount;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        RepositoryCore repo = ((StyleTrackApp) application).getRepoCore();
        profile = new Loadable<>(this, repo::profile);
        appointments = new Loadable<>(this, repo::appointments);
        recommendations = new Loadable<>(this, repo::recommendations);
        unreadCount = new Loadable<>(this, repo::unreadCount);
    }

    public void refreshIfStale() {
        profile.refreshIfStale();
        appointments.refreshIfStale();
        recommendations.refreshIfStale();
        unreadCount.refreshIfStale();
    }
}
