package com.styletrack.customer.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.styletrack.customer.R;
import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.Phones;
import com.styletrack.customer.data.RepositoryCore;

/** Sign-in / create-account screen. Replaces AuthScreen.kt. */
public class AuthFragment extends Fragment {

    private boolean registering = false;
    private boolean busy = false;

    private TextInputLayout nameLayout, phoneLayout, emailLayout, passwordLayout, confirmLayout;
    private TextInputEditText nameField, phoneField, emailField, passwordField, confirmField;
    private android.widget.TextView title, subtitle, error;
    private MaterialButton submit, toggleMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        nameLayout = view.findViewById(R.id.nameLayout);
        phoneLayout = view.findViewById(R.id.phoneLayout);
        emailLayout = view.findViewById(R.id.emailLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        confirmLayout = view.findViewById(R.id.confirmLayout);
        nameField = view.findViewById(R.id.nameField);
        phoneField = view.findViewById(R.id.phoneField);
        emailField = view.findViewById(R.id.emailField);
        passwordField = view.findViewById(R.id.passwordField);
        confirmField = view.findViewById(R.id.confirmField);
        title = view.findViewById(R.id.title);
        subtitle = view.findViewById(R.id.subtitle);
        error = view.findViewById(R.id.error);
        submit = view.findViewById(R.id.submit);
        toggleMode = view.findViewById(R.id.toggleMode);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        nameField.addTextChangedListener(watcher);
        phoneField.addTextChangedListener(watcher);
        emailField.addTextChangedListener(watcher);
        passwordField.addTextChangedListener(watcher);
        confirmField.addTextChangedListener(watcher);

        toggleMode.setOnClickListener(v -> {
            registering = !registering;
            error.setVisibility(View.GONE);
            refresh();
        });
        submit.setOnClickListener(v -> submit());

        refresh();
    }

    private String text(TextInputEditText f) {
        return f.getText() == null ? "" : f.getText().toString();
    }

    private boolean isValid() {
        boolean phoneOk = Phones.normalizePhone(text(phoneField)).length() >= 7;
        if (!phoneOk) return false;
        if (registering) {
            String name = text(nameField).trim();
            String password = text(passwordField);
            String confirm = text(confirmField);
            return !name.isEmpty() && password.length() >= 8 && password.equals(confirm);
        } else {
            return !text(passwordField).isEmpty();
        }
    }

    private void refresh() {
        nameLayout.setVisibility(registering ? View.VISIBLE : View.GONE);
        emailLayout.setVisibility(registering ? View.VISIBLE : View.GONE);
        confirmLayout.setVisibility(registering ? View.VISIBLE : View.GONE);
        passwordLayout.setHelperText(registering ? "At least 8 characters." : null);

        subtitle.setText(registering
                ? "Create your account to book appointments and collect rewards."
                : "Sign in to book appointments, track your points and see your history.");

        String confirm = text(confirmField);
        String password = text(passwordField);
        boolean mismatch = registering && !confirm.isEmpty() && !confirm.equals(password);
        confirmLayout.setError(mismatch ? "Passwords don't match." : null);
        confirmLayout.setErrorEnabled(mismatch);

        submit.setEnabled(isValid() && !busy);
        submit.setText(busy ? (registering ? "Creating account…" : "Signing in…")
                : (registering ? "Create account" : "Sign in"));
        toggleMode.setText(registering ? "I already have an account" : "New here? Create an account");
    }

    private void submit() {
        if (!isValid() || busy) return;
        busy = true;
        error.setVisibility(View.GONE);
        refresh();

        StyleTrackApp app = (StyleTrackApp) requireActivity().getApplication();
        RepositoryCore repo = app.getRepoCore();
        String phone = text(phoneField);
        String password = text(passwordField);

        java.util.concurrent.CompletableFuture<Void> future;
        if (registering) {
            String name = text(nameField);
            String email = text(emailField);
            future = repo.register(name, phone, email.isBlank() ? null : email, password);
        } else {
            future = repo.login(phone, password);
        }
        future.whenCompleteAsync((v, err) -> {
            busy = false;
            if (!isAdded()) return;
            if (err != null) {
                Throwable cause = (err instanceof java.util.concurrent.CompletionException && err.getCause() != null) ? err.getCause() : err;
                error.setText(RepositoryCore.userMessage(cause));
                error.setVisibility(View.VISIBLE);
            }
            refresh();
            // success: the session LiveData changes and MainActivity swaps to the app
        }, androidx.core.content.ContextCompat.getMainExecutor(requireContext()));
    }
}
