package com.styletrack.customer.ui.common;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;

import java.util.function.Consumer;

/**
 * Wires a Loadable's state to the common 3-way layout: a ProgressBar, an error TextView + "Try
 * again" Button, and the real content view. Each fragment's layout should include a view with this
 * shape (progress/error/content siblings) wherever it shows a Loadable.
 */
public final class LoadBinding {
    private LoadBinding() {}

    public static <T> void bind(
            LifecycleOwner owner,
            Loadable<T> loadable,
            View progress,
            View errorGroup,
            TextView errorText,
            Button retryButton,
            View content,
            Consumer<T> onReady) {
        if (retryButton != null) retryButton.setOnClickListener(v -> loadable.reload());
        loadable.state().observe(owner, state -> {
            if (state instanceof UiState.Loading) {
                progress.setVisibility(View.VISIBLE);
                errorGroup.setVisibility(View.GONE);
                content.setVisibility(View.GONE);
            } else if (state instanceof UiState.Failed) {
                progress.setVisibility(View.GONE);
                errorGroup.setVisibility(View.VISIBLE);
                content.setVisibility(View.GONE);
                errorText.setText(((UiState.Failed<T>) state).message);
            } else if (state instanceof UiState.Ready) {
                progress.setVisibility(View.GONE);
                errorGroup.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                onReady.accept(((UiState.Ready<T>) state).data);
            }
        });
    }
}
