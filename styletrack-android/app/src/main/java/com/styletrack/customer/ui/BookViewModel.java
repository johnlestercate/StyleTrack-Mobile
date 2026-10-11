package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Profile;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.SalonService;
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.Loadable;
import com.styletrack.customer.ui.common.UiState;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionException;

public class BookViewModel extends AndroidViewModel {
    public final Loadable<List<SalonService>> services;
    public final Loadable<List<Stylist>> stylists;
    public final Loadable<Profile> profile;
    public final MutableLiveData<UiState<List<String>>> slots = new MutableLiveData<>(new UiState.Loading<>());

    private final RepositoryCore repo;
    private String lastKey;

    // Form state. Living in the ViewModel (rather than the fragment) is what makes it survive rotation.
    public long serviceId;
    public long stylistId;
    public boolean home;
    public LocalDate date = LocalDate.now();
    public String slot;
    public String address = "";
    public boolean addressTouched;
    public String notes = "";
    public boolean initialized;

    public BookViewModel(@NonNull Application application) {
        super(application);
        repo = ((StyleTrackApp) application).getRepoCore();
        services = new Loadable<>(this, repo::services);
        stylists = new Loadable<>(this, repo::stylists);
        profile = new Loadable<>(this, repo::profile);
    }

    public java.util.concurrent.CompletableFuture<com.styletrack.customer.data.Appointment> book(com.styletrack.customer.data.BookRequest request) {
        return repo.book(request);
    }

    public void refreshIfStale() {
        services.refreshIfStale();
        stylists.refreshIfStale();
        profile.refreshIfStale();
    }

    /** Mirrors the Compose SlotPicker: reloads whenever stylist/service/home/date change. */
    public void loadSlots(Long stylistId, Long serviceId, boolean home, LocalDate date) {
        if (stylistId == null || serviceId == null) {
            lastKey = null;
            slots.setValue(new UiState.Ready<>(List.of()));
            return;
        }
        String key = stylistId + "-" + serviceId + "-" + home + "-" + date;
        if (key.equals(lastKey)) return;
        lastKey = key;
        slots.setValue(new UiState.Loading<>());
        repo.availability(stylistId, date.toString(), serviceId, home).whenCompleteAsync((data, err) -> {
            if (!key.equals(lastKey)) return; // a newer request has already superseded this one
            if (err == null) {
                slots.setValue(new UiState.Ready<>(data));
            } else {
                Throwable cause = (err instanceof CompletionException && err.getCause() != null) ? err.getCause() : err;
                slots.setValue(new UiState.Failed<>(RepositoryCore.userMessage(cause)));
            }
        }, ContextCompat.getMainExecutor(getApplication()));
    }
}
