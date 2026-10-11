package com.styletrack.customer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.styletrack.customer.R;
import com.styletrack.customer.data.AppNotification;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.SimpleAdapter;
import com.styletrack.customer.ui.common.UiState;

import java.util.List;

/** Replaces NotificationsScreen.kt. */
public class NotificationsFragment extends Fragment {

    private NotificationsViewModel vm;
    private boolean markedSeen;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(NotificationsViewModel.class);
        NavController nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "Notifications", nav::navigateUp);

        View progress = view.findViewById(R.id.progress);
        View errorGroup = view.findViewById(R.id.errorGroup);
        TextView errorText = view.findViewById(R.id.errorText);
        View retryButton = view.findViewById(R.id.retryButton);
        View emptyGroup = view.findViewById(R.id.emptyGroup);
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleAdapter<AppNotification> adapter = new SimpleAdapter<>(R.layout.item_notification, (row, n) -> {
            TextView title = row.findViewById(R.id.title);
            title.setText(n.title);
            title.setTypeface(null, n.seen ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
            ((TextView) row.findViewById(R.id.message)).setText(n.message);
            ((TextView) row.findViewById(R.id.when)).setText(Formats.fmtWhen(n.createdAt));
            row.findViewById(R.id.unseenDot).setVisibility(n.seen ? View.GONE : View.VISIBLE);
            if (n.appointmentId != null) {
                row.setOnClickListener(v -> nav.navigate(R.id.bookingDetail, AppNav.idArg("appointmentId", n.appointmentId)));
                row.setClickable(true);
            } else {
                row.setOnClickListener(null);
                row.setClickable(false);
            }
        });
        list.setAdapter(adapter);
        retryButton.setOnClickListener(v -> vm.notifications.reload());

        vm.notifications.state().observe(getViewLifecycleOwner(), state -> {
            progress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            errorGroup.setVisibility(state instanceof UiState.Failed ? View.VISIBLE : View.GONE);
            emptyGroup.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
            if (state instanceof UiState.Failed) {
                errorText.setText(((UiState.Failed<List<AppNotification>>) state).message);
            } else if (state instanceof UiState.Ready) {
                List<AppNotification> items = ((UiState.Ready<List<AppNotification>>) state).data;
                // Opening the list counts as reading it; rows already on screen keep their "new" dot until next visit.
                if (!markedSeen) {
                    boolean anyUnseen = items.stream().anyMatch(n -> !n.seen);
                    if (anyUnseen) {
                        markedSeen = true;
                        vm.markAllSeen();
                    }
                }
                if (items.isEmpty()) {
                    emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    adapter.submit(items);
                    list.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
