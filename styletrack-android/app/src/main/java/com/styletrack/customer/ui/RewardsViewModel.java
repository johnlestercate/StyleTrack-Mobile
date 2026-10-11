package com.styletrack.customer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.LoyaltyBalance;
import com.styletrack.customer.data.LoyaltyTx;
import com.styletrack.customer.data.Recommendation;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Loadable;

import java.util.List;

public class RewardsViewModel extends AndroidViewModel {
    public final Loadable<LoyaltyBalance> balance;
    public final Loadable<List<LoyaltyTx>> history;
    public final Loadable<List<Recommendation>> recommendations;

    public RewardsViewModel(@NonNull Application application) {
        super(application);
        RepositoryCore repo = ((StyleTrackApp) application).getRepoCore();
        balance = new Loadable<>(this, repo::loyalty);
        history = new Loadable<>(this, repo::loyaltyHistory);
        recommendations = new Loadable<>(this, repo::recommendations);
    }

    public void refreshIfStale() {
        balance.refreshIfStale();
        history.refreshIfStale();
        recommendations.refreshIfStale();
    }
}
