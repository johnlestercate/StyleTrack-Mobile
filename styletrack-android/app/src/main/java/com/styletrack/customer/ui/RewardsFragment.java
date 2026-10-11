package com.styletrack.customer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.styletrack.customer.R;
import com.styletrack.customer.data.LoyaltyBalance;
import com.styletrack.customer.data.LoyaltyTx;
import com.styletrack.customer.data.Recommendation;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.UiState;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Replaces RewardsScreen.kt. */
public class RewardsFragment extends Fragment {

    private static final DateTimeFormatter TX_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private RewardsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(RewardsViewModel.class);
        NavController nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "Rewards", null);

        View balanceProgress = view.findViewById(R.id.balanceProgress);
        View balanceErrorGroup = view.findViewById(R.id.balanceErrorGroup);
        TextView balanceError = view.findViewById(R.id.balanceError);
        View balanceRetry = view.findViewById(R.id.balanceRetry);
        View pointsCard = view.findViewById(R.id.pointsCard);
        balanceRetry.setOnClickListener(v -> vm.balance.reload());

        vm.balance.state().observe(getViewLifecycleOwner(), state -> {
            balanceProgress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            balanceErrorGroup.setVisibility(state instanceof UiState.Failed ? View.VISIBLE : View.GONE);
            pointsCard.setVisibility(state instanceof UiState.Ready ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Failed) balanceError.setText(((UiState.Failed<LoyaltyBalance>) state).message);
            if (state instanceof UiState.Ready) {
                LoyaltyBalance b = ((UiState.Ready<LoyaltyBalance>) state).data;
                HomeFragment.bindPoints(pointsCard, b.balance, b.completedVisits, b.tier, b.visitsToNextTier);
            }
        });

        View recommendationsSection = view.findViewById(R.id.recommendationsSection);
        android.widget.LinearLayout recommendationsList = view.findViewById(R.id.recommendationsList);
        vm.recommendations.state().observe(getViewLifecycleOwner(), state -> {
            if (!(state instanceof UiState.Ready)) return;
            List<Recommendation> recs = ((UiState.Ready<List<Recommendation>>) state).data;
            recommendationsList.removeAllViews();
            if (recs.isEmpty()) {
                recommendationsSection.setVisibility(View.GONE);
                return;
            }
            recommendationsSection.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (Recommendation r : recs) {
                View row = inflater.inflate(R.layout.item_recommendation, recommendationsList, false);
                ((TextView) row.findViewById(R.id.serviceName)).setText(r.serviceName);
                TextView stylist = row.findViewById(R.id.stylistName);
                if (r.stylistName != null) {
                    stylist.setText("with " + r.stylistName);
                    stylist.setVisibility(View.VISIBLE);
                } else {
                    stylist.setVisibility(View.GONE);
                }
                ((TextView) row.findViewById(R.id.message)).setText(r.message);
                row.findViewById(R.id.bookThis).setOnClickListener(v ->
                        nav.navigate(R.id.book, AppNav.bookArgs(r.serviceId, -1, false)));
                recommendationsList.addView(row);
            }
        });

        View historyProgress = view.findViewById(R.id.historyProgress);
        View historyErrorGroup = view.findViewById(R.id.historyErrorGroup);
        TextView historyError = view.findViewById(R.id.historyError);
        View historyRetry = view.findViewById(R.id.historyRetry);
        View historyEmptyGroup = view.findViewById(R.id.historyEmptyGroup);
        android.widget.LinearLayout historyList = view.findViewById(R.id.historyList);
        historyRetry.setOnClickListener(v -> vm.history.reload());

        vm.history.state().observe(getViewLifecycleOwner(), state -> {
            historyProgress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            historyErrorGroup.setVisibility(state instanceof UiState.Failed ? View.VISIBLE : View.GONE);
            historyEmptyGroup.setVisibility(View.GONE);
            historyList.setVisibility(View.GONE);
            historyList.removeAllViews();
            if (state instanceof UiState.Failed) {
                historyError.setText(((UiState.Failed<List<LoyaltyTx>>) state).message);
            } else if (state instanceof UiState.Ready) {
                List<LoyaltyTx> txs = ((UiState.Ready<List<LoyaltyTx>>) state).data;
                if (txs.isEmpty()) {
                    historyEmptyGroup.setVisibility(View.VISIBLE);
                } else {
                    historyList.setVisibility(View.VISIBLE);
                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                    for (LoyaltyTx tx : txs) {
                        View row = inflater.inflate(R.layout.item_loyalty_tx, historyList, false);
                        String desc = (tx.description != null && !tx.description.isBlank()) ? tx.description : txLabel(tx.type);
                        ((TextView) row.findViewById(R.id.description)).setText(desc);
                        ((TextView) row.findViewById(R.id.dateLine)).setText(
                                Formats.parseDateTime(tx.createdAt).format(TX_DATE_FMT) + " · balance " + tx.balanceAfter);
                        TextView points = row.findViewById(R.id.points);
                        points.setText((tx.points > 0 ? "+" : "") + tx.points);
                        points.setTextColor(ContextCompat.getColor(requireContext(),
                                tx.points >= 0 ? R.color.primary : R.color.error));
                        historyList.addView(row);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshIfStale();
    }

    private static String txLabel(String type) {
        switch (type) {
            case "EARNED": return "Points earned";
            case "REDEEMED": return "Points used";
            case "ADJUSTED": return "Adjustment";
            default: return "Points update";
        }
    }
}
