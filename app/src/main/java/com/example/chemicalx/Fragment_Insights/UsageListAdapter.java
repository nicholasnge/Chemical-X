/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.chemicalx.Fragment_Insights;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.chemicalx.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

/**
 * Provide views to RecyclerView with the directory entries.
 */
public class UsageListAdapter extends RecyclerView.Adapter<UsageListAdapter.ViewHolder> {

    private List<AppUsageInfo> appUsageInfoList = new ArrayList<>();
    private DateFormat mDateFormat = new SimpleDateFormat();

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mPackageName;
        private final TextView mMinutesUsed;
        private final ImageView mAppIcon;
        private final HorizontalBarChart usageBar;

        public ViewHolder(View v) {
            super(v);
            mPackageName = (TextView) v.findViewById(R.id.textview_package_name);
            mMinutesUsed = (TextView) v.findViewById(R.id.textview_minutes_used);
            mAppIcon = (ImageView) v.findViewById(R.id.app_icon);
            usageBar = (HorizontalBarChart) v.findViewById(R.id.usage_bar_chart);
            initialiseUsageBar();
        }

        private void initialiseUsageBar() {
            // make x-axis (right axis) invisible
            usageBar.getXAxis().setEnabled(false);

            // make left y-axis (top axis) invisible
            usageBar.getAxisLeft().setEnabled(false);

            // set common range, ideally should be from 0 to the largest time spent
            usageBar.getAxisLeft().setAxisMaximum(123f); // 123f placeholder of largest
            usageBar.getAxisLeft().setAxisMinimum(0f);

            // make right y-axis (bottom axis) invisible
            usageBar.getAxisRight().setEnabled(false);

            // make legend invisible
            usageBar.getLegend().setEnabled(false);

            // make description invisible
            usageBar.getDescription().setEnabled(false);

            // disable all possible touch-interactions
            usageBar.setTouchEnabled(false);
        }

        public void updateViewHolder(AppUsageInfo appUsageInfo) {
            mPackageName.setText(appUsageInfo.name);
            mMinutesUsed.setText(Long.toString(appUsageInfo.timeInForeground/60000));
            mAppIcon.setImageDrawable(appUsageInfo.appIcon);
            updateUsageBar(appUsageInfo);
        }

        private void updateUsageBar(AppUsageInfo appUsageInfo) {
            // entries for a data set, only the time in this case
            List<BarEntry> barEntries = new ArrayList<>();
            barEntries.add(new BarEntry(0, appUsageInfo.timeInForeground / 60 / 1000));

            // the data set and its settings
            BarDataSet barDataSet = new BarDataSet(barEntries, "App Usage");
            barDataSet.setColor(Color.rgb(232, 174, 104));

            // the bar data (all of the data sets) and its settings
            BarData barData = new BarData(barDataSet);
            barData.setDrawValues(false);

            // set the data
            usageBar.setData(barData);

            // refresh
            usageBar.invalidate();
        }
    }

    public UsageListAdapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.usage_row, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.updateViewHolder(appUsageInfoList.get(position));
    }

    @Override
    public int getItemCount() {
        return appUsageInfoList.size();
    }

    public void setCustomUsageStatsList(List<AppUsageInfo> appUsageInfoList) {
        // sort according to usage time
        appUsageInfoList.sort(new Comparator<AppUsageInfo>() {
            @Override
            public int compare(AppUsageInfo info1, AppUsageInfo info2) {
                return (int) (info2.timeInForeground - info1.timeInForeground);
            }
        });

        this.appUsageInfoList = appUsageInfoList;
    }
}