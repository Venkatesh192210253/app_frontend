package com.simats.myfitnessbuddy;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WeightSetupFragment extends Fragment {

    private EditText etWeight, etGoalWeight;
    private TextView tvWeightDifference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_weight, container, false);

        etWeight = view.findViewById(R.id.et_weight);
        etGoalWeight = view.findViewById(R.id.et_goal_weight);
        tvWeightDifference = view.findViewById(R.id.tv_weight_difference);

        TextWatcher weightWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateDifference();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etWeight.addTextChangedListener(weightWatcher);
        etGoalWeight.addTextChangedListener(weightWatcher);

        return view;
    }

    private void calculateDifference() {
        String weightStr = etWeight.getText().toString();
        String goalStr = etGoalWeight.getText().toString();

        if (!weightStr.isEmpty() && !goalStr.isEmpty()) {
            try {
                double weight = Double.parseDouble(weightStr);
                double goal = Double.parseDouble(goalStr);
                double diff = weight - goal;

                if (diff > 0) {
                    tvWeightDifference.setText(String.format("You are aiming to reduce %.1f kg", diff));
                } else if (diff < 0) {
                    tvWeightDifference.setText(String.format("You are aiming to gain %.1f kg", Math.abs(diff)));
                } else {
                    tvWeightDifference.setText("Your goal is to maintain your weight");
                }
            } catch (NumberFormatException e) {
                tvWeightDifference.setText("");
            }
        } else {
            tvWeightDifference.setText("");
        }
    }
}
