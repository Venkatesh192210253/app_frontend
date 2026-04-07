package com.simats.myfitnessbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View indicator1, indicator2, indicator3;
    private Button btnNext;
    private TextView tvSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        View mainView = findViewById(R.id.main);
        int initialPaddingLeft = mainView.getPaddingLeft();
        int initialPaddingTop = mainView.getPaddingTop();
        int initialPaddingRight = mainView.getPaddingRight();
        int initialPaddingBottom = mainView.getPaddingBottom();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            );
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        indicator1 = findViewById(R.id.indicator1);
        indicator2 = findViewById(R.id.indicator2);
        indicator3 = findViewById(R.id.indicator3);
        btnNext = findViewById(R.id.btn_next);
        tvSkip = findViewById(R.id.tv_skip);

        OnboardingAdapter adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUI(position);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() < 2) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                } else {
                    proceedFromOnboarding();
                }
            }
        });

        tvSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proceedFromOnboarding();
            }
        });
    }

    private void proceedFromOnboarding() {
        com.simats.myfitnessbuddy.data.local.SettingsManager.INSTANCE.setOnboardingSeen(true);
        Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUI(int position) {
        indicator1.setBackgroundColor(ContextCompat.getColor(this, position == 0 ? R.color.vibrant_green : R.color.light_gray));
        indicator2.setBackgroundColor(ContextCompat.getColor(this, position == 1 ? R.color.vibrant_green : R.color.light_gray));
        indicator3.setBackgroundColor(ContextCompat.getColor(this, position == 2 ? R.color.vibrant_green : R.color.light_gray));

        if (position == 2) {
            btnNext.setText("Get Started");
            tvSkip.setVisibility(View.INVISIBLE);
        } else {
            btnNext.setText("Next");
            tvSkip.setVisibility(View.VISIBLE);
        }
    }
}
