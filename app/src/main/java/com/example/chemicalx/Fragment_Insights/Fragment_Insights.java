package com.example.chemicalx.Fragment_Insights;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class Fragment_Insights extends Fragment {
    private static final String TAG = Fragment_Insights.class.getSimpleName();
    private static final long minimumTime = 1000 * 60;
    private static final int NUM_OF_CATEGORIES = 9;

    private UsageListAdapter mUsageListAdapter;
    private UsageStatsManager mUsageStatsManager;
    private List<AppUsageInfo> appUsageInfos;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private Button mOpenUsageSettingButton;
    private PieChart usagePieChart;
    private TextView adviceText;

    public Fragment_Insights() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsageStatsManager = (UsageStatsManager) getActivity()
                .getSystemService(Context.USAGE_STATS_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_insights, container, false);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        // pie chart creation
        usagePieChart = rootView.findViewById(R.id.usage_pie_chart);
        initialisePieChart();

        // productivity advice text
        adviceText = rootView.findViewById(R.id.insights_productivity_advice_text);
        adviceText.setSelected(true);

        mUsageListAdapter = new UsageListAdapter();
        appUsageInfos = new ArrayList<>();
        mOpenUsageSettingButton = (Button) rootView.findViewById(R.id.button_open_usage_setting);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
        getUsageStats();

        // update pie chart with data
        updatePieChart();

        // update productivity advice text
        adviceText.setText("You are more productive after feeding Diglet.\t"
                + "You are less productive after writing an essay.");
    }

    private void getUsageStats() {
        appUsageInfos.clear();

        PackageManager packageManager = getActivity().getPackageManager();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        long end = cal.getTimeInMillis();
        long start = cal.getTimeInMillis() - (1000 * 60 * 60 * 24); // last hour interval
        Log.d(TAG, "start: " + Long.toString(start) + " end: " + Long.toString(end));
        HashMap<String, AppUsageInfo> hm = queryUsageStatistics(getActivity(), start, end);
        Log.d("PRE FILTER", hm.size() + "");

        for (String key : hm.keySet()) {
            AppUsageInfo appUsageInfo = hm.get(key);
            // try finding the app icon
            try {
                Drawable appIcon = packageManager.getApplicationIcon(key);
                appUsageInfo.appIcon = appIcon;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, String.format("App Icon is not found for %s",
                        key));
                appUsageInfo.appIcon = getActivity()
                        .getDrawable(R.drawable.iu);
            }

            // try finding the app info to get the app name and category
            try {
                //
                ApplicationInfo appInfo = packageManager.getApplicationInfo(key, 0);
                appUsageInfo.name = packageManager.getApplicationLabel(appInfo).toString();

                // sorting out some issues with API version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appUsageInfo.category = appInfo.category;
                } else {
                    throw new UnsupportedOperationException();
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, String.format("App Info is not found for %s",
                        key));
                appUsageInfo.name = key;
                appUsageInfo.category = ApplicationInfo.CATEGORY_UNDEFINED;
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, String.format("App Category is not found for %s",
                        key));
                appUsageInfo.category = ApplicationInfo.CATEGORY_UNDEFINED;
            }

            // add packageName to AppUsageInfo
            appUsageInfo.packageName = key;

            //add AppUsageInfo to AppUsageInfoList if >minimumTime used
            if (appUsageInfo.timeInForeground > minimumTime){
                appUsageInfos.add(appUsageInfo);
            }

            Log.d(key, "time in foreground: " + appUsageInfo.timeInForeground);
        }
        Log.d("POST FILTER", appUsageInfos.size() + "");

        mUsageListAdapter.setCustomUsageStatsList(appUsageInfos);
        mUsageListAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(0);
    }

    private HashMap<String, AppUsageInfo> queryUsageStatistics(Context context, long startTime, long endTime) {
        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEvents = new ArrayList<>();
        HashMap<String, AppUsageInfo> map = new HashMap<>();
        assert mUsageStatsManager != null;
        // Here we query the events from startTime till endTime.
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);

        // go over all events.
        while (usageEvents.hasNextEvent()) {
            currentEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(currentEvent);
            String packageName = currentEvent.getPackageName();
            if (currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED || currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED ||
                    currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_STOPPED) {
                allEvents.add(currentEvent); // an extra event is found, add to all events list.
                // taking it into a collection to access by package name
                if (!map.containsKey(packageName)) {
                    map.put(packageName, new AppUsageInfo());
                }
            }
        }

        Log.d("all events size", allEvents.size() + "");
        // iterate through all events.
        for (int i = 0; i < allEvents.size() - 1; i++) {
            UsageEvents.Event event0 = allEvents.get(i);
            UsageEvents.Event event1 = allEvents.get(i + 1);

            // for launchCount of apps in time range
            if (!event0.getPackageName().equals(event1.getPackageName()) && event1.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                // if true, E1 (launch event of an app) app launched
                Objects.requireNonNull(map.get(event1.getPackageName())).launchCount++;
            }

            // for UsageTime of apps in time range
            if (event0.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED &&
                    (event1.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED || event1.getEventType() == UsageEvents.Event.ACTIVITY_STOPPED)
                    && event0.getPackageName().equals(event1.getPackageName())) {
                long diff = event1.getTimeStamp() - event0.getTimeStamp();
                Objects.requireNonNull(map.get(event0.getPackageName())).timeInForeground += diff;
            }
        }

        // if got error, check if user has given permission
        if (map.keySet().size() == 0) {
            Log.i(TAG, "The user may not allow the access to apps usage. ");
            Toast.makeText(getActivity(),
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            mOpenUsageSettingButton.setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.insights_data_layout).setVisibility(View.GONE);
            mOpenUsageSettingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }
            });
        }

        // and return the map.
        return map;
    }

        private void initialisePieChart() {
        // use percent values, not absolute values
        usagePieChart.setUsePercentValues(true);

        // remove labels, rely on the legend
        usagePieChart.setDrawEntryLabels(false);

        // remove description
        usagePieChart.getDescription().setEnabled(false);

        // probably looks better vertical but gets clipped, will change to vertical when I figure it out
        usagePieChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);

        // to prevent clipping, doesn't work for vertical
        usagePieChart.getLegend().setWordWrapEnabled(true);

        // disable all touch-interactions
        usagePieChart.setTouchEnabled(false);
    }

    private void updatePieChart() {
        // update the data
        usagePieChart.setData(generatePieData());

        // refresh
        usagePieChart.invalidate();
    }

    private PieData generatePieData() {
        // total usage times for different categories
        long[] categoryTotalUsage = new long[NUM_OF_CATEGORIES];

        // sum the usage times of the different apps for respective categories
        for (AppUsageInfo appUsageInfo : appUsageInfos) {
            categoryTotalUsage[appUsageInfo.category + 1] += appUsageInfo.timeInForeground;
        }

        // add entries for different categories
        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_UNDEFINED + 1], "Other"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_GAME + 1], "Games"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_AUDIO + 1], "Audio"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_VIDEO + 1], "Video"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_IMAGE + 1], "Imaging"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_SOCIAL + 1], "Social"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_NEWS + 1], "News"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_MAPS + 1], "Maps"));
        pieEntries.add(new PieEntry(categoryTotalUsage[ApplicationInfo.CATEGORY_PRODUCTIVITY + 1], "Productivity"));

        // sort according to usage time to make pie chart neater
        pieEntries.sort(new Comparator<PieEntry>() {
            @Override
            public int compare(PieEntry pieEntry1, PieEntry pieEntry2) {
                return (int) (pieEntry1.getValue() - pieEntry2.getValue());
            }
        });

        // pie data set and settings
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        // pie chart slice colours
        List<Integer> colours = new ArrayList<>();
        colours.add(Color.rgb(255, 0, 0));
        colours.add(Color.rgb(255, 127, 0));
        colours.add(Color.rgb(255, 255, 0));
        colours.add(Color.rgb(127, 255, 0));
        colours.add(Color.rgb(0, 255, 0));
        colours.add(Color.rgb(0, 255, 255));
        colours.add(Color.rgb(0, 0, 255));
        colours.add(Color.rgb(127, 0, 255));
        colours.add(Color.rgb(255, 0, 255));
        pieDataSet.setColors(colours);
        // move percentage labels to outside the slice to make it neater
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData pieData = new PieData(pieDataSet);

        return pieData;
    }
}

