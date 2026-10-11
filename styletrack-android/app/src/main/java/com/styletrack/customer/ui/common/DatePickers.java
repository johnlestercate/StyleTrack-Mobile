package com.styletrack.customer.ui.common;

import android.app.DatePickerDialog;
import android.content.Context;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Consumer;

/** Opens a plain DatePickerDialog restricted to today or later. Replaces the Compose DatePickerButton. */
public final class DatePickers {
    private DatePickers() {}

    public static void show(Context context, LocalDate current, Consumer<LocalDate> onChange) {
        DatePickerDialog dialog = new DatePickerDialog(
                context,
                (view, year, month, day) -> onChange.accept(LocalDate.of(year, month + 1, day)),
                current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        long todayMillis = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
        dialog.getDatePicker().setMinDate(todayMillis);
        dialog.show();
    }
}
