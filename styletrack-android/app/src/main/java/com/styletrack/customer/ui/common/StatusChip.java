package com.styletrack.customer.ui.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.styletrack.customer.R;

/** Configures a TextView as a small status pill, mirroring the Compose StatusChip. */
public final class StatusChip {
    private StatusChip() {}

    public static void bind(TextView view, String status) {
        Context c = view.getContext();
        String label;
        int bg;
        int fg;
        switch (status == null ? "" : status) {
            case "PENDING":
                label = "Waiting for confirmation";
                bg = R.color.status_pending_bg;
                fg = R.color.status_pending_fg;
                break;
            case "ACCEPTED":
                label = "Confirmed";
                bg = R.color.status_accepted_bg;
                fg = R.color.status_accepted_fg;
                break;
            case "COMPLETED":
                label = "Completed";
                bg = R.color.status_neutral_bg;
                fg = R.color.status_neutral_fg;
                break;
            case "REJECTED":
                label = "Not accepted";
                bg = R.color.status_error_bg;
                fg = R.color.status_error_fg;
                break;
            case "CANCELLED":
                label = "Cancelled";
                bg = R.color.status_error_bg;
                fg = R.color.status_error_fg;
                break;
            default:
                label = status;
                bg = R.color.status_neutral_bg;
                fg = R.color.status_neutral_fg;
                break;
        }
        view.setText(label);
        view.setTextColor(ContextCompat.getColor(c, fg));
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(999f);
        shape.setColor(ContextCompat.getColor(c, bg));
        view.setBackground(shape);
        int padH = (int) (10 * c.getResources().getDisplayMetrics().density);
        int padV = (int) (3 * c.getResources().getDisplayMetrics().density);
        view.setPadding(padH, padV, padH, padV);
        view.setTextSize(13);
    }
}
