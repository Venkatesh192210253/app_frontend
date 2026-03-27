package com.simats.myfitnessbuddy;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SetupPagerAdapter extends FragmentStateAdapter {

    public SetupPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return SetupStepFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return 9;
    }
}
