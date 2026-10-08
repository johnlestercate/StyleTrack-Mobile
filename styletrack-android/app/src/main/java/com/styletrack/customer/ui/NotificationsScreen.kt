package com.styletrack.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.styletrack.customer.data.AppNotification
import kotlinx.coroutines.CancellationException

@Composable
fun NotificationsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val vm = rememberLoad("notifications") { repo.notifications() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Notifications", onBack = { nav.popBackStack() })
        Loaded(vm) { list ->
            val anyUnseen = list.any { !it.seen }
            // Opening the list counts as reading it. The rows already on screen keep their "new" dot until the next visit.
            LaunchedEffect(anyUnseen) {
                if (anyUnseen) {
                    try {
                        repo.markAllSeen()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // not critical; they stay unread and show again next time
                    }
                }
            }
            if (list.isEmpty()) {
                EmptyBox("Nothing new", "Updates about your bookings will appear here.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list, key = { it.id }) { n ->
                        NotificationRow(n, onClick = n.appointmentId?.let { id -> { nav.navigate("booking/$id") } })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: (() -> Unit)?) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (n.seen) FontWeight.Normal else FontWeight.Bold)
                Text(n.message, modifier = Modifier.padding(top = 2.dp))
                Text(
                    fmtWhen(n.createdAt),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!n.seen) {
                Box(Modifier.padding(start = 8.dp, top = 6.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
