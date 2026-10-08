package com.styletrack.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.styletrack.customer.data.LoyaltyTx
import java.time.format.DateTimeFormatter
import java.util.Locale

private val txDateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

@Composable
fun RewardsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val balance = rememberLoad("rewards-balance") { repo.loyalty() }
    val history = rememberLoad("rewards-history") { repo.loyaltyHistory() }
    val recs = rememberLoad("rewards-recs") { repo.recommendations() }

    val balanceState by balance.state.collectAsState()
    val historyState by history.state.collectAsState()
    val recState by recs.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rewards")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                when (val s = balanceState) {
                    UiState.Loading -> LoadingBox(Modifier.fillMaxSize().padding(24.dp))
                    is UiState.Failed -> ErrorBox(s.message, { balance.reload() }, Modifier.fillMaxSize())
                    is UiState.Ready -> PointsCard(s.data)
                }
            }

            val suggestions = (recState as? UiState.Ready)?.data.orEmpty()
            if (suggestions.isNotEmpty()) {
                item { SectionTitle("Recommended for you", Modifier.padding(top = 8.dp)) }
                items(suggestions, key = { "rec-${it.id}" }) { r ->
                    InfoCard {
                        Text(r.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        r.stylistName?.let { Text("with $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(r.message, modifier = Modifier.padding(top = 4.dp))
                        TextButton(onClick = { nav.navigate(bookRoute(serviceId = r.serviceId)) }) { Text("Book this") }
                    }
                }
            }

            item { SectionTitle("Points history", Modifier.padding(top = 8.dp)) }
            when (val s = historyState) {
                UiState.Loading -> item { LoadingBox(Modifier.fillMaxSize().padding(24.dp)) }
                is UiState.Failed -> item { ErrorBox(s.message, { history.reload() }, Modifier.fillMaxSize()) }
                is UiState.Ready -> {
                    if (s.data.isEmpty()) {
                        item { EmptyBox("No points yet", "You earn points after each completed visit.") }
                    } else {
                        items(s.data, key = { "tx-${it.id}" }) { tx -> TxRow(tx) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TxRow(tx: LoyaltyTx) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tx.description?.takeIf { it.isNotBlank() } ?: txLabel(tx.type), fontWeight = FontWeight.SemiBold)
                Text(
                    parseDateTime(tx.createdAt).format(txDateFmt) + " · balance ${tx.balanceAfter}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                (if (tx.points > 0) "+" else "") + tx.points,
                style = MaterialTheme.typography.titleMedium,
                color = if (tx.points >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun txLabel(type: String) = when (type) {
    "EARNED" -> "Points earned"
    "REDEEMED" -> "Points used"
    "ADJUSTED" -> "Adjustment"
    else -> "Points update"
}
