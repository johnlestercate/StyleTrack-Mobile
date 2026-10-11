package com.styletrack.customer.ui.common;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.styletrack.customer.R;

/** Binds the reusable view_screen_header.xml include (title + optional back button). */
public final class ScreenHeader {
    private ScreenHeader() {}

    public static void bind(View root, String title, Runnable onBack) {
        ((TextView) root.findViewById(R.id.headerTitle)).setText(title);
        ImageButton back = root.findViewById(R.id.headerBack);
        if (onBack != null) {
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> onBack.run());
        } else {
            back.setVisibility(View.GONE);
        }
    }
}
