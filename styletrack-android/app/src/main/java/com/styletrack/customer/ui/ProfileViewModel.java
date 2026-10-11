package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Profile;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Loadable;

import java.util.concurrent.CompletableFuture;

public class ProfileViewModel extends AndroidViewModel {
    public final Loadable<Profile> profile;
    private final RepositoryCore repo;

    public long initializedForId = -1L;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repo = ((StyleTrackApp) application).getRepoCore();
        profile = new Loadable<>(this, repo::profile);
    }

    public CompletableFuture<Profile> updateProfile(String name, String email, String address) {
        return repo.updateProfile(name, email, address);
    }

    public CompletableFuture<Void> changePassword(String current, String newPassword) {
        return repo.changePassword(current, newPassword);
    }

    public void signOut() {
        repo.signOut();
    }
}
