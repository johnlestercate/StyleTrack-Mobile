package com.styletrack.customer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.styletrack.customer.R;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.ui.common.Appointments;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.SimpleAdapter;
import com.styletrack.customer.ui.common.StatusChip;
import com.styletrack.customer.ui.common.UiState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Replaces BookingsScreen.kt's top-level BookingsScreen composable. */
public class BookingsFragment extends Fragment {

    private BookingsViewModel vm;
    private int tab = 0;

    private MaterialButton newBooking;
    private ProgressBar progress;
    private View errorGroup;
    private TextView errorText, emptyTitle, emptyHint;
    private View emptyGroup;
    private MaterialButton retryButton;
    private RecyclerView list;
    private SimpleAdapter<Appointment> adapter;

    private List<Appointment> all = new ArrayList<>();
    private boolean ready, failed;
    private String errorMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(BookingsViewModel.class);
        NavController nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "My bookings", null);

        TabLayout tabs = view.findViewById(R.id.tabs);
        newBooking = view.findViewById(R.id.newBooking);
        progress = view.findViewById(R.id.progress);
        errorGroup = view.findViewById(R.id.errorGroup);
        errorText = view.findViewById(R.id.errorText);
        retryButton = view.findViewById(R.id.retryButton);
        emptyGroup = view.findViewById(R.id.emptyGroup);
        emptyTitle = view.findViewById(R.id.emptyTitle);
        emptyHint = view.findViewById(R.id.emptyHint);
        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SimpleAdapter<>(R.layout.item_booking, (row, a) -> {
            ((TextView) row.findViewById(R.id.serviceName)).setText(a.serviceName);
            StatusChip.bind(row.findViewById(R.id.status), a.status);
            ((TextView) row.findViewById(R.id.when)).setText(Formats.fmtWhen(a.startTime));
            ((TextView) row.findViewById(R.id.stylistLine)).setText(a.stylistName + (a.isHomeService() ? " · Home visit" : ""));
            row.setOnClickListener(v -> nav.navigate(R.id.bookingDetail, AppNav.idArg("appointmentId", a.id)));
        });

        newBooking.setOnClickListener(v -> nav.navigate(R.id.book, AppNav.bookArgs(-1, -1, false)));
        retryButton.setOnClickListener(v -> vm.appointments.reload());

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {}

            @Override
            public void onTabReselected(TabLayout.Tab t) {}
        });

        vm.appointments.state().observe(getViewLifecycleOwner(), state -> {
            ready = state instanceof UiState.Ready;
            failed = state instanceof UiState.Failed;
            if (ready) all = ((UiState.Ready<List<Appointment>>) state).data;
            if (failed) errorMessage = ((UiState.Failed<List<Appointment>>) state).message;
            render();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.appointments.refreshIfStale();
    }

    private void render() {
        newBooking.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        progress.setVisibility(!ready && !failed ? View.VISIBLE : View.GONE);
        errorGroup.setVisibility(failed ? View.VISIBLE : View.GONE);
        if (failed) errorText.setText(errorMessage);
        emptyGroup.setVisibility(View.GONE);
        list.setVisibility(View.GONE);

        if (!ready) return;

        List<Appointment> shown = new ArrayList<>();
        if (tab == 0) {
            for (Appointment a : all) if (Appointments.isUpcoming(a)) shown.add(a);
            shown.sort(Comparator.comparing(a -> Formats.parseDateTime(a.startTime)));
        } else {
            for (Appointment a : all) if (!Appointments.isUpcoming(a)) shown.add(a);
            shown.sort(Comparator.comparing((Appointment a) -> Formats.parseDateTime(a.startTime)).reversed());
        }

        if (shown.isEmpty()) {
            if (tab == 0) {
                emptyTitle.setText("No upcoming bookings");
                emptyHint.setText("When you book, it will show up here with its status.");
            } else {
                emptyTitle.setText("No past bookings yet");
                emptyHint.setText("Completed and cancelled visits are listed here.");
            }
            emptyGroup.setVisibility(View.VISIBLE);
        } else {
            list.setAdapter(adapter);
            adapter.submit(shown);
            list.setVisibility(View.VISIBLE);
        }
    }
}
