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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.styletrack.customer.data.SalonService
import com.styletrack.customer.data.Stylist
import androidx.compose.material3.Tab as TabItem

@Composable
fun BrowseScreen(nav: NavController) {
    val repo = LocalRepo.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    val services = rememberLoad("browse-services") { repo.services() }
    val stylists = rememberLoad("browse-stylists") { repo.stylists() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Browse")
        TabRow(selectedTabIndex = tab) {
            TabItem(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Services") })
            TabItem(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Stylists") })
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(if (tab == 0) "Search services" else "Search stylists") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (tab == 0) {
            Loaded(services) { all ->
                val q = query.trim()
                val shown = all.filter {
                    q.isEmpty() || it.name.contains(q, ignoreCase = true) ||
                        it.category.orEmpty().contains(q, ignoreCase = true)
                }
                if (shown.isEmpty()) {
                    EmptyBox("Nothing found", if (all.isEmpty()) "The salon hasn't listed any services yet." else "Try a different word.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.id }) { s -> ServiceRow(s) { nav.navigate("service/${s.id}") } }
                    }
                }
            }
        } else {
            Loaded(stylists) { all ->
                val q = query.trim()
                val shown = all.filter {
                    q.isEmpty() || it.fullName.contains(q, ignoreCase = true) ||
                        it.specialization.orEmpty().contains(q, ignoreCase = true)
                }
                if (shown.isEmpty()) {
                    EmptyBox("Nothing found", if (all.isEmpty()) "No stylists are listed yet." else "Try a different word.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.id }) { s -> StylistRow(s) { nav.navigate("stylist/${s.id}") } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(s: SalonService, onClick: () -> Unit) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                s.category?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    fmtDuration(s.durationMinutes) + if (s.homeServiceAvailable) " · Home visit available" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(peso(s.price), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StylistRow(s: Stylist, onClick: () -> Unit) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(s.fullName)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(s.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                s.specialization?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun ServiceDetailScreen(nav: NavController, id: Long) {
    val repo = LocalRepo.current
    val vm = rememberLoad("service-detail") { repo.services() }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Service", onBack = { nav.popBackStack() })
        Loaded(vm) { all ->
            val s = all.firstOrNull { it.id == id }
            if (s == null) {
                EmptyBox("This service is no longer available")
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(s.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    s.category?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    InfoCard {
                        FactRow("Price", peso(s.price))
                        FactRow("Time needed", fmtDuration(s.durationMinutes))
                        FactRow("Home visit", if (s.homeServiceAvailable) "Available" else "Salon only")
                        s.pointsAwarded?.takeIf { it > 0 }?.let { FactRow("Points earned", "$it points") }
                    }
                    s.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    Button(onClick = { nav.navigate(bookRoute(serviceId = s.id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Book at the salon")
                    }
                    if (s.homeServiceAvailable) {
                        OutlinedButton(onClick = { nav.navigate(bookRoute(serviceId = s.id, home = true)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Book a home visit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StylistDetailScreen(nav: NavController, id: Long) {
    val repo = LocalRepo.current
    val vm = rememberLoad("stylist-detail") { repo.stylists() }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Stylist", onBack = { nav.popBackStack() })
        Loaded(vm) { all ->
            val s = all.firstOrNull { it.id == id }
            if (s == null) {
                EmptyBox("This stylist is no longer listed")
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(s.fullName, size = 64)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(s.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            s.specialization?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    s.bio?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    Button(onClick = { nav.navigate(bookRoute(stylistId = s.id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Book with ${s.fullName.substringBefore(' ')}")
                    }
                    OutlinedButton(onClick = { nav.navigate(bookRoute(stylistId = s.id, home = true)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Book a home visit")
                    }
                }
            }
        }
    }
}
