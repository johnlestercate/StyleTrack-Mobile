package com.styletrack.customer;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.styletrack.customer.data.SessionState;
import com.styletrack.customer.notify.NotificationWorker;
import com.styletrack.customer.notify.Notifier;
import com.styletrack.customer.ui.AuthFragment;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final Set<Integer> TAB_IDS = Set.of(
            R.id.home, R.id.browse, R.id.bookings, R.id.rewards, R.id.profile);

    private View loading;
    private FrameLayout authContainer;
    private View appContainer;
    private BottomNavigationView bottomNav;
    private boolean askedNotificationPermission;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        loading = findViewById(R.id.loading);
        authContainer = findViewById(R.id.authContainer);
        appContainer = findViewById(R.id.appContainer);
        bottomNav = findViewById(R.id.bottomNav);

        notificationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

        setupBottomNav();

        StyleTrackApp app = (StyleTrackApp) getApplication();
        app.getRepoCore().restoreSession();

        app.getSession().getState().observe(this, this::onSessionState);
    }

    private void setupBottomNav() {
        FragmentContainerView navHost = findViewById(R.id.navHostFragment);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHostFragment == null) return;
        NavController nav = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, nav);
        nav.addOnDestinationChangedListener((controller, destination, arguments) ->
                bottomNav.setVisibility(isTab(destination) ? View.VISIBLE : View.GONE));
    }

    private boolean isTab(NavDestination destination) {
        for (NavDestination d = destination; d != null; d = d.getParent()) {
            if (TAB_IDS.contains(d.getId())) return true;
        }
        return false;
    }

    private void onSessionState(SessionState state) {
        StyleTrackApp app = (StyleTrackApp) getApplication();
        if (state instanceof SessionState.StartingState) {
            loading.setVisibility(View.VISIBLE);
            authContainer.setVisibility(View.GONE);
            appContainer.setVisibility(View.GONE);
        } else if (state instanceof SessionState.SignedOutState) {
            loading.setVisibility(View.GONE);
            authContainer.setVisibility(View.VISIBLE);
            appContainer.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.authContainer, new AuthFragment())
                    .commitAllowingStateLoss();
            NotificationWorker.stop(this);
        } else if (state instanceof SessionState.SignedIn) {
            loading.setVisibility(View.GONE);
            authContainer.setVisibility(View.GONE);
            appContainer.setVisibility(View.VISIBLE);
            NotificationWorker.start(this);
            askForNotificationPermissionOnce();
        }
    }

    private void askForNotificationPermissionOnce() {
        if (askedNotificationPermission) return;
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.hasPermission(this)) {
            askedNotificationPermission = true;
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
