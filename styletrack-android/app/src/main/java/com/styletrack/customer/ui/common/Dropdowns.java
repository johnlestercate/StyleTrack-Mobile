package com.styletrack.customer.ui.common;

import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Binds an AutoCompleteTextView (ExposedDropdownMenu style) to an id/label option list. */
public final class Dropdowns {
    private Dropdowns() {}

    public static void bind(AutoCompleteTextView field, List<Pair<Long, String>> options, Long selected, Consumer<Long> onSelect) {
        List<String> labels = new ArrayList<>();
        String selectedLabel = "";
        for (Pair<Long, String> o : options) {
            labels.add(o.second);
            if (selected != null && o.first.equals(selected)) selectedLabel = o.second;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(field.getContext(), android.R.layout.simple_list_item_1, labels);
        field.setAdapter(adapter);
        field.setText(selectedLabel, false);
        field.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < options.size()) onSelect.accept(options.get(position).first);
        });
    }
}
