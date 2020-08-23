package com.example.chemicalx.tasksuggester;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.Fragment_Tasks.TaskItemModel;
import com.example.chemicalx.R;

public class TaskSuggestionResultDialogFragment extends DialogFragment {
    private TaskItemModel task;
    private int numOfHours;
    private int numOfMinutes;

    public TaskSuggestionResultDialogFragment(
            TaskItemModel task, int numOfHours, int numOfMinutes) {
        this.task = task;
        this.numOfHours = numOfHours;
        this.numOfMinutes = numOfMinutes;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.task_suggestion_result_dialog_title)
                .setMessage(getMessage())
                .setNeutralButton(R.string.okay,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                .create();

        return alertDialog;
    }

    private String getMessage() {
        String message;
        if (task == null) {
            message = getContext()
                    .getResources()
                    .getString(R.string.task_suggestion_result_dialog_failed_request_message);
        } else {
            message = "You should do \"" + task.getTitle() + "\" for the next";
            if (numOfHours > 0) {
                message += " " + numOfHours + "h";
            }
            if (numOfMinutes > 0) {
                message += " " + numOfMinutes + "min";
            }
            message += " to maximise your productivity.";
        }
        return message;
    }
}