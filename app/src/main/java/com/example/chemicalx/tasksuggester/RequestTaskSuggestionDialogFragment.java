package com.example.chemicalx.tasksuggester;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

import java.util.Calendar;

public class RequestTaskSuggestionDialogFragment extends DialogFragment {
    private static final String INVALID_DURATION_INPUT_MESSAGE = "Please enter a non-zero desired duration.";
    private RequestTaskSuggestionDialogListener listener;
    private View view;

    private TimePicker timePicker;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface RequestTaskSuggestionDialogListener {
        public void onRequestTaskSuggestionDialogRequestClick(
                DialogFragment dialog, int durationInMinutes);
    }

    public RequestTaskSuggestionDialogFragment(RequestTaskSuggestionDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        view = inflater
                .inflate(R.layout.fragment_dialog_request_task_suggestion, null);

        assignViews();
        initViews();

        // Use the Builder class for convenient dialog construction
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.request_task_suggestion_dialog_title)
                .setView(view)
                .setMessage(R.string.request_task_suggestion_dialog_message)
                .setPositiveButton(R.string.request_task_suggestion_dialog_positive_button_text,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                handleSubmissionRequest(alertDialog);
                            }
                        });
            }
        });

        return alertDialog;
    }

    private void assignViews() {
        timePicker = view.findViewById(R.id.request_task_suggestion_dialog_timepicker);
    }

    private void initViews() {
        initTimePicker();
    }

    private void initTimePicker() {
        timePicker.setIs24HourView(true);
        timePicker.setHour(1);
        timePicker.setMinute(0);
    }

    private void handleSubmissionRequest(AlertDialog alertDialog) {
        int numOfHours = timePicker.getHour();
        int numOfMinutes = timePicker.getMinute();
        int durationInMinutes = numOfHours * 60 + numOfMinutes;
        if (numOfHours != 0 || numOfMinutes != 0) {
            listener.onRequestTaskSuggestionDialogRequestClick(
                    RequestTaskSuggestionDialogFragment.this, durationInMinutes);
            dismiss();
        } else {
            TextView messageView = alertDialog.findViewById(android.R.id.message);
            String message = messageView.getText().toString();
            if (!message.contains(INVALID_DURATION_INPUT_MESSAGE)) {
                message += "\n\n" + INVALID_DURATION_INPUT_MESSAGE;
                messageView.setText(message);
            }
        }
    }
}