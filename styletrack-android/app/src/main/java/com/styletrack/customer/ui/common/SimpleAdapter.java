package com.styletrack.customer.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** A generic single-view-type RecyclerView adapter, used by every list screen to avoid repeating boilerplate. */
public class SimpleAdapter<T> extends RecyclerView.Adapter<SimpleAdapter.Holder> {
    private final int layoutRes;
    private final BiConsumer<View, T> binder;
    private List<T> items = new ArrayList<>();

    public SimpleAdapter(int layoutRes, BiConsumer<View, T> binder) {
        this.layoutRes = layoutRes;
        this.binder = binder;
    }

    public void submit(List<T> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    public T get(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        binder.accept(holder.itemView, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
