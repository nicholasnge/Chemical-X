package com.example.chemicalx.Fragment_Schedule;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

public class ReadCalendarPermissionDialogFragment extends DialogFragment {
    private ReadCalendarPermissionDialogListener listener;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface ReadCalendarPermissionDialogListener {
        public void onReadCalendarPermissionDialogGrantClick(DialogFragment dialog);
        public void onReadCalendarPermissionDialogDenyClick(DialogFragment dialog);
    }

    // Override the Fragment.onAttach() method to instantiate the
    // ReadCalendarPermissionDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the ReadCalendarPermissionDialogListener so we can send events to the
            // host
            listener = (ReadCalendarPermissionDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement ReadCalendarPermissionDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.schedule_read_calendar_permission_dialog_title)
                .setMessage(R.string.schedule_read_calendar_permission_dialog_message)
                .setPositiveButton(
                        R.string.schedule_read_calendar_permission_dialog_positive_button_text,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onReadCalendarPermissionDialogGrantClick(
                                ReadCalendarPermissionDialogFragment.this);
                    }
                })
                .setNegativeButton(
                        R.string.schedule_read_calendar_permission_dialog_negative_button_text,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onReadCalendarPermissionDialogDenyClick(
                                ReadCalendarPermissionDialogFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}