package com.simats.myfitnessbuddy;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetupStepFragment extends Fragment {

    private static final String ARG_POSITION = "position";
    private int position;
    private String selectedGender = "";

    public static SetupStepFragment newInstance(int position) {
        SetupStepFragment fragment = new SetupStepFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_step, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_setup_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_setup_subtitle);
        RecyclerView rvOptions = view.findViewById(R.id.rv_options);

        if (rvOptions != null) {
            rvOptions.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        setupData(view, tvTitle, tvSubtitle, rvOptions);

        return view;
    }

    private void setupData(View view, TextView tvTitle, TextView tvSubtitle, RecyclerView rvOptions) {
        if (view == null) return;

        List<String> options = new ArrayList<>();
        boolean isMultiSelect = false;

        // Safely reset visibility
        View rv = view.findViewById(R.id.rv_options);
        if (rv != null) rv.setVisibility(View.VISIBLE);

        View genderContainer = view.findViewById(R.id.ll_gender_container);
        if (genderContainer != null) genderContainer.setVisibility(View.GONE);

        View inputContainer = view.findViewById(R.id.ll_input_container);
        if (inputContainer != null) inputContainer.setVisibility(View.GONE);

        View row1 = view.findViewById(R.id.ll_row_1);
        if (row1 != null) row1.setVisibility(View.VISIBLE);

        View til1_1 = view.findViewById(R.id.til_input_1_1);
        if (til1_1 != null) til1_1.setVisibility(View.VISIBLE);

        View til1_2 = view.findViewById(R.id.til_input_1_2);
        if (til1_2 != null) til1_2.setVisibility(View.GONE);

        View row2 = view.findViewById(R.id.ll_row_2);
        if (row2 != null) row2.setVisibility(View.GONE);

        View row3 = view.findViewById(R.id.ll_row_3);
        if (row3 != null) row3.setVisibility(View.GONE);

        View u1 = view.findViewById(R.id.tv_unit_1);
        if (u1 != null) u1.setVisibility(View.GONE);

        View u2 = view.findViewById(R.id.tv_unit_2);
        if (u2 != null) u2.setVisibility(View.GONE);

        View u3 = view.findViewById(R.id.tv_unit_3);
        if (u3 != null) u3.setVisibility(View.GONE);

        View l1 = view.findViewById(R.id.tv_label_1);
        if (l1 != null) l1.setVisibility(View.GONE);

        View l2 = view.findViewById(R.id.tv_label_2);
        if (l2 != null) l2.setVisibility(View.GONE);

        View l3 = view.findViewById(R.id.tv_label_3);
        if (l3 != null) l3.setVisibility(View.GONE);

        View weightDiff = view.findViewById(R.id.tv_weight_difference);
        if (weightDiff != null) weightDiff.setVisibility(View.GONE);

        switch (position) {
            case 0:
                if (tvTitle != null) tvTitle.setText("Hey! Let's start with your goals.");
                if (tvSubtitle != null) tvSubtitle.setText("Select up to three that are most important to you.");
                options = Arrays.asList("Lose weight", "Maintain weight", "Gain weight", "Gain muscle", "Modify my diet", "Plan meals", "Manage stress");
                isMultiSelect = true;
                break;
            case 1:
                if (tvTitle != null) tvTitle.setText("What have been your barriers to losing weight?");
                if (tvSubtitle != null) tvSubtitle.setText("Select all that apply.");
                options = Arrays.asList("Lack of time", "The regimen was hard to follow", "Healthy diets lack variety", "Stress around food choices", "Holidays/Social Events", "Food cravings", "Lack of progress");
                isMultiSelect = true;
                break;
            case 2:
                if (tvTitle != null) tvTitle.setText("Which health habits are most important to you?");
                if (tvSubtitle != null) tvSubtitle.setText("Recommended for you");
                options = Arrays.asList("Eat more protein", "Plan more meals", "Meal prep and cook", "Eat more fiber", "Move more", "Workout more");
                isMultiSelect = true;
                break;
            case 3:
                if (tvTitle != null) tvTitle.setText("How often do you plan your meals in advance?");
                if (tvSubtitle != null) tvSubtitle.setText("");
                options = Arrays.asList("Never", "Rarely", "Occasionally", "Frequently", "Always");
                isMultiSelect = false;
                break;
            case 4:
                if (tvTitle != null) tvTitle.setText("Do you want us to help you build weekly meal plans?");
                if (tvSubtitle != null) tvSubtitle.setText("Your custom plan will be tailored to your lifestyle and goals.");
                options = Arrays.asList("Yes, definitely", "Open to trying", "No thanks");
                isMultiSelect = false;
                break;
            case 5:
                if (tvTitle != null) tvTitle.setText("What is your baseline activity level?");
                if (tvSubtitle != null) tvSubtitle.setText("Not including workouts - we count that separately.");
                options = Arrays.asList("Not Very Active", "Lightly Active", "Active", "Very Active");
                isMultiSelect = false;
                break;
            case 6:
                if (tvTitle != null) tvTitle.setText("Tell us a little bit about yourself");
                if (tvSubtitle != null) tvSubtitle.setText("Please select which sex we should use to calculate your calorie needs:");
                if (rvOptions != null) rvOptions.setVisibility(View.GONE);
                
                LinearLayout llGender = view.findViewById(R.id.ll_gender_container);
                if (llGender != null) llGender.setVisibility(View.VISIBLE);
                
                LinearLayout btnMale = view.findViewById(R.id.btn_male);
                LinearLayout btnFemale = view.findViewById(R.id.btn_female);
                ImageView ivMaleCheck = view.findViewById(R.id.iv_male_check);
                ImageView ivFemaleCheck = view.findViewById(R.id.iv_female_check);

                View.OnClickListener genderClick = v -> {
                    if (v.getId() == R.id.btn_male) {
                        selectedGender = "Male";
                        if (btnMale != null) btnMale.setSelected(true);
                        if (btnFemale != null) btnFemale.setSelected(false);
                        if (ivMaleCheck != null) ivMaleCheck.setVisibility(View.VISIBLE);
                        if (ivFemaleCheck != null) ivFemaleCheck.setVisibility(View.GONE);
                    } else {
                        selectedGender = "Female";
                        if (btnMale != null) btnMale.setSelected(false);
                        if (btnFemale != null) btnFemale.setSelected(true);
                        if (ivMaleCheck != null) ivMaleCheck.setVisibility(View.GONE);
                        if (ivFemaleCheck != null) ivFemaleCheck.setVisibility(View.VISIBLE);
                    }
                    checkCompletionCase6(view);
                };
                if (btnMale != null) btnMale.setOnClickListener(genderClick);
                if (btnFemale != null) btnFemale.setOnClickListener(genderClick);

                if (inputContainer != null) inputContainer.setVisibility(View.VISIBLE);
                TextView tvLabel1 = view.findViewById(R.id.tv_label_1);
                if (tvLabel1 != null) {
                    tvLabel1.setVisibility(View.VISIBLE);
                    tvLabel1.setText("How old are you?");
                }
                
                TextInputEditText etAge = view.findViewById(R.id.et_input_1_1);
                if (etAge != null) {
                    etAge.setInputType(InputType.TYPE_CLASS_NUMBER);
                    etAge.addTextChangedListener(new TextWatcher() {
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            checkCompletionCase6(view);
                        }
                        public void afterTextChanged(Editable s) {}
                    });
                }

                TextView tvLabel2 = view.findViewById(R.id.tv_label_2);
                if (tvLabel2 != null) {
                    tvLabel2.setVisibility(View.VISIBLE);
                    tvLabel2.setText("Where do you live?");
                }
                
                if (row2 != null) row2.setVisibility(View.VISIBLE);
                TextInputLayout tilCountry = view.findViewById(R.id.til_input_2_1);
                if (tilCountry != null) tilCountry.setVisibility(View.VISIBLE);
                TextInputEditText etCountry = view.findViewById(R.id.et_input_2_1);
                if (etCountry != null) etCountry.setHint("Select country");
                
                TextView tvWeightInfo = view.findViewById(R.id.tv_weight_difference);
                if (tvWeightInfo != null) {
                    tvWeightInfo.setVisibility(View.VISIBLE);
                    tvWeightInfo.setText("We use sex at birth and age to calculate an accurate goal for you.");
                    tvWeightInfo.setTextColor(getResources().getColor(R.color.gray_text));
                }
                break;
            case 7:
                if (tvTitle != null) tvTitle.setText("Just a few more questions");
                if (tvSubtitle != null) tvSubtitle.setText("");
                if (rvOptions != null) rvOptions.setVisibility(View.GONE);
                if (inputContainer != null) inputContainer.setVisibility(View.VISIBLE);
                
                TextView l1_7 = view.findViewById(R.id.tv_label_1);
                if (l1_7 != null) {
                    l1_7.setVisibility(View.VISIBLE);
                    l1_7.setText("How tall are you?");
                }
                
                TextInputEditText et7_1 = view.findViewById(R.id.et_input_1_1);
                if (et7_1 != null) {
                    et7_1.setInputType(InputType.TYPE_CLASS_NUMBER);
                    et7_1.setHint("ft/in");
                }
                
                TextView u1_7 = view.findViewById(R.id.tv_unit_1);
                if (u1_7 != null) {
                    u1_7.setVisibility(View.VISIBLE);
                    u1_7.setText("ft/in");
                }

                TextView l2_7 = view.findViewById(R.id.tv_label_2);
                if (l2_7 != null) {
                    l2_7.setVisibility(View.VISIBLE);
                    l2_7.setText("How much do you weigh?");
                }
                
                if (row2 != null) row2.setVisibility(View.VISIBLE);
                TextInputLayout til7_2 = view.findViewById(R.id.til_input_2_1);
                if (til7_2 != null) til7_2.setVisibility(View.VISIBLE);
                TextInputEditText et7_2 = view.findViewById(R.id.et_input_2_1);
                if (et7_2 != null) et7_2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                
                TextView u2_7 = view.findViewById(R.id.tv_unit_2);
                if (u2_7 != null) {
                    u2_7.setVisibility(View.VISIBLE);
                    u2_7.setText("kg");
                }

                TextView l3_7 = view.findViewById(R.id.tv_label_3);
                if (l3_7 != null) {
                    l3_7.setVisibility(View.VISIBLE);
                    l3_7.setText("What's your goal weight?");
                }
                
                if (row3 != null) row3.setVisibility(View.VISIBLE);
                TextInputLayout til7_3 = view.findViewById(R.id.til_input_3_1);
                if (til7_3 != null) til7_3.setVisibility(View.VISIBLE);
                TextInputEditText et7_3 = view.findViewById(R.id.et_input_3_1);
                if (et7_3 != null) et7_3.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                
                TextView u3_7 = view.findViewById(R.id.tv_unit_3);
                if (u3_7 != null) {
                    u3_7.setVisibility(View.VISIBLE);
                    u3_7.setText("kg");
                }

                TextWatcher tw7 = new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        boolean allFilled = (et7_1 != null && et7_1.length() > 0) && 
                                            (et7_2 != null && et7_2.length() > 0) && 
                                            (et7_3 != null && et7_3.length() > 0);
                        if (getActivity() instanceof SetupActivity) {
                            ((SetupActivity) getActivity()).onSelectionCompleted(allFilled);
                        }
                    }
                    public void afterTextChanged(Editable s) {}
                };
                if (et7_1 != null) et7_1.addTextChangedListener(tw7);
                if (et7_2 != null) et7_2.addTextChangedListener(tw7);
                if (et7_3 != null) et7_3.addTextChangedListener(tw7);
                break;
            case 8:
                if (tvTitle != null) tvTitle.setText("What is your weekly goal?");
                if (tvSubtitle != null) tvSubtitle.setText("Pick one");
                options = Arrays.asList("Lose 0.25 kg per week", "Lose 0.5 kg per week", "Lose 0.75 kg per week", "Lose 1 kg per week");
                isMultiSelect = false;
                break;
        }

        if (rvOptions != null) {
            OptionsAdapter adapter = new OptionsAdapter(options, isMultiSelect);
            adapter.setOnSelectionChangedListener(hasSelection -> {
                if (getActivity() instanceof SetupActivity) {
                    ((SetupActivity) getActivity()).onSelectionCompleted(hasSelection);
                }
            });
            rvOptions.setAdapter(adapter);
        }
    }

    private void checkCompletionCase6(View view) {
        if (view == null) return;
        TextInputEditText etAge = view.findViewById(R.id.et_input_1_1);
        boolean completed = !selectedGender.isEmpty() && etAge != null && etAge.length() > 0;
        if (getActivity() instanceof SetupActivity) {
            ((SetupActivity) getActivity()).onSelectionCompleted(completed);
        }
    }
}
