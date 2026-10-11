package com.styletrack.customer.ui;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.styletrack.customer.R;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.Dropdowns;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.SlotPicker;
import com.styletrack.customer.ui.common.UiState;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

/** Replaces RescheduleScreen in BookingsScreen.kt. */
public class RescheduleFragment extends Fragment {

    private RescheduleViewModel vm;
    private NavController nav;
    private long appointmentId;
    private boolean busy;

    private Appointment appointment;
    private List<Stylist> allStylists = new ArrayList<>();
    private boolean stylistsReady;

    private ProgressBar progress;
    private View content;
    private TextView currentService, currentInfo;
    private AutoCompleteTextView stylistField;
    private MaterialButton dateButton, confirm;
    private ProgressBar slotsProgress;
    private TextView slotsMessage, error;
    private ChipGroup slotsChips;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reschedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        appointmentId = getArguments() != null ? getArguments().getLong("appointmentId") : -1L;
        nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "Change date or time", nav::navigateUp);

        vm = new ViewModelProvider(this, new RescheduleViewModel.Factory(requireActivity().getApplication(), appointmentId))
                .get(RescheduleViewModel.class);

        progress = view.findViewById(R.id.progress);
        content = view.findViewById(R.id.content);
        currentService = view.findViewById(R.id.currentService);
        currentInfo = view.findViewById(R.id.currentInfo);
        stylistField = view.findViewById(R.id.stylistField);
        dateButton = view.findViewById(R.id.dateButton);
        slotsProgress = view.findViewById(R.id.slotsProgress);
        slotsMessage = view.findViewById(R.id.slotsMessage);
        slotsChips = view.findViewById(R.id.slotsChips);
        error = view.findViewById(R.id.error);
        confirm = view.findViewById(R.id.confirm);

        dateButton.setOnClickListener(v -> com.styletrack.customer.ui.common.DatePickers.show(requireContext(), vm.date, date -> {
            vm.date = date;
            vm.slot = null;
            renderForm();
        }));
        confirm.setOnClickListener(v -> submit());

        vm.appointment.state().observe(getViewLifecycleOwner(), state -> {
            progress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Ready) {
                appointment = ((UiState.Ready<Appointment>) state).data;
                content.setVisibility(View.VISIBLE);
                currentService.setText(appointment.serviceName);
                currentInfo.setText("Currently " + Formats.fmtWhen(appointment.startTime) + " with " + appointment.stylistName);
                renderForm();
            }
        });
        vm.stylists.state().observe(getViewLifecycleOwner(), state -> {
            stylistsReady = state instanceof UiState.Ready;
            if (stylistsReady) allStylists = ((UiState.Ready<List<Stylist>>) state).data;
            renderForm();
        });
        vm.slots.observe(getViewLifecycleOwner(), state -> {
            long chosenStylist = currentStylistId();
            SlotPicker.render(slotsProgress, slotsMessage, slotsChips, chosenStylist > 0 ? chosenStylist : null,
                    appointment != null ? appointment.serviceId : null, state, vm.slot, slot -> {
                        vm.slot = slot;
                        updateConfirmEnabled();
                    });
            updateConfirmEnabled();
        });
    }

    private long currentStylistId() {
        if (vm.stylistId > 0) return vm.stylistId;
        return appointment != null ? appointment.stylistId : 0L;
    }

    private void renderForm() {
        if (appointment == null || !stylistsReady) return;

        List<Pair<Long, String>> options = new ArrayList<>();
        for (Stylist s : allStylists) options.add(new Pair<>(s.id, s.fullName));
        Dropdowns.bind(stylistField, options, currentStylistId(), id -> {
            vm.stylistId = id;
            vm.slot = null;
            renderForm();
        });

        dateButton.setText(Formats.fmtLongDay(vm.date));
        vm.loadSlots(currentStylistId(), appointment.serviceId, appointment.isHomeService(), vm.date);
        updateConfirmEnabled();
    }

    private void updateConfirmEnabled() {
        confirm.setEnabled(vm.slot != null && !busy);
        confirm.setText(busy ? "Please wait…" : "Confirm change");
    }

    private void submit() {
        if (appointment == null || vm.slot == null || busy) return;
        String chosen = vm.slot;
        busy = true;
        error.setVisibility(View.GONE);
        updateConfirmEnabled();

        long chosenStylist = currentStylistId();
        Long newStylistId = chosenStylist != appointment.stylistId ? chosenStylist : null;
        String startTime = Formats.wireDateTime(vm.date, LocalTime.parse(chosen));

        vm.reschedule(appointment.id, startTime, newStylistId).whenCompleteAsync((a, err) -> {
            busy = false;
            if (!isAdded()) return;
            if (err == null) {
                Snackbar.make(requireView(), "Change sent. The salon will confirm the new time.", Snackbar.LENGTH_LONG).show();
                nav.navigateUp();
            } else {
                Throwable cause = (err instanceof CompletionException && err.getCause() != null) ? err.getCause() : err;
                error.setText(RepositoryCore.userMessage(cause));
                error.setVisibility(View.VISIBLE);
                vm.slot = null;
                renderForm();
            }
            updateConfirmEnabled();
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}
