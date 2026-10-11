package com.styletrack.customer.ui.common;

/** Loading / failed / ready, for anything a screen loads from the server. Replaces the Compose sealed UiState. */
public abstract class UiState<T> {
    private UiState() {}

    public static final class Loading<T> extends UiState<T> {
    }

    public static final class Failed<T> extends UiState<T> {
        public final String message;

        public Failed(String message) {
            this.message = message;
        }
    }

    public static final class Ready<T> extends UiState<T> {
        public final T data;

        public Ready(T data) {
            this.data = data;
        }
    }
}
