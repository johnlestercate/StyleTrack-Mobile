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
import com.styletrack.customer.data.Stylist;
import com.styletrack.customer.ui.common.Avatars;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.UiState;

import java.util.List;

/** Replaces StylistDetailScreen in BrowseScreen.kt. */
public class StylistDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stylist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long id = getArguments() != null ? getArguments().getLong("stylistId") : -1L;
        NavController nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "Stylist", nav::navigateUp);

        BrowseViewModel vm = new ViewModelProvider(this).get(BrowseViewModel.class);

        ProgressBar progress = view.findViewById(R.id.progress);
        TextView missingText = view.findViewById(R.id.missingText);
        View content = view.findViewById(R.id.content);

        vm.stylists.state().observe(getViewLifecycleOwner(), state -> {
            progress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Failed) {
                missingText.setVisibility(View.VISIBLE);
                content.setVisibility(View.GONE);
                missingText.setText(((UiState.Failed<List<Stylist>>) state).message);
            } else if (state instanceof UiState.Ready) {
                List<Stylist> all = ((UiState.Ready<List<Stylist>>) state).data;
                Stylist s = null;
                for (Stylist x : all) if (x.id == id) { s = x; break; }
                if (s == null) {
                    missingText.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);
                    missingText.setText("This stylist is no longer listed");
                } else {
                    missingText.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    bind(view, s, nav);
                }
            }
        });
    }

    private void bind(View view, Stylist s, NavController nav) {
        Avatars.bind(view.findViewById(R.id.avatar), s.fullName);
        ((TextView) view.findViewById(R.id.fullName)).setText(s.fullName);
        TextView spec = view.findViewById(R.id.specialization);
        if (s.specialization != null) {
            spec.setText(s.specialization);
            spec.setVisibility(View.VISIBLE);
        } else {
            spec.setVisibility(View.GONE);
        }
        TextView bio = view.findViewById(R.id.bio);
        if (s.bio != null && !s.bio.isBlank()) {
            bio.setText(s.bio);
            bio.setVisibility(View.VISIBLE);
        } else {
            bio.setVisibility(View.GONE);
        }

        com.google.android.material.button.MaterialButton bookSalon = view.findViewById(R.id.bookSalon);
        String firstName = s.fullName == null ? "" : s.fullName.split(" ")[0];
        bookSalon.setText("Book with " + firstName);
        bookSalon.setOnClickListener(v -> nav.navigate(R.id.book, AppNav.bookArgs(-1, s.id, false)));

        view.findViewById(R.id.bookHome).setOnClickListener(v ->
                nav.navigate(R.id.book, AppNav.bookArgs(-1, s.id, true)));
    }
}
