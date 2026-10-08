package com.styletrack.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.styletrack.customer.data.Appointment
import com.styletrack.customer.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import androidx.compose.material3.Tab as TabItem

/** A booking can still be changed while it is open and hasn't started yet. */
private fun Appointment.canChange() = isOpen && parseDateTime(startTime).isAfter(LocalDateTime.now())

private fun Appointment.isUpcoming() = isOpen && parseDateTime(endTime).isAfter(LocalDateTime.now())

@Composable
fun BookingsScreen(nav: NavController) {
    val repo = LocalRepo.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val vm = rememberLoad("bookings-all") { repo.appointments() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My bookings")
        TabRow(selectedTabIndex = tab) {
            TabItem(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Upcoming") })
            TabItem(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
        }
        Loaded(vm) { all ->
            val list = if (tab == 0) {
                all.filter { it.isUpcoming() }.sortedBy { parseDateTime(it.startTime) }
            } else {
                all.filter { !it.isUpcoming() }.sortedByDescending { parseDateTime(it.startTime) }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (tab == 0) {
                    item {
                        Button(onClick = { nav.navigate(bookRoute()) }, modifier = Modifier.fillMaxWidth()) { Text("New booking") }
                    }
                }
                if (list.isEmpty()) {
                    item {
                        if (tab == 0) EmptyBox("No upcoming bookings", "When you book, it will show up here with its status.")
                        else EmptyBox("No past bookings yet", "Completed and cancelled visits are listed here.")
                    }
                }
                items(list, key = { it.id }) { a -> BookingRow(a) { nav.navigate("booking/${a.id}") } }
            }
        }
    }
}

@Composable
private fun BookingRow(a: Appointment, onClick: () -> Unit) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusChip(a.status)
        }
        Text(fmtWhen(a.startTime), modifier = Modifier.padding(top = 4.dp))
        Text(
            a.stylistName + if (a.isHomeService) " · Home visit" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BookingDetailScreen(nav: NavController, id: Long) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val vm = rememberLoad("booking-$id") { repo.appointment(id) }
    var confirmCancel by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Booking", onBack = { nav.popBackStack() })
        Loaded(vm) { a ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.serviceName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusChip(a.status)
                }

                when (a.status) {
                    "PENDING" -> Text("Waiting for the salon to confirm. We'll notify you as soon as they do.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    "ACCEPTED" -> Text("Your booking is confirmed. See you then!", color = MaterialTheme.colorScheme.primary)
                    "REJECTED" -> Text(
                        "The salon couldn't take this booking" + (a.rejectionReason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ".") +
                            " You're welcome to book another time.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                InfoCard {
                    FactRow("Date", fmtDay(a.startTime))
                    FactRow("Time", "${fmtTime(a.startTime)} – ${fmtTime(a.endTime)}")
                    FactRow("Stylist", a.stylistName)
                    FactRow("Type", if (a.isHomeService) "Home visit" else "At the salon")
                    a.address?.takeIf { a.isHomeService && it.isNotBlank() }?.let { FactRow("Address", it) }
                    a.notes?.takeIf { it.isNotBlank() }?.let { FactRow("Your notes", it) }
                }

                InfoCard {
                    FactRow("Service", peso(a.totalAmount - a.homeServiceFee))
                    if (a.isHomeService) FactRow("Home visit fee", peso(a.homeServiceFee))
                    FactRow("Total", peso(a.totalAmount))
                    FactRow("Payment", if (a.paymentStatus == "PAID") "Paid" + (a.paymentMethod?.let { " ($it)" } ?: "") else "Pay at the salon")
                    if (a.status == "COMPLETED" && a.pointsEarned > 0) FactRow("Points earned", "${a.pointsEarned}")
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (a.canChange()) {
                    Button(onClick = { nav.navigate("reschedule/${a.id}") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Change date or time")
                    }
                    OutlinedButton(onClick = { confirmCancel = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Cancel booking")
                    }
                }
            }

            if (confirmCancel) {
                AlertDialog(
                    onDismissRequest = { confirmCancel = false },
                    title = { Text("Cancel this booking?") },
                    text = { Text("${a.serviceName} on ${fmtWhen(a.startTime)} will be cancelled.") },
                    dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it") } },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmCancel = false
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    repo.cancel(a.id)
                                    snackbar.showSnackbar("Booking cancelled.")
                                    vm.reload(quiet = true)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = e.userMessage()
                                } finally {
                                    busy = false
                                }
                            }
                        }) { Text("Cancel booking", color = MaterialTheme.colorScheme.error) }
                    },
                )
            }
        }
    }
}

@Composable
fun RescheduleScreen(nav: NavController, id: Long) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val vm = rememberLoad("booking-$id") { repo.appointment(id) }
    val stylists = rememberLoad("reschedule-stylists") { repo.stylists() }

    var dateText by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    var stylistId by rememberSaveable { mutableStateOf(0L) }
    var slot by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val date = runCatching { java.time.LocalDate.parse(dateText) }.getOrDefault(java.time.LocalDate.now())

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Change date or time", onBack = { nav.popBackStack() })
        Loaded(vm) { a ->
            Loaded(stylists) { allStylists ->
                val chosenStylist = if (stylistId > 0) stylistId else a.stylistId
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    InfoCard {
                        Text(a.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Currently ${fmtWhen(a.startTime)} with ${a.stylistName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownField(
                        label = "Stylist",
                        options = allStylists.map { it.id to it.fullName },
                        selected = chosenStylist,
                        onSelect = { stylistId = it; slot = null },
                    )
                    SectionTitle("New date")
                    DatePickerButton(date, onChange = { dateText = it.toString(); slot = null })
                    SectionTitle("New time")
                    SlotPicker(chosenStylist, a.serviceId, a.isHomeService, date, slot, onSelect = { slot = it })

                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    Button(
                        enabled = slot != null && !busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val chosen = slot ?: return@Button
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    repo.reschedule(a.id, wireDateTime(date, java.time.LocalTime.parse(chosen)), chosenStylist.takeIf { it != a.stylistId })
                                    snackbar.showSnackbar("Change sent. The salon will confirm the new time.")
                                    nav.popBackStack()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = e.userMessage()
                                    slot = null
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Confirm change")
                    }
                    Text(
                        "A changed booking goes back to waiting for the salon's confirmation.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
