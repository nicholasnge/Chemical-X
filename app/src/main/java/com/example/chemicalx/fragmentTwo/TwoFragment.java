package com.example.chemicalx.fragmentTwo;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class TwoFragment extends Fragment {
    private static final String TAG = TwoFragment.class.getSimpleName();
    private static final long minimumTime = 1000 * 60;

    UsageListAdapter mUsageListAdapter;
    private UsageStatsManager mUsageStatsManager;
    private List<AppUsageInfo> AppUsageInfos;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    Button mOpenUsageSettingButton;

    public TwoFragment() {
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
        return inflater.inflate(R.layout.fragment_two, container, false);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        mUsageListAdapter = new UsageListAdapter();
        AppUsageInfos = new ArrayList<>();
        mOpenUsageSettingButton = (Button) rootView.findViewById(R.id.button_open_usage_setting);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
        getUsageStats();
    }

    private void getUsageStats() {
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
                Drawable appIcon = getActivity().getPackageManager()
                        .getApplicationIcon(key);
                appUsageInfo.appIcon = appIcon;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, String.format("App Icon is not found for %s",
                        key));
                appUsageInfo.appIcon = getActivity()
                        .getDrawable(R.drawable.iu);
            }
            // add packageName to AppUsageInfo
            appUsageInfo.packageName = key;

            //add AppUsageInfo to AppUsageInfoList if >minimumTime used
            if (appUsageInfo.timeInForeground > minimumTime){
                AppUsageInfos.add(appUsageInfo);
            }

            Log.d(key, "time in foreground: " + appUsageInfo.timeInForeground);
        }
        Log.d("POST FILTER", AppUsageInfos.size() + "");

        mUsageListAdapter.setCustomUsageStatsList(AppUsageInfos);
        mUsageListAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(0);
    }

    public HashMap<String, AppUsageInfo> queryUsageStatistics(Context context, long startTime, long endTime) {
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

            //for launchCount of apps in time range
            if (!event0.getPackageName().equals(event1.getPackageName()) && event1.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                // if true, E1 (launch event of an app) app launched
                Objects.requireNonNull(map.get(event1.getPackageName())).launchCount++;
            }

            //for UsageTime of apps in time range
            if (event0.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED &&
                    (event1.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED || event1.getEventType() == UsageEvents.Event.ACTIVITY_STOPPED)
                    && event0.getPackageName().equals(event1.getPackageName())) {
                long diff = event1.getTimeStamp() - event0.getTimeStamp();
                Objects.requireNonNull(map.get(event0.getPackageName())).timeInForeground += diff;
            }
        }

        //if got error, check if user has given permission
        if (map.keySet().size() == 0) {
            Log.i(TAG, "The user may not allow the access to apps usage. ");
            Toast.makeText(getActivity(),
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            mOpenUsageSettingButton.setVisibility(View.VISIBLE);
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
}

