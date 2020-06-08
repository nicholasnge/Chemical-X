package com.example.chemicalx.fragmentTwo;

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
import java.util.List;


public class TwoFragment extends Fragment {
    private static final String TAG = TwoFragment.class.getSimpleName();

    UsageListAdapter mUsageListAdapter;
    private UsageStatsManager mUsageStatsManager;
    private ArrayList<CustomUsageStats> customUsageStats;
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

        customUsageStats = new ArrayList<>();
        mUsageListAdapter = new UsageListAdapter();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
        getUsageStats();
    }

    private void getUsageStats() {
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_MONTH, -1);
        long start = cal.getTimeInMillis();
        Log.d(TAG, "start: " + Long.toString(start) + " end: " + Long.toString(end));
        List<UsageStats> queryUsageStats = mUsageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);

        //if got error, check if user has given permission
        if (queryUsageStats.size() == 0) {
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

        for (UsageStats stat : queryUsageStats){
            if (stat.getTotalTimeInForeground() > 60000) {
                CustomUsageStats customStat = new CustomUsageStats();
                customStat.usageStats = stat;
                try {
                    Drawable appIcon = getActivity().getPackageManager()
                            .getApplicationIcon(stat.getPackageName());
                    customStat.appIcon = appIcon;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, String.format("App Icon is not found for %s",
                            stat.getPackageName()));
                    customStat.appIcon = getActivity()
                            .getDrawable(R.drawable.iu);
                }
                customUsageStats.add(customStat);
                Log.d(stat.getPackageName(), "1st time stamp: " + stat.getFirstTimeStamp()
                        + ", end time stamp: " + stat.getLastTimeStamp() + ", value: " + stat.getTotalTimeInForeground());
            }
        }
        Log.d("LISTSIZE", queryUsageStats.size() + "");
        Log.d("USAGE LIST SIZE", customUsageStats.size() + "");

        mUsageListAdapter.setCustomUsageStatsList(customUsageStats);
        mUsageListAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(0);
    }
}

