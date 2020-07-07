package com.example.chemicalx.Fragment_Schedule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.Comparator;
import java.util.Date;


public class Fragment_Schedule extends Fragment {
    private ArrayList<TimeLineModel> mDataList = new ArrayList<>();
    private TimeLineAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView recyclerView;
    private TextView dateAndDay;
    private LinearLayout readCalendarDeniedLayout;
    private LinearLayout readCalendarGrantedLayout;
    private Button grantReadCalendarPermissionButton;

    // for calendar permissions handling
    public static final int READ_CALENDAR_PERMISSION_REQUEST_CODE = 0;
    public static final int WRITE_CALENDAR_PERMISSION_REQUEST_CODE = 1;
    public static boolean isReadCalendarGranted;
    public static boolean isWriteCalendarGranted;

    // for refreshing after permission granted
    private boolean mustRefresh = false;

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    private static final String[] PROJECTION = new String[]{
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN
    };

    // The indices for the projection array above.
    private static final int PROJECTION_TITLE_INDEX = 0;
    private static final int PROJECTION_BEGIN_INDEX = 1;

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

        // assign layouts, text views, buttons, recycler views, etc.
        assignViews(view);

        // check if READ_CALENDAR permission is granted and requests for it if necessary
        handleReadCalendarPermissionRequest();

        // modifies the layout according towhether READ_CALENDAR is granted
        modifyLayoutAccordingToReadCalendarPermission();

        return view;
    }

    private void setDataListItems() {
        // remove previous entries, otherwise, the day's events will repeatedly be added
        mDataList.clear();

        // start of today
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        // end of today
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        // the exact moment right now
        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = CalendarContract.Instances.CONTENT_URI;

        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start.getTimeInMillis());
        ContentUris.appendId(builder, end.getTimeInMillis());

        // Submit the query and get a Cursor object back.
        // method called only after READ_CALENDAR permission obtained so can suppress
        @SuppressLint("MissingPermission")
        Cursor cur = cr.query(builder.build(), PROJECTION, null, null, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            String title;
            long dtstart; // UTC ms since the start of the epoch
            OrderStatus status;

            // Get the field values
            title = cur.getString(PROJECTION_TITLE_INDEX);
            dtstart = cur.getLong(PROJECTION_BEGIN_INDEX);

            // processing of values
            // temporary placeholder status for now
            if (now.getTimeInMillis() < dtstart) {
                status = OrderStatus.INACTIVE;
            } else {
                status = OrderStatus.COMPLETED;
            }

            // add to data list
            mDataList.add(new TimeLineModel(title, dtstart, status));
        }

        // saving these for now for future debugging purposes
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

        // move earlier events nearer to the start of the list
        mDataList.sort(new Comparator<TimeLineModel>() {
            @Override
            public int compare(TimeLineModel tlm1, TimeLineModel tlm2) {
                return (int) (tlm1.getDtstart() - tlm2.getDtstart());
            }
        });
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
        // get today's date and format it to something like "2 July 2020, Thursday"
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM yyyy, EEEE");
        dateAndDay.setText(simpleDateFormat.format(date));
    }

    private void initGrantReadCalendarPermissionButton() {
        // if clicked, request permission
        grantReadCalendarPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toAppPermissionsSettings =
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(),
                        null);
                toAppPermissionsSettings.setData(uri);
                startActivity(toAppPermissionsSettings);
            }
        });
    }

    private void assignViews(View view) {
        readCalendarDeniedLayout = view.findViewById(R.id.schedule_read_calendar_denied_layout);
        readCalendarGrantedLayout = view.findViewById(R.id.schedule_read_calendar_granted_layout);
        grantReadCalendarPermissionButton = view
                .findViewById(R.id.schedule_read_calendar_disabled_grant_permission_button);
        dateAndDay = view.findViewById(R.id.schedule_date_and_day);
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    private void handleReadCalendarPermissionRequest() {
        // check if has calendar permissions, otherwise inform the user why the permission is needed
        // then ask for the permission
        isReadCalendarGranted = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        if (isReadCalendarGranted) {
            // You can use the API that requires the permission.
            // performAction(...);
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            // showInContextUI(...);
            ReadCalendarPermissionDialogFragment readCalendarPermissionDialogFragment =
                    new ReadCalendarPermissionDialogFragment();

            readCalendarPermissionDialogFragment.show(getParentFragmentManager(),
                    "Read Calendar Permission Dialog Fragment");
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_CALENDAR},
                    READ_CALENDAR_PERMISSION_REQUEST_CODE);
        }
    }

    private void modifyLayoutAccordingToReadCalendarPermission() {
        if (isReadCalendarGranted) {
            // change the appearance to the READ_CALENDAR permission granted layout
            readCalendarDeniedLayout.setVisibility(View.GONE);
            readCalendarGrantedLayout.setVisibility(View.VISIBLE);

            // initialise children of the readCalendarGrantedLayout
            initDateAndDay();
            setDataListItems();
            initRecyclerView();
        } else {
            // change the appearance to the READ_CALENDAR permission denied layout
            readCalendarDeniedLayout.setVisibility(View.VISIBLE);
            readCalendarGrantedLayout.setVisibility(View.GONE);

            // initialise children of the readCalendarDeniedLayout
            initGrantReadCalendarPermissionButton();
        }
    }

    public boolean needsToRefresh() {
        return this.mustRefresh;
    }

    public void refresh() {
        this.mustRefresh = true;
    }

    public void hasRefreshed() {
        this.mustRefresh = false;
    }
}