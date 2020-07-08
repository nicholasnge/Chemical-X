package com.example.chemicalx.Fragment_Schedule;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

import java.util.Calendar;

public class UpdateEventDateTimeDialogFragment extends DialogFragment {
    private UpdateEventDateTimeDialogListener listener;
    private long dateTimeMillis;
    private boolean isAllDay;
    private boolean is24HourFormat;
    private View view;

    private DatePicker datePicker;
    private TimePicker timePicker;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface UpdateEventDateTimeDialogListener {
        public void onUpdateEventDateTimeDialogUpdateClick(DialogFragment dialog,
                                                           long dateTimeMillis);
        default public void onUpdateEventDateTimeDialogCancelClick(DialogFragment dialog) {
            dialog.getDialog().cancel();
        }
    }

    public UpdateEventDateTimeDialogFragment(UpdateEventDateTimeDialogListener listener,
                                             long dateTimeMillis, boolean isAllDay,
                                             boolean is24HourFormat) {
        this.listener = listener;
        this.dateTimeMillis = dateTimeMillis;
        this.isAllDay = isAllDay;
        this.is24HourFormat = is24HourFormat;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fragment_dialog_add_event_update_datetime, null);

        assignViews();
        initViews();

        builder.setTitle(R.string.add_event_update_datetime_dialog_title)
                .setView(view)
                .setPositiveButton(
                        R.string.add_event_update_datetime_dialog_positive_button_text,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onUpdateEventDateTimeDialogUpdateClick(
                                UpdateEventDateTimeDialogFragment.this,
                                findDateTimeMillis());
                    }
                })
                .setNegativeButton(
                        R.string.cancel,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onUpdateEventDateTimeDialogCancelClick(
                                UpdateEventDateTimeDialogFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void assignViews() {
        datePicker = view.findViewById(R.id.update_event_datetime_dialog_datepicker);
        timePicker = view.findViewById(R.id.update_event_datetime_dialog_timepicker);
    }

    private void initViews() {
        initDatePicker();
        initTimePicker();
    }

    private void initDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateTimeMillis);
        datePicker.updateDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private void initTimePicker() {
        if (isAllDay) {
            timePicker.setVisibility(View.GONE);
        } else {
            timePicker.setVisibility(View.VISIBLE);
            timePicker.setIs24HourView(is24HourFormat);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateTimeMillis);
        timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(calendar.get(Calendar.MINUTE));
    }

    private long findDateTimeMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                timePicker.getHour(), timePicker.getMinute());
        return calendar.getTimeInMillis();
    }
}