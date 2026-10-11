package com.styletrack.customer.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.styletrack.customer.R;
import com.styletrack.customer.data.BookRequest;
import com.styletrack.customer.data.Profile;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.SalonService;
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.DatePickers;
import com.styletrack.customer.ui.common.Dropdowns;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.SlotPicker;
import com.styletrack.customer.ui.common.UiState;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

/** Replaces BookScreen.kt. */
public class BookFragment extends Fragment {

    private BookViewModel vm;
    private NavController nav;
    private boolean busy;

    private ChipGroup homeToggle;
    private Chip atSalonChip, homeVisitChip;
    private TextInputLayout serviceLayout, addressLayout;
    private AutoCompleteTextView serviceField, stylistField;
    private TextView durationHint;
    private MaterialButton dateButton;
    private ProgressBar slotsProgress;
    private TextView slotsMessage;
    private ChipGroup slotsChips;
    private TextInputEditText addressField, notesField;
    private TextView error;
    private MaterialButton submit;

    private List<SalonService> allServices = new ArrayList<>();
    private List<Stylist> allStylists = new ArrayList<>();
    private boolean servicesReady, stylistsReady;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(BookViewModel.class);
        nav = NavHostFragment.findNavController(this);

        if (!vm.initialized) {
            Bundle args = getArguments();
            long presetService = args != null ? args.getLong("serviceId", -1L) : -1L;
            long presetStylist = args != null ? args.getLong("stylistId", -1L) : -1L;
            boolean presetHome = args != null && args.getBoolean("home", false);
            vm.serviceId = presetService > 0 ? presetService : 0L;
            vm.stylistId = presetStylist > 0 ? presetStylist : 0L;
            vm.home = presetHome;
            vm.initialized = true;
        }

        ScreenHeader.bind(view, vm.home ? "Book a home visit" : "Book an appointment", nav::navigateUp);

        homeToggle = view.findViewById(R.id.homeToggle);
        atSalonChip = view.findViewById(R.id.atSalonChip);
        homeVisitChip = view.findViewById(R.id.homeVisitChip);
        serviceLayout = view.findViewById(R.id.serviceLayout);
        addressLayout = view.findViewById(R.id.addressLayout);
        serviceField = view.findViewById(R.id.serviceField);
        stylistField = view.findViewById(R.id.stylistField);
        durationHint = view.findViewById(R.id.durationHint);
        dateButton = view.findViewById(R.id.dateButton);
        slotsProgress = view.findViewById(R.id.slotsProgress);
        slotsMessage = view.findViewById(R.id.slotsMessage);
        slotsChips = view.findViewById(R.id.slotsChips);
        addressField = view.findViewById(R.id.addressField);
        notesField = view.findViewById(R.id.notesField);
        error = view.findViewById(R.id.error);
        submit = view.findViewById(R.id.submit);

        atSalonChip.setChecked(!vm.home);
        homeVisitChip.setChecked(vm.home);
        atSalonChip.setOnClickListener(v -> {
            vm.home = false;
            vm.slot = null;
            ScreenHeader.bind(view, "Book an appointment", nav::navigateUp);
            renderForm();
        });
        homeVisitChip.setOnClickListener(v -> {
            vm.home = true;
            vm.slot = null;
            SalonService current = findService(vm.serviceId);
            if (current == null || !current.homeServiceAvailable) vm.serviceId = 0L;
            ScreenHeader.bind(view, "Book a home visit", nav::navigateUp);
            renderForm();
        });

        dateButton.setOnClickListener(v -> DatePickers.show(requireContext(), vm.date, date -> {
            vm.date = date;
            vm.slot = null;
            renderForm();
        }));

        addressField.setText(vm.address);
        addressField.addTextChangedListener(simpleWatcher(s -> {
            vm.address = s;
            vm.addressTouched = true;
            updateSubmitEnabled();
        }));
        notesField.setText(vm.notes);
        notesField.addTextChangedListener(simpleWatcher(s -> vm.notes = s));

        submit.setOnClickListener(v -> submit());

        vm.services.state().observe(getViewLifecycleOwner(), state -> {
            servicesReady = state instanceof UiState.Ready;
            if (servicesReady) allServices = ((UiState.Ready<List<SalonService>>) state).data;
            renderForm();
        });
        vm.stylists.state().observe(getViewLifecycleOwner(), state -> {
            stylistsReady = state instanceof UiState.Ready;
            if (stylistsReady) allStylists = ((UiState.Ready<List<Stylist>>) state).data;
            renderForm();
        });
        vm.profile.state().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof UiState.Ready) {
                Profile p = ((UiState.Ready<Profile>) state).data;
                if (vm.home && !vm.addressTouched && vm.address.isEmpty() && p.address != null && !p.address.isBlank()) {
                    vm.address = p.address;
                    addressField.setText(vm.address);
                }
            }
        });
        vm.slots.observe(getViewLifecycleOwner(), state -> {
            SlotPicker.render(slotsProgress, slotsMessage, slotsChips,
                    vm.stylistId > 0 ? vm.stylistId : null, currentServiceId(), state, vm.slot, slot -> {
                        vm.slot = slot;
                        updateSubmitEnabled();
                    });
            updateSubmitEnabled();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshIfStale();
    }

    private interface StringConsumer {
        void accept(String s);
    }

    private TextWatcher simpleWatcher(StringConsumer consumer) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                consumer.accept(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }

    private SalonService findService(long id) {
        for (SalonService s : allServices) if (s.id == id) return s;
        return null;
    }

    private Long currentServiceId() {
        SalonService s = findService(vm.serviceId);
        return s != null ? s.id : null;
    }

    private void renderForm() {
        if (!servicesReady || !stylistsReady) return;

        List<SalonService> offered = new ArrayList<>();
        for (SalonService s : allServices) if (!vm.home || s.homeServiceAvailable) offered.add(s);

        List<Pair<Long, String>> serviceOptions = new ArrayList<>();
        for (SalonService s : offered) serviceOptions.add(new Pair<>(s.id, s.name + " · " + Formats.peso(s.price)));
        Dropdowns.bind(serviceField, serviceOptions, vm.serviceId > 0 ? vm.serviceId : null, id -> {
            vm.serviceId = id;
            vm.slot = null;
            renderForm();
        });

        SalonService service = findService(vm.serviceId);
        if (service != null) {
            durationHint.setText("About " + Formats.fmtDuration(service.durationMinutes));
            durationHint.setVisibility(View.VISIBLE);
        } else {
            durationHint.setVisibility(View.GONE);
        }

        List<Pair<Long, String>> stylistOptions = new ArrayList<>();
        for (Stylist s : allStylists) stylistOptions.add(new Pair<>(s.id, s.fullName));
        Dropdowns.bind(stylistField, stylistOptions, vm.stylistId > 0 ? vm.stylistId : null, id -> {
            vm.stylistId = id;
            vm.slot = null;
            renderForm();
        });

        dateButton.setText(Formats.fmtLongDay(vm.date));
        addressLayout.setVisibility(vm.home ? View.VISIBLE : View.GONE);

        vm.loadSlots(vm.stylistId > 0 ? vm.stylistId : null, service != null ? service.id : null, vm.home, vm.date);
        updateSubmitEnabled();
    }

    private void updateSubmitEnabled() {
        SalonService service = findService(vm.serviceId);
        boolean ready = service != null && vm.stylistId > 0 && vm.slot != null && (!vm.home || !vm.address.trim().isEmpty());
        submit.setEnabled(ready && !busy);
        submit.setText(busy ? "Please wait…" : "Request booking");
    }

    private void submit() {
        String chosen = vm.slot;
        if (chosen == null || busy) return;
        busy = true;
        error.setVisibility(View.GONE);
        updateSubmitEnabled();

        BookRequest request = new BookRequest(
                vm.stylistId, vm.serviceId,
                vm.home ? "HOME_SERVICE" : "SALON",
                Formats.wireDateTime(vm.date, LocalTime.parse(chosen)),
                vm.home ? vm.address.trim() : null,
                vm.notes.trim().isBlank() ? null : vm.notes.trim());

        vm.book(request).whenCompleteAsync((booked, err) -> {
            busy = false;
            if (!isAdded()) return;
            if (err == null) {
                Snackbar.make(requireView(), "Request sent. The salon will confirm it soon.", Snackbar.LENGTH_LONG).show();
                NavOptions options = new NavOptions.Builder().setPopUpTo(R.id.home, false).build();
                nav.navigate(R.id.bookingDetail, AppNav.idArg("appointmentId", booked.id), options);
            } else {
                Throwable cause = (err instanceof CompletionException && err.getCause() != null) ? err.getCause() : err;
                error.setText(RepositoryCore.userMessage(cause));
                error.setVisibility(View.VISIBLE);
                vm.slot = null; // the chosen time may just have been taken; make them look again
                renderForm();
            }
            updateSubmitEnabled();
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}
