package com.styletrack.customer.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.styletrack.customer.data.Repository
import com.styletrack.customer.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocalRepo = staticCompositionLocalOf<Repository> { error("Repository not provided") }

// --------------------------------------------------------------------- loading

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Failed(val message: String) : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
}

/** Loads something from the server once, keeps it across rotation, and can reload it. */
class LoadVM<T>(private val loader: suspend () -> T) : ViewModel() {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state

    private var loading = false
    private var lastLoaded = 0L

    init {
        reload()
    }

    /** [quiet] keeps showing the current data while refreshing instead of flashing a spinner. */
    fun reload(quiet: Boolean = false) {
        if (loading) return
        loading = true
        if (!quiet || _state.value !is UiState.Ready) _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                _state.value = UiState.Ready(loader())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value !is UiState.Ready) _state.value = UiState.Failed(e.userMessage())
            } finally {
                loading = false
                lastLoaded = SystemClock.elapsedRealtime()
            }
        }
    }

    fun refreshIfStale() {
        if (SystemClock.elapsedRealtime() - lastLoaded > 3_000) reload(quiet = true)
    }
}

private class LoadFactory<T>(private val loader: suspend () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = LoadVM(loader) as V
}

/**
 * [key] must be unique for this loader on the screen (include any ids it depends on), because the
 * ViewModel is stored under it.
 */
@Composable
fun <T> rememberLoad(key: String, loader: suspend () -> T): LoadVM<T> {
    @Suppress("UNCHECKED_CAST")
    val vm = viewModel(
        modelClass = LoadVM::class.java,
        key = key,
        factory = LoadFactory(loader),
    ) as LoadVM<T>
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfStale() }
    return vm
}

/** Shows a spinner, an error with a retry button, or [content] once the data is there. */
@Composable
fun <T> Loaded(vm: LoadVM<T>, modifier: Modifier = Modifier.fillMaxSize(), content: @Composable (T) -> Unit) {
    val state by vm.state.collectAsState()
    when (val s = state) {
        UiState.Loading -> LoadingBox(modifier)
        is UiState.Failed -> ErrorBox(s.message, onRetry = { vm.reload() }, modifier = modifier)
        is UiState.Ready -> content(s.data)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(
        modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Try again") }
        }
    }
}

@Composable
fun EmptyBox(title: String, hint: String? = null, modifier: Modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp)) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(
                hint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

// ------------------------------------------------------------------ status chip

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, container, content) = when (status) {
        "PENDING" -> Triple("Waiting for confirmation", Color(0xFFFFF0D1), Color(0xFF7A4800))
        "ACCEPTED" -> Triple("Confirmed", Color(0xFFD9F0ED), Color(0xFF0B5D58))
        "COMPLETED" -> Triple("Completed", Color(0xFFE6E1F0), Color(0xFF4D3F6B))
        "REJECTED" -> Triple("Not accepted", Color(0xFFF9E3E6), Color(0xFF9C2F3F))
        "CANCELLED" -> Triple("Cancelled", Color(0xFFF9E3E6), Color(0xFF9C2F3F))
        else -> Triple(status, Color(0xFFE6E1F0), Color(0xFF4D3F6B))
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

// ------------------------------------------------------------------- formatting

private val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val longDayFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val wireFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun parseDateTime(s: String): LocalDateTime = LocalDateTime.parse(s)
fun fmtDay(s: String): String = parseDateTime(s).format(dayFmt)
fun fmtLongDay(d: LocalDate): String = d.format(longDayFmt)
fun fmtTime(s: String): String = parseDateTime(s).format(timeFmt)
fun fmtClock(t: LocalTime): String = t.format(timeFmt)
fun fmtWhen(s: String): String = parseDateTime(s).let { "${it.format(dayFmt)}, ${it.format(timeFmt)}" }
fun wireDateTime(date: LocalDate, time: LocalTime): String = LocalDateTime.of(date, time).format(wireFmt)

fun peso(v: Double): String = if (v % 1.0 == 0.0) String.format(Locale.US, "₱%,.0f", v) else String.format(Locale.US, "₱%,.2f", v)

fun fmtDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} hr"
    else -> "${minutes / 60} hr ${minutes % 60} min"
}

fun tierLabel(tier: String): String = when (tier) {
    "GOLD" -> "Gold member"
    "SILVER" -> "Silver member"
    else -> "Member"
}
