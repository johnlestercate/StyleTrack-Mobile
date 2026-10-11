package com.styletrack.customer.ui.common;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.styletrack.customer.data.RepositoryCore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Loads something from the server once, keeps it across rotation (as long as it lives inside an
 * AndroidViewModel), and can reload it. Replaces the Compose LoadVM/rememberLoad/Loaded trio.
 */
public final class Loadable<T> {
    private final AndroidViewModel owner;
    private final Supplier<CompletableFuture<T>> loader;
    private final MutableLiveData<UiState<T>> state = new MutableLiveData<>(new UiState.Loading<>());

    private boolean loading;
    private long lastLoadedMs;

    public Loadable(AndroidViewModel owner, Supplier<CompletableFuture<T>> loader) {
        this.owner = owner;
        this.loader = loader;
        reload(false);
    }

    public LiveData<UiState<T>> state() {
        return state;
    }

    public void reload() {
        reload(false);
    }

    /** quiet keeps showing the current data while refreshing instead of flashing a spinner. */
    public void reload(boolean quiet) {
        if (loading) return;
        loading = true;
        if (!quiet || !(state.getValue() instanceof UiState.Ready)) state.setValue(new UiState.Loading<>());
        loader.get().whenCompleteAsync((data, err) -> {
            loading = false;
            lastLoadedMs = System.currentTimeMillis();
            if (err == null) {
                state.setValue(new UiState.Ready<>(data));
            } else if (!(state.getValue() instanceof UiState.Ready)) {
                state.setValue(new UiState.Failed<>(RepositoryCore.userMessage(unwrap(err))));
            }
        }, ContextCompat.getMainExecutor(owner.getApplication()));
    }

    public void refreshIfStale() {
        if (System.currentTimeMillis() - lastLoadedMs > 3000) reload(true);
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }
}
