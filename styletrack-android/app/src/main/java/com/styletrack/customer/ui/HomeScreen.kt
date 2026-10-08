package com.styletrack.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.styletrack.customer.data.Appointment
import com.styletrack.customer.data.LoyaltyBalance
import com.styletrack.customer.data.Profile
import java.time.LocalDateTime

@Composable
fun HomeScreen(nav: NavController) {
    val repo = LocalRepo.current
    val profile = rememberLoad("home-profile") { repo.profile() }
    val appointments = rememberLoad("home-appointments") { repo.appointments() }
    val recommendations = rememberLoad("home-recommendations") { repo.recommendations() }
    val unread = rememberLoad("home-unread") { repo.unreadCount() }

    val profileState by profile.state.collectAsState()
    val apptState by appointments.state.collectAsState()
    val recState by recommendations.state.collectAsState()
    val unreadState by unread.state.collectAsState()

    val me = (profileState as? UiState.Ready)?.data
    val next = (apptState as? UiState.Ready)?.data?.let { nextAppointment(it) }
    val unreadCount = (unreadState as? UiState.Ready)?.data ?: 0L

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hello,", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        me?.fullName?.substringBefore(' ') ?: "",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = { nav.navigate("notifications") }) {
                    BadgedBox(badge = { if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) } }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
            }
        }

        if (profileState is UiState.Failed && me == null) {
            item { ErrorBox((profileState as UiState.Failed).message, onRetry = { profile.reload() }, modifier = Modifier.fillMaxWidth()) }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { nav.navigate(bookRoute()) }, modifier = Modifier.weight(1f)) { Text("Book at the salon") }
                OutlinedButton(onClick = { nav.navigate(bookRoute(home = true)) }, modifier = Modifier.weight(1f)) { Text("Book a home visit") }
            }
        }

        item { SectionTitle("Your next visit") }
        item {
            when {
                next != null -> NextVisitCard(next) { nav.navigate("booking/${next.id}") }
                apptState is UiState.Loading -> LoadingBox(Modifier.fillMaxWidth().padding(16.dp))
                apptState is UiState.Failed -> ErrorBox((apptState as UiState.Failed).message, { appointments.reload() }, Modifier.fillMaxWidth())
                else -> InfoCard {
                    Text("No upcoming bookings", style = MaterialTheme.typography.titleMedium)
                    Text("Pick a service and a time that suits you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (me != null) {
            item { SectionTitle("Rewards") }
            item { PointsCard(me) { nav.navigate("rewards") } }
        }

        val recs = (recState as? UiState.Ready)?.data.orEmpty()
        if (recs.isNotEmpty()) {
            item { SectionTitle("Recommended for you") }
            items(recs, key = { it.id }) { r ->
                InfoCard {
                    Text(r.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    r.stylistName?.let { Text("with $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(r.message, modifier = Modifier.padding(top = 4.dp))
                    TextButton(onClick = { nav.navigate(bookRoute(serviceId = r.serviceId)) }) { Text("Book this") }
                }
            }
        }
    }
}

/** The soonest booking that is still ahead of us and not cancelled/rejected. */
fun nextAppointment(all: List<Appointment>): Appointment? =
    all.filter { it.isOpen && parseDateTime(it.endTime).isAfter(LocalDateTime.now()) }
        .minByOrNull { parseDateTime(it.startTime) }

@Composable
fun NextVisitCard(a: Appointment, onClick: () -> Unit) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusChip(a.status)
        }
        Text("${fmtWhen(a.startTime)} · ${a.stylistName}", modifier = Modifier.padding(top = 4.dp))
        if (a.isHomeService) Text("Home visit", color = MaterialTheme.colorScheme.primary)
        if (a.status == "PENDING") {
            Text(
                "The salon will confirm your booking soon.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun PointsCard(p: Profile, onClick: () -> Unit) = PointsCard(p.loyaltyPoints, p.completedVisits, p.tier, p.visitsToNextTier, onClick)

@Composable
fun PointsCard(b: LoyaltyBalance, onClick: (() -> Unit)? = null) =
    PointsCard(b.balance, b.completedVisits, b.tier, b.visitsToNextTier, onClick)

@Composable
private fun PointsCard(points: Int, visits: Int, tier: String, toNext: Int, onClick: (() -> Unit)?) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(points.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(" points", modifier = Modifier.padding(bottom = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(tierLabel(tier), fontWeight = FontWeight.SemiBold)
        if (toNext > 0) {
            val target = visits + toNext
            LinearProgressIndicator(
                progress = { visits.toFloat() / target },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                "$toNext more ${if (toNext == 1) "visit" else "visits"} to reach the next level",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Text("You've reached our top level. Thank you!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
