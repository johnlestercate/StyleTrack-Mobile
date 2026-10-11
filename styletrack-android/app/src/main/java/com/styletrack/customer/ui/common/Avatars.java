package com.styletrack.customer.ui.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.styletrack.customer.R;

/** A circular initials avatar, built on a plain TextView with an oval background. */
public final class Avatars {
    private Avatars() {}

    public static void bind(TextView view, String name) {
        Context c = view.getContext();
        view.setText(initials(name));
        view.setTextColor(ContextCompat.getColor(c, R.color.on_primary_container));
        view.setGravity(android.view.Gravity.CENTER);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(ContextCompat.getColor(c, R.color.primary_container));
        view.setBackground(shape);
    }

    public static String initials(String name) {
        if (name == null) return "";
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (String word : name.trim().split("\\s+")) {
            if (word.isBlank()) continue;
            out.append(Character.toUpperCase(word.charAt(0)));
            count++;
            if (count >= 2) break;
        }
        return out.toString();
    }
}
