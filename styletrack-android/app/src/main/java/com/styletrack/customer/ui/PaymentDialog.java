package com.styletrack.customer.ui;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.styletrack.customer.R;
import com.styletrack.customer.StyleTrackApp;
import com.styletrack.customer.data.PayStarted;
import com.styletrack.customer.ui.common.Formats;

/**
 * Shown after a GCash or QR Ph payment has been started. It never decides that the customer has
 * paid: it keeps asking the salon's server (which asks PayMongo) and closes when the server says PAID.
 * Replaces the Compose PaymentDialog in PaymentSheet.kt.
 */
public class PaymentDialog extends DialogFragment {
    private static final long POLL_MILLIS = 4_000L;

    public interface Listener {
        void onPaymentFinished(boolean paid);
    }

    private long appointmentId;
    private String method;
    private long paymentId;
    private double amount;
    private String redirectUrl;
    private String qrImage;
    private String expiresAt;

    private String outcome = "PENDING";
    private boolean opened;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    private View rootView;

    public static PaymentDialog newInstance(long appointmentId, PayStarted payment) {
        PaymentDialog dialog = new PaymentDialog();
        Bundle args = new Bundle();
        args.putLong("appointmentId", appointmentId);
        args.putString("method", payment.method);
        args.putLong("paymentId", payment.paymentId);
        args.putDouble("amount", payment.amount);
        args.putString("redirectUrl", payment.redirectUrl);
        args.putString("qrImage", payment.qrImage);
        args.putString("expiresAt", payment.expiresAt);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        Bundle args = requireArguments();
        appointmentId = args.getLong("appointmentId");
        method = args.getString("method");
        paymentId = args.getLong("paymentId");
        amount = args.getDouble("amount");
        redirectUrl = args.getString("redirectUrl");
        qrImage = args.getString("qrImage");
        expiresAt = args.getString("expiresAt");
        if (savedInstanceState != null) {
            outcome = savedInstanceState.getString("outcome", "PENDING");
            opened = savedInstanceState.getBoolean("opened", false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("outcome", outcome);
        outState.putBoolean("opened", opened);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        boolean isGcash = "GCASH".equals(method);
        rootView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(isGcash ? "Pay with GCash" : "Pay with QR Ph")
                .setView(rootView)
                .setOnCancelListener(d -> finish(false));

        builder.setPositiveButton(outcome.equals("PENDING") ? "Close" : "OK", (d, w) -> finish(false));

        AlertDialog dialog = builder.create();

        if (isGcash && !opened && redirectUrl != null) {
            opened = true;
            openLink(redirectUrl);
        }

        render(isGcash);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkStatusNow();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
    }

    private void render(boolean isGcash) {
        ((TextView) rootView.findViewById(R.id.amount)).setText("Amount: " + Formats.peso(amount));
        TextView message = rootView.findViewById(R.id.message);
        MaterialButton openGcash = rootView.findViewById(R.id.openGcash);
        View waitingRow = rootView.findViewById(R.id.waitingRow);
        androidx.cardview.widget.CardView qrCard = rootView.findViewById(R.id.qrCard);
        ImageView qrImageView = rootView.findViewById(R.id.qrImage);
        MaterialButton openQr = rootView.findViewById(R.id.openQr);
        TextView caption = rootView.findViewById(R.id.caption);

        message.setVisibility(View.GONE);
        openGcash.setVisibility(View.GONE);
        waitingRow.setVisibility(View.GONE);
        qrCard.setVisibility(View.GONE);
        openQr.setVisibility(View.GONE);
        caption.setVisibility(View.GONE);

        int errorColor = ContextCompat.getColor(requireContext(), R.color.error);

        switch (outcome) {
            case "FAILED":
                message.setText("The payment didn't go through. You have not been charged. You can close this and try again.");
                message.setTextColor(errorColor);
                message.setVisibility(View.VISIBLE);
                break;
            case "EXPIRED":
                message.setText("This payment timed out. Close this and start again if you still want to pay online.");
                message.setTextColor(errorColor);
                message.setVisibility(View.VISIBLE);
                break;
            case "NEEDS_REFUND":
                message.setText("We received your payment but couldn't apply it to this booking. Please contact the salon and we will sort it out.");
                message.setTextColor(errorColor);
                message.setVisibility(View.VISIBLE);
                break;
            default:
                if (isGcash) {
                    message.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink));
                    message.setText("Finish the payment in GCash, then come back here. This page updates by itself.");
                    message.setVisibility(View.VISIBLE);
                    openGcash.setVisibility(View.VISIBLE);
                    openGcash.setOnClickListener(v -> openLink(redirectUrl));
                    waitingRow.setVisibility(View.VISIBLE);
                } else {
                    Bitmap bitmap = decodeQr(qrImage);
                    if (bitmap != null) {
                        qrCard.setVisibility(View.VISIBLE);
                        qrImageView.setImageBitmap(bitmap);
                    } else if (qrImage != null && qrImage.startsWith("http")) {
                        openQr.setVisibility(View.VISIBLE);
                        openQr.setOnClickListener(v -> openLink(qrImage));
                    } else {
                        message.setTextColor(errorColor);
                        message.setText("The QR code couldn't be shown. Close this and try again.");
                        message.setVisibility(View.VISIBLE);
                    }
                    StringBuilder cap = new StringBuilder("Scan this with GCash, Maya or your bank's app (QR Ph).");
                    if (expiresAt != null) cap.append(" Valid until ").append(Formats.fmtTime(expiresAt)).append(".");
                    caption.setText(cap.toString());
                    caption.setVisibility(View.VISIBLE);
                    waitingRow.setVisibility(View.VISIBLE);
                }
                break;
        }

        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            MaterialButton button = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) button.setText(outcome.equals("PENDING") ? "Close" : "OK");
        }
    }

    private void checkStatusNow() {
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
        pollRunnable = this::pollOnce;
        handler.post(pollRunnable);
    }

    private void pollOnce() {
        if (!"PENDING".equals(outcome) || !isAdded()) return;
        StyleTrackApp app = (StyleTrackApp) requireActivity().getApplication();
        app.getRepoCore().paymentStatus(appointmentId).whenCompleteAsync((status, err) -> {
            if (!isAdded()) return;
            if (err == null) {
                String s = status.status;
                outcome = "NONE".equals(s) ? "PENDING" : s;
            } // a dropped connection should not end the payment: keep trying, outcome stays PENDING
            if ("PAID".equals(outcome)) {
                finish(true);
                return;
            }
            if (rootView != null) render("GCASH".equals(method));
            if ("PENDING".equals(outcome)) {
                pollRunnable = this::pollOnce;
                handler.postDelayed(pollRunnable, POLL_MILLIS);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void finish(boolean paid) {
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
        dismissAllowingStateLoss();
        androidx.fragment.app.Fragment parent = getParentFragment();
        if (parent instanceof Listener) {
            ((Listener) parent).onPaymentFinished(paid);
        }
    }

    private void openLink(String url) {
        if (url == null || !url.startsWith("https://")) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException ignored) {
            // no browser installed: nothing more we can do here
        }
    }

    private static Bitmap decodeQr(String image) {
        if (image == null) return null;
        try {
            String base64 = image.contains("base64,") ? image.substring(image.indexOf("base64,") + 7) : image;
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
