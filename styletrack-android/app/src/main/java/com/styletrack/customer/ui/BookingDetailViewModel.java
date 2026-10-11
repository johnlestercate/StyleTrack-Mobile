package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.PayOptions;
import com.styletrack.customer.data.PayStarted;
import com.styletrack.customer.data.PayStatus;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Loadable;

import java.util.concurrent.CompletableFuture;

public class BookingDetailViewModel extends AndroidViewModel {
    public final Loadable<Appointment> appointment;
    public final Loadable<PayOptions> payOptions;
    private final RepositoryCore repo;

    /** Kept here (not serialized) so the open payment dialog survives rotation. */
    public PayStarted payment;

    public BookingDetailViewModel(@NonNull Application application, long appointmentId) {
        super(application);
        repo = ((StyleTrackApp) application).getRepoCore();
        appointment = new Loadable<>(this, () -> repo.appointment(appointmentId));
        payOptions = new Loadable<>(this, repo::payOptions);
    }

    public CompletableFuture<PayStarted> startPayment(long appointmentId, String method) {
        return repo.startPayment(appointmentId, method);
    }

    public CompletableFuture<PayStatus> paymentStatus(long appointmentId) {
        return repo.paymentStatus(appointmentId);
    }

    public CompletableFuture<Appointment> cancel(long id) {
        return repo.cancel(id);
    }

    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final Application application;
        private final long appointmentId;

        public Factory(Application application, long appointmentId) {
            this.application = application;
            this.appointmentId = appointmentId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new BookingDetailViewModel(application, appointmentId);
        }
    }
}
