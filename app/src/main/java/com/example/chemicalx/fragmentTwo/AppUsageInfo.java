package com.example.chemicalx.fragmentTwo;

import android.graphics.drawable.Drawable;

public class AppUsageInfo {
    public String packageName;
    public long timeInForeground;
    public int launchCount;
    public Drawable appIcon;

    AppUsageInfo() {
        this.timeInForeground = 0;
        this.launchCount = 0;
    }
}