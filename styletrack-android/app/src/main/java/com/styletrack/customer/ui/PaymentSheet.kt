package com.styletrack.customer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.styletrack.customer.data.PayStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val POLL_MILLIS = 4_000L

/**
 * Shown after a GCash or QR Ph payment has been started. It never decides that the customer has paid:
 * it keeps asking the salon's server (which asks PayMongo) and closes when the server says PAID.
 *
 * [onFinished] gets `true` when the payment went through.
 */
@Composable
fun PaymentDialog(appointmentId: Long, payment: PayStarted, onFinished: (paid: Boolean) -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val isGcash = payment.method == "GCASH"

    var outcome by remember { mutableStateOf("PENDING") }
    var resumes by remember { mutableIntStateOf(0) }
    var opened by rememberSaveable(payment.paymentId) { mutableStateOf(false) }

    // GCash: open the GCash page once, straight away
    LaunchedEffect(payment.paymentId) {
        if (isGcash && !opened && payment.redirectUrl != null) {
            opened = true
            openLink(context, payment.redirectUrl)
        }
    }

    // coming back from GCash (or the bank app) checks immediately instead of waiting for the next poll
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }

    LaunchedEffect(payment.paymentId, resumes) {
        while (outcome == "PENDING") {
            try {
                outcome = repo.paymentStatus(appointmentId).status.let { if (it == "NONE") "PENDING" else it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // a dropped connection should not end the payment: keep trying
            }
            if (outcome == "PAID") {
                onFinished(true)
                return@LaunchedEffect
            }
            if (outcome == "PENDING") delay(POLL_MILLIS)
        }
    }

    AlertDialog(
        onDismissRequest = { onFinished(false) },
        title = { Text(if (isGcash) "Pay with GCash" else "Pay with QR Ph") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Amount: ${peso(payment.amount)}", style = MaterialTheme.typography.titleMedium)
                when {
                    outcome == "FAILED" -> Text(
                        "The payment didn't go through. You have not been charged. You can close this and try again.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    outcome == "EXPIRED" -> Text(
                        "This payment timed out. Close this and start again if you still want to pay online.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    outcome == "NEEDS_REFUND" -> Text(
                        "We received your payment but couldn't apply it to this booking. Please contact the salon and we will sort it out.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    isGcash -> {
                        Text("Finish the payment in GCash, then come back here. This page updates by itself.")
                        OutlinedButton(
                            onClick = { payment.redirectUrl?.let { openLink(context, it) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Open GCash again") }
                        Waiting()
                    }
                    else -> {
                        QrCode(payment, context)
                        Text(
                            "Scan this with GCash, Maya or your bank's app (QR Ph)." +
                                (payment.expiresAt?.let { " Valid until ${fmtTime(it)}." } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Waiting()
                    }
                }
            }
        },
        confirmButton = {
            if (outcome == "PENDING") {
                TextButton(onClick = { onFinished(false) }) { Text("Close") }
            } else {
                Button(onClick = { onFinished(false) }) { Text("OK") }
            }
        },
    )
}

@Composable
private fun Waiting() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text("Waiting for your payment…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QrCode(payment: PayStarted, context: Context) {
    val image = payment.qrImage
    val bitmap = remember(image) { image?.let { decodeQr(it) } }
    if (bitmap != null) {
        // white card behind the code so it stays scannable in dark mode
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Ph code for ${peso(payment.amount)}",
            modifier = Modifier.size(240.dp).background(Color.White).padding(8.dp),
        )
    } else if (image != null && image.startsWith("http")) {
        Button(onClick = { openLink(context, image) }, modifier = Modifier.fillMaxWidth()) { Text("Open QR code") }
    } else {
        Text("The QR code couldn't be shown. Close this and try again.", color = MaterialTheme.colorScheme.error)
    }
}

/** The server sends the QR as a data URI or plain Base64. */
private fun decodeQr(image: String): Bitmap? = runCatching {
    val bytes = Base64.decode(image.substringAfter("base64,", image), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

private fun openLink(context: Context, url: String) {
    if (!url.startsWith("https://")) return // only ever open secure web links that came from our own server
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // no browser installed: nothing more we can do here
    }
}
