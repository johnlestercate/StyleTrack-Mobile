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

import com.styletrack.customer.R;
import com.styletrack.customer.data.SalonService;
import com.styletrack.customer.ui.common.FactRow;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.UiState;

import java.util.List;

/** Replaces ServiceDetailScreen in BrowseScreen.kt. */
public class ServiceDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_service_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long id = getArguments() != null ? getArguments().getLong("serviceId") : -1L;
        NavController nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "Service", nav::navigateUp);

        BrowseViewModel vm = new ViewModelProvider(this).get(BrowseViewModel.class);

        ProgressBar progress = view.findViewById(R.id.progress);
        TextView missingText = view.findViewById(R.id.missingText);
        View content = view.findViewById(R.id.content);

        vm.services.state().observe(getViewLifecycleOwner(), state -> {
            progress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Failed) {
                missingText.setVisibility(View.VISIBLE);
                content.setVisibility(View.GONE);
                missingText.setText(((UiState.Failed<List<SalonService>>) state).message);
            } else if (state instanceof UiState.Ready) {
                List<SalonService> all = ((UiState.Ready<List<SalonService>>) state).data;
                SalonService s = null;
                for (SalonService x : all) if (x.id == id) { s = x; break; }
                if (s == null) {
                    missingText.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);
                    missingText.setText("This service is no longer available");
                } else {
                    missingText.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    bind(view, s, nav);
                }
            }
        });
    }

    private void bind(View view, SalonService s, NavController nav) {
        ((TextView) view.findViewById(R.id.name)).setText(s.name);
        TextView category = view.findViewById(R.id.category);
        if (s.category != null) {
            category.setText(s.category);
            category.setVisibility(View.VISIBLE);
        } else {
            category.setVisibility(View.GONE);
        }
        FactRow.bind(view.findViewById(R.id.priceRow), "Price", Formats.peso(s.price));
        FactRow.bind(view.findViewById(R.id.durationRow), "Time needed", Formats.fmtDuration(s.durationMinutes));
        FactRow.bind(view.findViewById(R.id.homeVisitRow), "Home visit", s.homeServiceAvailable ? "Available" : "Salon only");
        if (s.pointsAwarded != null && s.pointsAwarded > 0) {
            FactRow.bind(view.findViewById(R.id.pointsRow), "Points earned", s.pointsAwarded + " points");
        } else {
            FactRow.hide(view.findViewById(R.id.pointsRow));
        }

        TextView description = view.findViewById(R.id.description);
        if (s.description != null && !s.description.isBlank()) {
            description.setText(s.description);
            description.setVisibility(View.VISIBLE);
        } else {
            description.setVisibility(View.GONE);
        }

        view.findViewById(R.id.bookSalon).setOnClickListener(v ->
                nav.navigate(R.id.book, AppNav.bookArgs(s.id, -1, false)));

        View bookHome = view.findViewById(R.id.bookHome);
        if (s.homeServiceAvailable) {
            bookHome.setVisibility(View.VISIBLE);
            bookHome.setOnClickListener(v -> nav.navigate(R.id.book, AppNav.bookArgs(s.id, -1, true)));
        } else {
            bookHome.setVisibility(View.GONE);
        }
    }
}
