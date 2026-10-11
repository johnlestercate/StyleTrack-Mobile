package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.SalonService;
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.Loadable;

import java.util.List;

public class BrowseViewModel extends AndroidViewModel {
    public final Loadable<List<SalonService>> services;
    public final Loadable<List<Stylist>> stylists;

    public BrowseViewModel(@NonNull Application application) {
        super(application);
        RepositoryCore repo = ((StyleTrackApp) application).getRepoCore();
        services = new Loadable<>(this, repo::services);
        stylists = new Loadable<>(this, repo::stylists);
    }

    public void refreshIfStale() {
        services.refreshIfStale();
        stylists.refreshIfStale();
    }
}
