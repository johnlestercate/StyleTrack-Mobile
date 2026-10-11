package com.styletrack.customer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.styletrack.customer.R;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.LoyaltyBalance;
import com.styletrack.customer.data.Profile;
import com.styletrack.customer.ui.common.Appointments;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.StatusChip;
import com.styletrack.customer.ui.common.UiState;

import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel vm;

    private TextView greetingName, profileError;
    private TextView unreadBadge;
    private ImageButton notificationsButton;
    private MaterialButton bookSalon, bookHome;

    private ProgressBar nextVisitProgress;
    private View nextVisitErrorGroup;
    private TextView nextVisitError;
    private MaterialButton nextVisitRetry;
    private View nextVisitCard;
    private TextView nextVisitService, nextVisitStatus, nextVisitWhen, nextVisitPendingHint;
    private View nextVisitHome;
    private View noUpcomingCard;

    private View rewardsSection;
    private View recommendationsSection;
    private LinearLayout recommendationsList;

    private Appointment lastAppointments;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        greetingName = view.findViewById(R.id.greetingName);
        profileError = view.findViewById(R.id.profileError);
        unreadBadge = view.findViewById(R.id.unreadBadge);
        notificationsButton = view.findViewById(R.id.notificationsButton);
        bookSalon = view.findViewById(R.id.bookSalon);
        bookHome = view.findViewById(R.id.bookHome);

        nextVisitProgress = view.findViewById(R.id.nextVisitProgress);
        nextVisitErrorGroup = view.findViewById(R.id.nextVisitErrorGroup);
        nextVisitError = view.findViewById(R.id.nextVisitError);
        nextVisitRetry = view.findViewById(R.id.nextVisitRetry);
        nextVisitCard = view.findViewById(R.id.nextVisitCard);
        nextVisitService = view.findViewById(R.id.nextVisitService);
        nextVisitStatus = view.findViewById(R.id.nextVisitStatus);
        nextVisitWhen = view.findViewById(R.id.nextVisitWhen);
        nextVisitHome = view.findViewById(R.id.nextVisitHome);
        nextVisitPendingHint = view.findViewById(R.id.nextVisitPendingHint);
        noUpcomingCard = view.findViewById(R.id.noUpcomingCard);

        rewardsSection = view.findViewById(R.id.rewardsSection);
        recommendationsSection = view.findViewById(R.id.recommendationsSection);
        recommendationsList = view.findViewById(R.id.recommendationsList);

        notificationsButton.setOnClickListener(v -> nav().navigate(R.id.notifications));
        bookSalon.setOnClickListener(v -> nav().navigate(R.id.book, AppNav.bookArgs(-1, -1, false)));
        bookHome.setOnClickListener(v -> nav().navigate(R.id.book, AppNav.bookArgs(-1, -1, true)));
        nextVisitRetry.setOnClickListener(v -> vm.appointments.reload());

        vm.profile.state().observe(getViewLifecycleOwner(), this::onProfile);
        vm.appointments.state().observe(getViewLifecycleOwner(), this::onAppointments);
        vm.recommendations.state().observe(getViewLifecycleOwner(), this::onRecommendations);
        vm.unreadCount.state().observe(getViewLifecycleOwner(), this::onUnread);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshIfStale();
    }

    private androidx.navigation.NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    private void onProfile(UiState<Profile> state) {
        Profile me = (state instanceof UiState.Ready) ? ((UiState.Ready<Profile>) state).data : null;
        if (me != null) {
            String first = me.fullName == null ? "" : me.fullName.split(" ")[0];
            greetingName.setText(first);
            rewardsSection.setVisibility(View.VISIBLE);
            bindPoints(getView().findViewById(R.id.pointsCard), me.loyaltyPoints, me.completedVisits, me.tier, me.visitsToNextTier);
            getView().findViewById(R.id.pointsCard).setOnClickListener(v -> nav().navigate(R.id.rewards));
            profileError.setVisibility(View.GONE);
        } else {
            rewardsSection.setVisibility(View.GONE);
            if (state instanceof UiState.Failed) {
                profileError.setText(((UiState.Failed<Profile>) state).message);
                profileError.setVisibility(View.VISIBLE);
            }
        }
    }

    private void onAppointments(UiState<List<Appointment>> state) {
        nextVisitProgress.setVisibility(View.GONE);
        nextVisitErrorGroup.setVisibility(View.GONE);
        nextVisitCard.setVisibility(View.GONE);
        noUpcomingCard.setVisibility(View.GONE);

        if (state instanceof UiState.Ready) {
            List<Appointment> all = ((UiState.Ready<List<Appointment>>) state).data;
            Appointment next = Appointments.nextAppointment(all);
            if (next != null) {
                bindNextVisit(next);
                nextVisitCard.setVisibility(View.VISIBLE);
            } else {
                noUpcomingCard.setVisibility(View.VISIBLE);
            }
        } else if (state instanceof UiState.Loading) {
            nextVisitProgress.setVisibility(View.VISIBLE);
        } else if (state instanceof UiState.Failed) {
            nextVisitErrorGroup.setVisibility(View.VISIBLE);
            nextVisitError.setText(((UiState.Failed<List<Appointment>>) state).message);
        }
    }

    private void bindNextVisit(Appointment a) {
        nextVisitService.setText(a.serviceName);
        StatusChip.bind(nextVisitStatus, a.status);
        nextVisitWhen.setText(Formats.fmtWhen(a.startTime) + " · " + a.stylistName);
        nextVisitHome.setVisibility(a.isHomeService() ? View.VISIBLE : View.GONE);
        nextVisitPendingHint.setVisibility("PENDING".equals(a.status) ? View.VISIBLE : View.GONE);
        nextVisitCard.setOnClickListener(v -> nav().navigate(R.id.bookingDetail, AppNav.idArg("appointmentId", a.id)));
    }

    private void onRecommendations(UiState<List<com.styletrack.customer.data.Recommendation>> state) {
        if (!(state instanceof UiState.Ready)) return;
        List<com.styletrack.customer.data.Recommendation> recs = ((UiState.Ready<List<com.styletrack.customer.data.Recommendation>>) state).data;
        recommendationsList.removeAllViews();
        if (recs.isEmpty()) {
            recommendationsSection.setVisibility(View.GONE);
            return;
        }
        recommendationsSection.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (com.styletrack.customer.data.Recommendation r : recs) {
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
                    nav().navigate(R.id.book, AppNav.bookArgs(r.serviceId, -1, false)));
            recommendationsList.addView(row);
        }
    }

    private void onUnread(UiState<Long> state) {
        long count = (state instanceof UiState.Ready) ? ((UiState.Ready<Long>) state).data : 0L;
        if (count > 0) {
            unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            unreadBadge.setVisibility(View.VISIBLE);
        } else {
            unreadBadge.setVisibility(View.GONE);
        }
    }

    static void bindPoints(View card, int points, int visits, String tier, int toNext) {
        ((TextView) card.findViewById(R.id.points)).setText(String.valueOf(points));
        ((TextView) card.findViewById(R.id.tier)).setText(Formats.tierLabel(tier));
        ProgressBar progress = card.findViewById(R.id.tierProgress);
        TextView hint = card.findViewById(R.id.tierHint);
        if (toNext > 0) {
            int target = visits + toNext;
            progress.setVisibility(View.VISIBLE);
            progress.setMax(Math.max(target, 1));
            progress.setProgress(visits);
            hint.setText(toNext + " more " + (toNext == 1 ? "visit" : "visits") + " to reach the next level");
        } else {
            progress.setVisibility(View.GONE);
            hint.setText("You've reached our top level. Thank you!");
        }
    }
}
