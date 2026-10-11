package com.styletrack.customer.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.styletrack.customer.R;
import com.styletrack.customer.data.SalonService;
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.Avatars;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.SimpleAdapter;
import com.styletrack.customer.ui.common.UiState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrowseFragment extends Fragment {

    private BrowseViewModel vm;
    private int tab = 0;
    private String query = "";

    private TabLayout tabs;
    private TextInputLayout searchLayout;
    private TextInputEditText searchField;
    private ProgressBar progress;
    private View errorGroup;
    private TextView errorText;
    private MaterialButton retryButton;
    private View emptyGroup;
    private TextView emptyTitle, emptyHint;
    private RecyclerView list;

    private SimpleAdapter<SalonService> serviceAdapter;
    private SimpleAdapter<Stylist> stylistAdapter;

    private List<SalonService> allServices = new ArrayList<>();
    private List<Stylist> allStylists = new ArrayList<>();
    private boolean servicesReady, stylistsReady;
    private boolean servicesFailed, stylistsFailed;
    private String servicesError, stylistsError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(BrowseViewModel.class);

        com.styletrack.customer.ui.common.ScreenHeader.bind(view, "Browse", null);

        tabs = view.findViewById(R.id.tabs);
        searchLayout = view.findViewById(R.id.searchLayout);
        searchField = view.findViewById(R.id.searchField);
        progress = view.findViewById(R.id.progress);
        errorGroup = view.findViewById(R.id.errorGroup);
        errorText = view.findViewById(R.id.errorText);
        retryButton = view.findViewById(R.id.retryButton);
        emptyGroup = view.findViewById(R.id.emptyGroup);
        emptyTitle = view.findViewById(R.id.emptyTitle);
        emptyHint = view.findViewById(R.id.emptyHint);
        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        NavController nav = NavHostFragment.findNavController(this);

        serviceAdapter = new SimpleAdapter<>(R.layout.item_service, (row, s) -> {
            ((TextView) row.findViewById(R.id.name)).setText(s.name);
            TextView category = row.findViewById(R.id.category);
            if (s.category != null) {
                category.setText(s.category);
                category.setVisibility(View.VISIBLE);
            } else {
                category.setVisibility(View.GONE);
            }
            ((TextView) row.findViewById(R.id.durationLine)).setText(
                    Formats.fmtDuration(s.durationMinutes) + (s.homeServiceAvailable ? " · Home visit available" : ""));
            ((TextView) row.findViewById(R.id.price)).setText(Formats.peso(s.price));
            row.setOnClickListener(v -> nav.navigate(R.id.serviceDetail, AppNav.idArg("serviceId", s.id)));
        });

        stylistAdapter = new SimpleAdapter<>(R.layout.item_stylist, (row, s) -> {
            Avatars.bind(row.findViewById(R.id.avatar), s.fullName);
            ((TextView) row.findViewById(R.id.fullName)).setText(s.fullName);
            TextView spec = row.findViewById(R.id.specialization);
            if (s.specialization != null) {
                spec.setText(s.specialization);
                spec.setVisibility(View.VISIBLE);
            } else {
                spec.setVisibility(View.GONE);
            }
            row.setOnClickListener(v -> nav.navigate(R.id.stylistDetail, AppNav.idArg("stylistId", s.id)));
        });

        retryButton.setOnClickListener(v -> {
            if (tab == 0) vm.services.reload(); else vm.stylists.reload();
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                searchLayout.setHint(tab == 0 ? "Search services" : "Search stylists");
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {}

            @Override
            public void onTabReselected(TabLayout.Tab t) {}
        });

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString();
                render();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        vm.services.state().observe(getViewLifecycleOwner(), state -> {
            servicesReady = state instanceof UiState.Ready;
            servicesFailed = state instanceof UiState.Failed;
            if (servicesReady) allServices = ((UiState.Ready<List<SalonService>>) state).data;
            if (servicesFailed) servicesError = ((UiState.Failed<List<SalonService>>) state).message;
            render();
        });
        vm.stylists.state().observe(getViewLifecycleOwner(), state -> {
            stylistsReady = state instanceof UiState.Ready;
            stylistsFailed = state instanceof UiState.Failed;
            if (stylistsReady) allStylists = ((UiState.Ready<List<Stylist>>) state).data;
            if (stylistsFailed) stylistsError = ((UiState.Failed<List<Stylist>>) state).message;
            render();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshIfStale();
    }

    private void render() {
        boolean ready = tab == 0 ? servicesReady : stylistsReady;
        boolean failed = tab == 0 ? servicesFailed : stylistsFailed;

        progress.setVisibility(!ready && !failed ? View.VISIBLE : View.GONE);
        errorGroup.setVisibility(failed ? View.VISIBLE : View.GONE);
        if (failed) errorText.setText(tab == 0 ? servicesError : stylistsError);
        emptyGroup.setVisibility(View.GONE);
        list.setVisibility(View.GONE);

        if (!ready) return;

        String q = query.trim().toLowerCase(Locale.ROOT);
        if (tab == 0) {
            List<SalonService> shown = new ArrayList<>();
            for (SalonService s : allServices) {
                if (q.isEmpty() || s.name.toLowerCase(Locale.ROOT).contains(q)
                        || (s.category != null && s.category.toLowerCase(Locale.ROOT).contains(q))) {
                    shown.add(s);
                }
            }
            if (shown.isEmpty()) {
                showEmpty(allServices.isEmpty() ? "The salon hasn't listed any services yet." : "Try a different word.");
            } else {
                list.setAdapter(serviceAdapter);
                serviceAdapter.submit(shown);
                list.setVisibility(View.VISIBLE);
            }
        } else {
            List<Stylist> shown = new ArrayList<>();
            for (Stylist s : allStylists) {
                if (q.isEmpty() || s.fullName.toLowerCase(Locale.ROOT).contains(q)
                        || (s.specialization != null && s.specialization.toLowerCase(Locale.ROOT).contains(q))) {
                    shown.add(s);
                }
            }
            if (shown.isEmpty()) {
                showEmpty(allStylists.isEmpty() ? "No stylists are listed yet." : "Try a different word.");
            } else {
                list.setAdapter(stylistAdapter);
                stylistAdapter.submit(shown);
                list.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showEmpty(String hint) {
        emptyTitle.setText("Nothing found");
        emptyHint.setText(hint);
        emptyGroup.setVisibility(View.VISIBLE);
    }
}
