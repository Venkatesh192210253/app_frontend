package com.simats.myfitnessbuddy;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return OnboardingFragment.newInstance(
                        "Track Calories",
                        "Monitor your daily calorie intake and stay on top of your nutrition goals with ease",
                        R.drawable.ic_chart
                );
            case 1:
                return OnboardingFragment.newInstance(
                        "Compare With Friends",
                        "Join groups, create challenges, and compete with friends to stay motivated",
                        R.drawable.ic_people
                );
            case 2:
                return OnboardingFragment.newInstance(
                        "AI Coach",
                        "Get personalized diet plans, workout suggestions, and real-time guidance from your AI coach",
                        R.drawable.ic_robot
                );
            default:
                return OnboardingFragment.newInstance("", "", 0);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
