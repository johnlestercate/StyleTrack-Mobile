package com.styletrack.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.styletrack.customer.data.BookRequest
import com.styletrack.customer.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun BookScreen(nav: NavController, presetService: Long?, presetStylist: Long?, presetHome: Boolean) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    val services = rememberLoad("book-services") { repo.services() }
    val stylists = rememberLoad("book-stylists") { repo.stylists() }
    val profile = rememberLoad("book-profile") { repo.profile() }

    // 0 means "nothing chosen yet" so the values survive rotation as plain Longs
    var serviceId by rememberSaveable { mutableStateOf(presetService ?: 0L) }
    var stylistId by rememberSaveable { mutableStateOf(presetStylist ?: 0L) }
    var home by rememberSaveable { mutableStateOf(presetHome) }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var slot by rememberSaveable { mutableStateOf<String?>(null) }
    var address by rememberSaveable { mutableStateOf("") }
    var addressTouched by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.now())

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (home) "Book a home visit" else "Book an appointment", onBack = { nav.popBackStack() })
        Loaded(services) { allServices ->
            Loaded(stylists) { allStylists ->
                // Pre-fill the home address from the profile once, unless the customer started typing
                val profileState by profile.state.collectAsState()
                val saved = (profileState as? UiState.Ready)?.data?.address
                LaunchedEffect(home, saved) {
                    if (home && !addressTouched && address.isEmpty() && !saved.isNullOrBlank()) address = saved
                }

                val offered = allServices.filter { !home || it.homeServiceAvailable }
                val service = offered.firstOrNull { it.id == serviceId }

                Column(
                    Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !home,
                            onClick = { home = false; slot = null },
                            label = { Text("At the salon") },
                        )
                        FilterChip(
                            selected = home,
                            onClick = {
                                home = true
                                slot = null
                                // a service without home visits can't stay selected
                                if (allServices.firstOrNull { it.id == serviceId }?.homeServiceAvailable != true) serviceId = 0L
                            },
                            label = { Text("Home visit") },
                        )
                    }

                    DropdownField(
                        label = "Service",
                        options = offered.map { it.id to "${it.name} · ${peso(it.price)}" },
                        selected = serviceId.takeIf { it > 0 },
                        onSelect = { serviceId = it; slot = null },
                    )
                    if (service != null) {
                        Text(
                            "About ${fmtDuration(service.durationMinutes)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    DropdownField(
                        label = "Stylist",
                        options = allStylists.map { it.id to it.fullName },
                        selected = stylistId.takeIf { it > 0 },
                        onSelect = { stylistId = it; slot = null },
                    )

                    SectionTitle("Date")
                    DatePickerButton(date, onChange = { dateText = it.toString(); slot = null })

                    SectionTitle("Time")
                    SlotPicker(
                        stylistId = stylistId.takeIf { it > 0 },
                        serviceId = service?.id,
                        home = home,
                        date = date,
                        selected = slot,
                        onSelect = { slot = it },
                    )

                    if (home) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it; addressTouched = true },
                            label = { Text("Address for the visit") },
                            supportingText = { Text("A home visit may include a travel fee. The salon will confirm it.") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes for the stylist (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    val ready = service != null && stylistId > 0 && slot != null && (!home || address.isNotBlank())
                    Button(
                        enabled = ready && !busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val chosen = slot ?: return@Button
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val booked = repo.book(
                                        BookRequest(
                                            stylistId = stylistId,
                                            serviceId = serviceId,
                                            type = if (home) "HOME_SERVICE" else "SALON",
                                            startTime = wireDateTime(date, LocalTime.parse(chosen)),
                                            address = if (home) address.trim() else null,
                                            notes = notes.trim().ifBlank { null },
                                        ),
                                    )
                                    snackbar.showSnackbar("Request sent. The salon will confirm it soon.")
                                    nav.navigate("booking/${booked.id}") {
                                        popUpTo("home")
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = e.userMessage()
                                    slot = null // the chosen time may just have been taken; make them look again
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Request booking")
                    }
                    Text(
                        "Your booking stays pending until the salon confirms it.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
