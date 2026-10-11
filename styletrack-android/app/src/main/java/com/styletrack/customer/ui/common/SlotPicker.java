package com.styletrack.customer.ui.common;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

/** Renders free start times as a single-select ChipGroup. Replaces the Compose SlotPicker. */
public final class SlotPicker {
    private SlotPicker() {}

    public static void render(
            ProgressBar progress,
            TextView message,
            ChipGroup chipGroup,
            Long stylistId,
            Long serviceId,
            UiState<List<String>> state,
            String selected,
            Consumer<String> onSelect) {
        progress.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        chipGroup.setVisibility(View.GONE);
        chipGroup.removeAllViews();

        if (stylistId == null || serviceId == null) {
            message.setText("Choose a service and a stylist to see free times.");
            message.setVisibility(View.VISIBLE);
            return;
        }
        if (state instanceof UiState.Loading) {
            progress.setVisibility(View.VISIBLE);
            return;
        }
        if (state instanceof UiState.Failed) {
            message.setText(((UiState.Failed<List<String>>) state).message);
            message.setVisibility(View.VISIBLE);
            return;
        }
        List<String> times = ((UiState.Ready<List<String>>) state).data;
        if (times.isEmpty()) {
            message.setText("No free times on this day. Try another date or another stylist.");
            message.setVisibility(View.VISIBLE);
            return;
        }
        chipGroup.setVisibility(View.VISIBLE);
        for (String t : times) {
            Chip chip = new Chip(chipGroup.getContext());
            chip.setText(Formats.fmtClock(LocalTime.parse(t)));
            chip.setCheckable(true);
            chip.setChecked(t.equals(selected));
            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) onSelect.accept(t);
            });
            chipGroup.addView(chip);
        }
    }
}
