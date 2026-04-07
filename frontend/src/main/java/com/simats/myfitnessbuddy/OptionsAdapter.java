package com.simats.myfitnessbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(boolean hasSelection);
    }

    private final List<String> options;
    private final boolean isMultiSelect;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnSelectionChangedListener selectionChangedListener;

    public OptionsAdapter(List<String> options, boolean isMultiSelect) {
        this.options = options;
        this.isMultiSelect = isMultiSelect;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvOption.setText(options.get(position));
        boolean isSelected = selectedPositions.contains(position);
        
        holder.itemView.setSelected(isSelected);
        
        int indicatorColor = isSelected ? 
                ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green) : 
                ContextCompat.getColor(holder.itemView.getContext(), R.color.gray_text);
        holder.ivIndicator.setColorFilter(indicatorColor);

        holder.itemView.setOnClickListener(v -> {
            // Pop animation
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }).start();

            if (isMultiSelect) {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                notifyItemChanged(position);
            } else {
                int previousSelected = -1;
                if (!selectedPositions.isEmpty()) {
                    previousSelected = selectedPositions.iterator().next();
                }
                selectedPositions.clear();
                selectedPositions.add(position);
                if (previousSelected != -1) notifyItemChanged(previousSelected);
                notifyItemChanged(position);
            }
            
            if (selectionChangedListener != null) {
                // Ensure listener is notified with current state
                selectionChangedListener.onSelectionChanged(!selectedPositions.isEmpty());
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOption;
        ImageView ivIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOption = itemView.findViewById(R.id.tv_option_text);
            ivIndicator = itemView.findViewById(R.id.iv_selection_indicator);
        }
    }
}
