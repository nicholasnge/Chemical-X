package com.example.chemicalx.Fragment_Schedule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class Fragment_Schedule extends Fragment {
    private ArrayList<TimeLineModel> mDataList = new ArrayList<>();
    private TimeLineAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView recyclerView;
    private TextView dateAndDay;

    // for calendar permissions
    public static final int READ_CALENDAR_PERMISSION_REQUEST_CODE = 0;
    public static final int WRITE_CALENDAR_PERMISSION_REQUEST_CODE = 1;
    public static boolean isReadCalendarEnabled;
    public static boolean isWriteCalendarEnabled = false;

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    private static final String[] PROJECTION = new String[]{
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
    };

    // The indices for the projection array above.
    private static final int PROJECTION_TITLE_INDEX = 0;
    private static final int PROJECTION_DTSTART_INDEX = 1;

    public Fragment_Schedule() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        if (view == null) {
            Log.d("TAG", "VIEW IS NULL");
        }
        recyclerView = view.findViewById(R.id.recyclerView);
        dateAndDay = view.findViewById(R.id.schedule_date_and_day);
        initDateAndDay();

        // check if has calendar permissions
        if (ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            isReadCalendarEnabled = true;
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            isReadCalendarEnabled = false;
            // showInContextUI(...);
        } else {
            // You can directly ask for the permission.
            isReadCalendarEnabled = false;
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_CALENDAR},
                    READ_CALENDAR_PERMISSION_REQUEST_CODE);
        }

        if (isReadCalendarEnabled) {
            setDataListItems();
        }
        initRecyclerView();
        return view;
    }

    private void setDataListItems() {
        mDataList.clear();

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = CalendarContract.Events.CONTENT_URI;
        String selection =
                "((" + CalendarContract.Events.DTSTART + " >= " + start.getTimeInMillis() + ") "
                        + "AND (" + CalendarContract.Events.DTSTART + " <= " + end.getTimeInMillis() + ") "
                        + "AND (deleted != 1))";
        // Submit the query and get a Cursor object back.
        @SuppressLint("MissingPermission") // only called after permission obtained so no issue
        Cursor cur = cr.query(uri, PROJECTION, selection, null, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            String title;
            long dtstart;
            String dateText;
            OrderStatus status;

            // Get the field values
            title = cur.getString(PROJECTION_TITLE_INDEX);
            dtstart = cur.getLong(PROJECTION_DTSTART_INDEX);

            // processing
            Date date = new Date(dtstart);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
            dateText = simpleDateFormat.format(date);
            // temporary status
            if (now.getTimeInMillis() < dtstart) {
                status = OrderStatus.INACTIVE;
            } else {
                status = OrderStatus.COMPLETED;
            }

            // add to data list
            mDataList.add(new TimeLineModel(title, dateText, status));
        }
//        mDataList.add(new TimeLineModel("CS2030 Lecture", "10:00 AM", OrderStatus.COMPLETED));
//        mDataList.add(new TimeLineModel("CS2040 Tutorial", "12:00 PM", OrderStatus.COMPLETED));
//        mDataList.add(new TimeLineModel("Lunch", "1:30 PM", OrderStatus.COMPLETED));
//        mDataList.add(new TimeLineModel("Work - Recreation remaining: -4 min", "2:00 PM", OrderStatus.COMPLETED));
//        mDataList.add(new TimeLineModel("Dinner Break", "6:30 PM", OrderStatus.COMPLETED));
//        mDataList.add(new TimeLineModel("UTW1702B Recitation", "8:00 PM", OrderStatus.ACTIVE));
//        mDataList.add(new TimeLineModel("Work - Recreation remaining: 90 min", "9:30 PM", OrderStatus.INACTIVE));
//        mDataList.add(new TimeLineModel("Sleep - Overdue by: 0 min", "12:30 AM", OrderStatus.INACTIVE));
//        mDataList.add(new TimeLineModel("Work - Recreation remaining: 90 min", "9:30 PM", OrderStatus.INACTIVE));
//        mDataList.add(new TimeLineModel("Sleep - Overdue by: 0 min", "12:30 AM", OrderStatus.INACTIVE));
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TimeLineAdapter(mDataList);
        recyclerView.setAdapter(mAdapter);
    }

    private void initDateAndDay() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM, EEEE");
        dateAndDay.setText(simpleDateFormat.format(date));
    }
}