package com.example.chemicalx.Fragment_Schedule;

import android.Manifest;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class AddEventDialogFragment extends DialogFragment {
    private static final String TAG = "AddEventDialogFragment";
    private static final boolean DEFAULT_IS_ALL_DAY_VALUE = false;

    private final SimpleDateFormat dateTimeStandardDateFormat;
    private final SimpleDateFormat dateStandardDateFormat;

    private Fragment_Schedule caller;
    private boolean isWriteCalendarGranted;
    private AlertDialog addEventAlertDialog;
    private long calendarID;
    private long startDateTime;
    private long endDateTime;
    private boolean is24HourFormat;
    private TimeZone defaultTimeZone;
    private boolean isAllDay;
    private SimpleDateFormat df;

    private View view;
    private EditText eventTitleEditText;
    private Switch eventAllDaySwitch;
    private EditText eventStartDateTimeEditText;
    private EditText eventEndDateTimeEditText;
    private Spinner eventRecurrenceSpinner;
    private LinearLayout writeCalendarDisabledLayout;
    private Button writeCalendarGrantPermissionButton;

    // spinner item positions
    private static final int RECUR_NEVER = 0;
    private static final int RECUR_DAILY = 1;
    private static final int RECUR_WEEKLY = 2;
    private static final int RECUR_MONTHLY = 3;
    private static final int RECUR_YEARLY = 4;

    // for calendar permissions handling
    public static final int WRITE_CALENDAR_PERMISSION_PRE_DENIAL_REQUEST_CODE = 0;
    public static final int WRITE_CALENDAR_PERMISSION_POST_DENIAL_REQUEST_CODE = 1;

    // request codes for startActivityForResult
    private static final int GRANT_WRITE_CALENDAR = 0;

    // AsyncQueryHandler tokens
    private static final int ADD_EVENT = 0;

    // default start and end hour offsets
    private static final int DEFAULT_START_HOUR_OFFSET = 1;
    private static final int DEFAULT_END_HOUR_OFFSET = 2;

    public AddEventDialogFragment(Fragment_Schedule caller, long calendarID) {
        this.caller = caller;
        this.calendarID = calendarID;
        this.is24HourFormat = DateFormat.is24HourFormat(caller.getContext());
        if (this.is24HourFormat) {
            this.dateTimeStandardDateFormat = new SimpleDateFormat("EEE d MMM yyyy kk:mm");
        } else {
            this.dateTimeStandardDateFormat = new SimpleDateFormat("EEE d MMM yyyy hh:mm a");
        }
        this.dateStandardDateFormat = new SimpleDateFormat("EEE d MMM yyyy");
        this.df = dateTimeStandardDateFormat;
        this.defaultTimeZone = TimeZone.getDefault();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fragment_dialog_schedule_add_event, null);

        assignViews();
        initViews();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        addEventAlertDialog = builder
                .setTitle(R.string.schedule_add_event_dialog_title)
                .setView(view)
                .setPositiveButton(
                        R.string.schedule_add_event_dialog_positive_button_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handleWriteCalendarPermissionRequest();
                            }
                        })
                .setNeutralButton(
                        R.string.schedule_add_event_dialog_neutral_button_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                addEventIndirectly();
                            }
                        })
                .setNegativeButton(
                        R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .create();

        addEventAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                addEventAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                handleWriteCalendarPermissionRequest();
                            }
                        });
            }
        });

        // Create the AlertDialog object and return it
        return addEventAlertDialog;
    }

    private void assignViews() {
        eventTitleEditText = view.findViewById(R.id.add_event_dialog_event_title_edittext);
        eventAllDaySwitch = view.findViewById(R.id.add_event_dialog_event_all_day_switch);
        eventStartDateTimeEditText = view
                .findViewById(R.id.add_event_dialog_event_start_datetime_edittext);
        eventEndDateTimeEditText = view
                .findViewById(R.id.add_event_dialog_event_end_datetime_edittext);
        eventRecurrenceSpinner = view.findViewById(R.id.add_event_dialog_event_recurrence_spinner);
        writeCalendarDisabledLayout = view
                .findViewById(R.id.add_event_dialog_write_calendar_disabled_layout);
        writeCalendarGrantPermissionButton = view
                .findViewById(R.id.add_event_dialog_write_calendar_grant_permission_button);
    }

    private void initViews() {
        initEventAllDaySwitch();
        initEventRecurrenceSpinner();
        initEventStartDateTimeEditText();
        initEventEndDateTimeEditText();
        initWriteCalendarDisabledLayout();
        initWriteCalendarGrantPermissionButton();
    }

    private void initEventAllDaySwitch() {
        isAllDay = DEFAULT_IS_ALL_DAY_VALUE;
        eventAllDaySwitch.setChecked(isAllDay);

        eventAllDaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAllDay = isChecked;
                int inputTypeNum;

                if (isChecked) {
                    inputTypeNum = InputType.TYPE_DATETIME_VARIATION_DATE;
                    df = dateStandardDateFormat;
                } else {
                    inputTypeNum = InputType.TYPE_DATETIME_VARIATION_NORMAL;
                    df = dateTimeStandardDateFormat;
                }

                updateDateTimeEditTextFormat(eventStartDateTimeEditText, startDateTime,
                        inputTypeNum);
                updateDateTimeEditTextFormat(eventEndDateTimeEditText, endDateTime,
                        inputTypeNum);
            }
        });
    }

    private void initEventRecurrenceSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.field_recurrence_rule_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        eventRecurrenceSpinner.setAdapter(adapter);
    }

    private void initEventStartDateTimeEditText() {
        eventStartDateTimeEditText.setInputType(InputType.TYPE_DATETIME_VARIATION_NORMAL);
        Calendar calendarNow = Calendar.getInstance();
        calendarNow.add(Calendar.HOUR_OF_DAY, DEFAULT_START_HOUR_OFFSET);
        setCalendarToNearestPassedHourOfDay(calendarNow);
        eventStartDateTimeEditText.setText(df.format(calendarNow.getTime()));
        startDateTime = calendarNow.getTimeInMillis();
        eventStartDateTimeEditText.setKeyListener(null);
        eventStartDateTimeEditText.setFocusable(false);
        eventStartDateTimeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateEventDateTimeDialogFragment.UpdateEventDateTimeDialogListener
                        updateEventDateTimeDialogListener =
                        new UpdateEventDateTimeDialogFragment.UpdateEventDateTimeDialogListener() {
                    @Override
                    public void onUpdateEventDateTimeDialogUpdateClick(
                            DialogFragment dialog, long dateTimeMillis) {
                        startDateTime = dateTimeMillis;
                        updateDateTimeEditText(eventStartDateTimeEditText, dateTimeMillis);
                    }
                };
                UpdateEventDateTimeDialogFragment updateEventDateTimeDialogFragment =
                        new UpdateEventDateTimeDialogFragment(updateEventDateTimeDialogListener,
                                startDateTime, isAllDay, is24HourFormat);
                updateEventDateTimeDialogFragment.show(getParentFragmentManager(),
                        "Update Event Start Date-Time Dialog Fragment");
            }
        });
    }

    private void initEventEndDateTimeEditText() {
        eventEndDateTimeEditText.setInputType(InputType.TYPE_DATETIME_VARIATION_NORMAL);
        Calendar calendarLater = Calendar.getInstance();
        calendarLater.add(Calendar.HOUR_OF_DAY, DEFAULT_END_HOUR_OFFSET);
        setCalendarToNearestPassedHourOfDay(calendarLater);
        eventEndDateTimeEditText.setText(df.format(calendarLater.getTime()));
        endDateTime = calendarLater.getTimeInMillis();
        eventEndDateTimeEditText.setKeyListener(null);
        eventEndDateTimeEditText.setFocusable(false);
        eventEndDateTimeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateEventDateTimeDialogFragment.UpdateEventDateTimeDialogListener
                        updateEventDateTimeDialogListener =
                        new UpdateEventDateTimeDialogFragment.UpdateEventDateTimeDialogListener() {
                    @Override
                    public void onUpdateEventDateTimeDialogUpdateClick(
                            DialogFragment dialog, long dateTimeMillis) {
                        endDateTime = dateTimeMillis;
                        updateDateTimeEditText(eventEndDateTimeEditText, dateTimeMillis);
                    }
                };
                UpdateEventDateTimeDialogFragment updateEventDateTimeDialogFragment =
                        new UpdateEventDateTimeDialogFragment(updateEventDateTimeDialogListener,
                                endDateTime, isAllDay, is24HourFormat);
                updateEventDateTimeDialogFragment.show(getParentFragmentManager(),
                        "Update Event End Date-Time Dialog Fragment");
            }
        });
    }

    private void initWriteCalendarDisabledLayout() {
        writeCalendarDisabledLayout.setVisibility(View.GONE);
    }

    private void initWriteCalendarGrantPermissionButton() {
        writeCalendarGrantPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleWriteCalendarPermissionRequest();
            }
        });
    }

    private void setCalendarToNearestPassedHourOfDay(Calendar calendar) {
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void updateDateTimeEditTextFormat(EditText dateTimeEditText, long dateTime,
                                              int inputTypeNum) {
        dateTimeEditText.setInputType(inputTypeNum);
        updateDateTimeEditText(dateTimeEditText, dateTime);
    }

    private void updateDateTimeEditText(EditText dateTimeEditText, long dateTime) {
        Date date = new Date();
        date.setTime(dateTime);
        String dateString = df.format(date);
        dateTimeEditText.setText(dateString);
    }

    private void addEventDirectly() {
        String title = eventTitleEditText.getText().toString();
        String startDateTimeString = eventStartDateTimeEditText.getText().toString();
        String endDateTimeString = eventEndDateTimeEditText.getText().toString();
        int recurrenceSpinnerPos = eventRecurrenceSpinner.getSelectedItemPosition();

        SimpleDateFormat df;

        if (isAllDay) {
            df = dateStandardDateFormat;
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else {
            df = dateTimeStandardDateFormat;
            df.setTimeZone(defaultTimeZone);
        }

        try {
            startDateTime = df.parse(startDateTimeString).getTime();
            endDateTime = df.parse(endDateTimeString).getTime();
            if (isAllDay) {
                // endDateTime is exclusive and must be set a day after the actual all-day end date
                endDateTime += 1 * 24 * 60 * 60 * 1000;
            }
        } catch (ParseException parseException) {
            Toast.makeText(getContext(),
                    "There was a problem with processing the date and/or time. Please try again.",
                    Toast.LENGTH_LONG);
            Log.e(TAG, parseException.getMessage());
            return;
        }

        ContentResolver cr = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.ALL_DAY, isAllDay);
        values.put(CalendarContract.Events.DTSTART, startDateTime);
        if (recurrenceSpinnerPos == RECUR_NEVER) {
            values.put(CalendarContract.Events.DTEND, endDateTime);
        } else {
            long duration = (endDateTime - startDateTime) / 1000;
            values.put(CalendarContract.Events.DURATION, String.format("PT%dS", duration));
            switch (recurrenceSpinnerPos) {
                case RECUR_DAILY:
                    values.put(CalendarContract.Events.RRULE, "FREQ=DAILY");
                    break;
                case RECUR_WEEKLY:
                    values.put(CalendarContract.Events.RRULE, "FREQ=WEEKLY");
                    break;
                case RECUR_MONTHLY:
                    values.put(CalendarContract.Events.RRULE, "FREQ=MONTHLY");
                    break;
                case RECUR_YEARLY:
                    values.put(CalendarContract.Events.RRULE, "FREQ=YEARLY");
                    break;
            }
        }
        values.put(CalendarContract.Events.CALENDAR_ID, calendarID);
        if (isAllDay) {
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
        } else {
            values.put(CalendarContract.Events.EVENT_TIMEZONE, defaultTimeZone.getID());
        }

        AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(cr) {
            @Override
            protected void onInsertComplete(int token, Object cookie, Uri uri) {
                switch (token) {
                    case ADD_EVENT:
                        caller.updateTimelineRecyclerView();
                        return;
                }
            }
        };

        asyncQueryHandler.startInsert(ADD_EVENT, null, CalendarContract.Events.CONTENT_URI,
                values);

        dismiss();
    }

    private void addEventIndirectly() {
        String title = eventTitleEditText.getText().toString();
        String startDateTimeString = eventStartDateTimeEditText.getText().toString();
        String endDateTimeString = eventEndDateTimeEditText.getText().toString();
        int recurrenceSpinnerPos = eventRecurrenceSpinner.getSelectedItemPosition();

        SimpleDateFormat df =
                isAllDay
                        ? dateStandardDateFormat
                        : dateTimeStandardDateFormat;

        long startDateTime;
        long endDateTime;

        try {
            startDateTime = df.parse(startDateTimeString).getTime();
            endDateTime = df.parse(endDateTimeString).getTime();
        } catch (ParseException parseException) {
            Toast.makeText(getContext(),
                    "There was a problem with processing the date and/or time. Please try again.",
                    Toast.LENGTH_LONG);
            Log.e(TAG, parseException.getMessage());
            return;
        }

        Intent toCalendarAddEventActivity = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.ALL_DAY, isAllDay)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDateTime)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDateTime);

        switch (recurrenceSpinnerPos) {
            case RECUR_DAILY:
                toCalendarAddEventActivity.putExtra(CalendarContract.Events.RRULE,
                        "FREQ=DAILY");
                break;
            case RECUR_WEEKLY:
                toCalendarAddEventActivity.putExtra(CalendarContract.Events.RRULE,
                        "FREQ=WEEKLY");
                break;
            case RECUR_MONTHLY:
                toCalendarAddEventActivity.putExtra(CalendarContract.Events.RRULE,
                        "FREQ=MONTHLY");
                break;
            case RECUR_YEARLY:
                toCalendarAddEventActivity.putExtra(CalendarContract.Events.RRULE,
                        "FREQ=YEARLY");
                break;
        }

        caller.startCalendarAddEventActivity(toCalendarAddEventActivity);
    }

    private void handleWriteCalendarPermissionRequest() {
        // check if has calendar permissions, otherwise inform the user why the permission is needed
        // then ask for the permission
        isWriteCalendarGranted = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        if (isWriteCalendarGranted) {
            // You can use the API that requires the permission.
            // performAction(...);
            addEventDirectly();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            // showInContextUI(...);
            showRequestPermissionRationale();
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(getActivity(),
                    new String[] { Manifest.permission.WRITE_CALENDAR },
                    WRITE_CALENDAR_PERMISSION_POST_DENIAL_REQUEST_CODE);
        }
    }

    private void showRequestPermissionRationale() {
        // TODO create actual UI for this and not just request straight away
        ActivityCompat.requestPermissions(getActivity(),
                new String[] { Manifest.permission.WRITE_CALENDAR },
                WRITE_CALENDAR_PERMISSION_PRE_DENIAL_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case WRITE_CALENDAR_PERMISSION_PRE_DENIAL_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    addEventDirectly();
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
                        addEventAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setEnabled(false);
                        writeCalendarDisabledLayout.setVisibility(View.VISIBLE);
                    }
                }
                return;
            case WRITE_CALENDAR_PERMISSION_POST_DENIAL_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
                        Intent toAppPermissionsSettings =
                                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getContext().getPackageName(),
                                null);
                        toAppPermissionsSettings.setData(uri);
                        startActivityForResult(toAppPermissionsSettings, GRANT_WRITE_CALENDAR);
                    }
                }
                return;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GRANT_WRITE_CALENDAR:
                isWriteCalendarGranted = ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
                if (isWriteCalendarGranted) {
                    writeCalendarDisabledLayout.setVisibility(View.GONE);
                    addEventAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                return;
        }
    }
}