package com.styletrack.customer.ui.common;

import android.view.View;
import android.widget.TextView;

import com.styletrack.customer.R;

/** Binds one row of the reusable view_fact_row.xml include (a label/value pair). */
public final class FactRow {
    private FactRow() {}

    public static void bind(View row, String label, String value) {
        row.setVisibility(View.VISIBLE);
        ((TextView) row.findViewById(R.id.factLabel)).setText(label);
        ((TextView) row.findViewById(R.id.factValue)).setText(value);
    }

    public static void hide(View row) {
        row.setVisibility(View.GONE);
    }
}
