package com.example.chemicalx.Fragment_Insights;

import android.graphics.drawable.Drawable;

public class AppUsageInfo {
    public String name;
    public String packageName;
    public long timeInForeground;
    public int launchCount;
    public int category;
    public Drawable appIcon;

    AppUsageInfo() {
        this.timeInForeground = 0;
        this.launchCount = 0;
    }
}