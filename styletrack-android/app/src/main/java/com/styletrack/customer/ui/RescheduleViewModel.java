package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.Loadable;
import com.styletrack.customer.ui.common.UiState;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class RescheduleViewModel extends AndroidViewModel {
    public final Loadable<Appointment> appointment;
    public final Loadable<List<Stylist>> stylists;
    public final MutableLiveData<UiState<List<String>>> slots = new MutableLiveData<>(new UiState.Loading<>());

    public long stylistId; // 0 = "use the booking's current stylist"
    public LocalDate date = LocalDate.now();
    public String slot;

    private final RepositoryCore repo;
    private String lastKey;

    public RescheduleViewModel(@NonNull Application application, long appointmentId) {
        super(application);
        repo = ((StyleTrackApp) application).getRepoCore();
        appointment = new Loadable<>(this, () -> repo.appointment(appointmentId));
        stylists = new Loadable<>(this, repo::stylists);
    }

    public void loadSlots(long stylistId, long serviceId, boolean home, LocalDate date) {
        String key = stylistId + "-" + serviceId + "-" + home + "-" + date;
        if (key.equals(lastKey)) return;
        lastKey = key;
        slots.setValue(new UiState.Loading<>());
        repo.availability(stylistId, date.toString(), serviceId, home).whenCompleteAsync((data, err) -> {
            if (!key.equals(lastKey)) return;
            if (err == null) {
                slots.setValue(new UiState.Ready<>(data));
            } else {
                Throwable cause = (err instanceof CompletionException && err.getCause() != null) ? err.getCause() : err;
                slots.setValue(new UiState.Failed<>(RepositoryCore.userMessage(cause)));
            }
        }, ContextCompat.getMainExecutor(getApplication()));
    }

    public CompletableFuture<Appointment> reschedule(long id, String startTime, Long newStylistId) {
        return repo.reschedule(id, startTime, newStylistId);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;
        private final long appointmentId;

        public Factory(Application application, long appointmentId) {
            this.application = application;
            this.appointmentId = appointmentId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new RescheduleViewModel(application, appointmentId);
        }
    }
}
