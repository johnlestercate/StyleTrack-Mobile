package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Loadable;

import java.util.List;

public class BookingsViewModel extends AndroidViewModel {
    public final Loadable<List<Appointment>> appointments;

    public BookingsViewModel(@NonNull Application application) {
        super(application);
        RepositoryCore repo = ((StyleTrackApp) application).getRepoCore();
        appointments = new Loadable<>(this, repo::appointments);
    }
}
