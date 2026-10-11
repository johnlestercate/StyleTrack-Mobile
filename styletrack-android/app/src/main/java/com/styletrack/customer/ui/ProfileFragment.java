package com.styletrack.customer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.styletrack.customer.R;
import com.styletrack.customer.data.Profile;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.FactRow;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.UiState;

import java.util.concurrent.CompletionException;

/** Replaces ProfileScreen.kt. */
public class ProfileFragment extends Fragment {

    private ProfileViewModel vm;
    private boolean savingProfile, savingPassword;

    private TextInputEditText nameField, emailField, addressField;
    private TextInputEditText currentPasswordField, newPasswordField, confirmPasswordField;
    private com.google.android.material.textfield.TextInputLayout confirmPasswordLayout;
    private TextView profileError, passwordError;
    private MaterialButton saveProfile, changePassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(ProfileViewModel.class);
        ScreenHeader.bind(view, "Profile", null);

        View progress = view.findViewById(R.id.progress);
        View content = view.findViewById(R.id.content);
        nameField = view.findViewById(R.id.nameField);
        emailField = view.findViewById(R.id.emailField);
        addressField = view.findViewById(R.id.addressField);
        profileError = view.findViewById(R.id.profileError);
        saveProfile = view.findViewById(R.id.saveProfile);
        currentPasswordField = view.findViewById(R.id.currentPasswordField);
        newPasswordField = view.findViewById(R.id.newPasswordField);
        confirmPasswordField = view.findViewById(R.id.confirmPasswordField);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout);
        passwordError = view.findViewById(R.id.passwordError);
        changePassword = view.findViewById(R.id.changePassword);
        MaterialButton signOut = view.findViewById(R.id.signOut);

        saveProfile.setOnClickListener(v -> saveProfile());
        changePassword.setOnClickListener(v -> changePassword());
        signOut.setOnClickListener(v -> confirmSignOut());

        android.text.TextWatcher nameWatcher = watcher(() -> saveProfile.setEnabled(!nameField.getText().toString().isBlank() && !savingProfile));
        nameField.addTextChangedListener(nameWatcher);

        android.text.TextWatcher passwordWatcher = watcher(this::updatePasswordButton);
        currentPasswordField.addTextChangedListener(passwordWatcher);
        newPasswordField.addTextChangedListener(passwordWatcher);
        confirmPasswordField.addTextChangedListener(passwordWatcher);

        vm.profile.state().observe(getViewLifecycleOwner(), state -> {
            progress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Ready) {
                content.setVisibility(View.VISIBLE);
                Profile p = ((UiState.Ready<Profile>) state).data;
                if (vm.initializedForId != p.id) {
                    vm.initializedForId = p.id;
                    nameField.setText(p.fullName);
                    emailField.setText(p.email == null ? "" : p.email);
                    addressField.setText(p.address == null ? "" : p.address);
                }
                FactRow.bind(view.findViewById(R.id.rowPhone), "Mobile number", p.phone);
                FactRow.bind(view.findViewById(R.id.rowTier), "Member level", Formats.tierLabel(p.tier));
                saveProfile.setEnabled(!nameField.getText().toString().isBlank() && !savingProfile);
            }
        });
    }

    private android.text.TextWatcher watcher(Runnable onChange) {
        return new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                onChange.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };
    }

    private String text(TextInputEditText f) {
        return f.getText() == null ? "" : f.getText().toString();
    }

    private void updatePasswordButton() {
        String current = text(currentPasswordField);
        String newPassword = text(newPasswordField);
        String confirm = text(confirmPasswordField);
        boolean mismatch = !confirm.isEmpty() && !confirm.equals(newPassword);
        confirmPasswordLayout.setError(mismatch ? "Passwords don't match." : null);
        confirmPasswordLayout.setErrorEnabled(mismatch);
        boolean valid = !current.isEmpty() && newPassword.length() >= 8 && newPassword.equals(confirm);
        changePassword.setEnabled(valid && !savingPassword);
    }

    private void saveProfile() {
        if (text(nameField).isBlank() || savingProfile) return;
        savingProfile = true;
        profileError.setVisibility(View.GONE);
        saveProfile.setEnabled(false);
        saveProfile.setText("Saving…");

        vm.updateProfile(text(nameField), text(emailField), text(addressField)).whenCompleteAsync((p, err) -> {
            savingProfile = false;
            if (!isAdded()) return;
            saveProfile.setText("Save details");
            saveProfile.setEnabled(!text(nameField).isBlank());
            if (err == null) {
                Snackbar.make(requireView(), "Details saved.", Snackbar.LENGTH_LONG).show();
                vm.profile.reload(true);
            } else {
                profileError.setText(RepositoryCore.userMessage(unwrap(err)));
                profileError.setVisibility(View.VISIBLE);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void changePassword() {
        String current = text(currentPasswordField);
        String newPassword = text(newPasswordField);
        String confirm = text(confirmPasswordField);
        if (current.isEmpty() || newPassword.length() < 8 || !newPassword.equals(confirm) || savingPassword) return;
        savingPassword = true;
        passwordError.setVisibility(View.GONE);
        changePassword.setEnabled(false);
        changePassword.setText("Saving…");

        vm.changePassword(current, newPassword).whenCompleteAsync((v, err) -> {
            savingPassword = false;
            if (!isAdded()) return;
            changePassword.setText("Change password");
            if (err == null) {
                currentPasswordField.setText("");
                newPasswordField.setText("");
                confirmPasswordField.setText("");
                Snackbar.make(requireView(), "Password changed.", Snackbar.LENGTH_LONG).show();
            } else {
                passwordError.setText(RepositoryCore.userMessage(unwrap(err)));
                passwordError.setVisibility(View.VISIBLE);
            }
            updatePasswordButton();
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sign out?")
                .setMessage("You'll need your mobile number and password to sign back in.")
                .setNegativeButton("Stay signed in", null)
                .setPositiveButton("Sign out", (d, w) -> vm.signOut())
                .show();
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }
}
