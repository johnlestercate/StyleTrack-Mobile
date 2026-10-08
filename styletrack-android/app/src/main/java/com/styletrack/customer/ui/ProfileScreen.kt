package com.styletrack.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.styletrack.customer.data.Profile
import com.styletrack.customer.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val repo = LocalRepo.current
    val vm = rememberLoad("profile") { repo.profile() }
    var confirmSignOut by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Profile")
        Loaded(vm) { p ->
            ProfileForm(p, onSaved = { vm.reload(quiet = true) }, onSignOut = { confirmSignOut = true })
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need your mobile number and password to sign back in.") },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Stay signed in") } },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; repo.signOut() }) { Text("Sign out") } },
        )
    }
}

@Composable
private fun ProfileForm(p: Profile, onSaved: () -> Unit, onSignOut: () -> Unit) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(p.id) { mutableStateOf(p.fullName) }
    var email by rememberSaveable(p.id) { mutableStateOf(p.email.orEmpty()) }
    var address by rememberSaveable(p.id) { mutableStateOf(p.address.orEmpty()) }
    var savingProfile by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }

    var current by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var savingPassword by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard {
            FactRow("Mobile number", p.phone)
            FactRow("Member level", tierLabel(p.tier))
        }

        SectionTitle("Your details")
        OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            email, { email = it }, label = { Text("Email (optional)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            address, { address = it }, label = { Text("Home address (used for home visits)") },
            minLines = 2, modifier = Modifier.fillMaxWidth(),
        )
        profileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = name.isNotBlank() && !savingProfile,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                savingProfile = true
                profileError = null
                scope.launch {
                    try {
                        repo.updateProfile(name, email, address)
                        snackbar.showSnackbar("Details saved.")
                        onSaved()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        profileError = e.userMessage()
                    } finally {
                        savingProfile = false
                    }
                }
            },
        ) { Text(if (savingProfile) "Saving…" else "Save details") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SectionTitle("Change password")
        OutlinedTextField(
            current, { current = it }, label = { Text("Current password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            newPassword, { newPassword = it }, label = { Text("New password (8+ characters)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            confirm, { confirm = it }, label = { Text("Repeat new password") }, singleLine = true,
            isError = confirm.isNotEmpty() && confirm != newPassword,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(
            enabled = current.isNotEmpty() && newPassword.length >= 8 && newPassword == confirm && !savingPassword,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                savingPassword = true
                passwordError = null
                scope.launch {
                    try {
                        repo.changePassword(current, newPassword)
                        current = ""; newPassword = ""; confirm = ""
                        snackbar.showSnackbar("Password changed.")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        passwordError = e.userMessage()
                    } finally {
                        savingPassword = false
                    }
                }
            },
        ) { Text(if (savingPassword) "Saving…" else "Change password") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}
