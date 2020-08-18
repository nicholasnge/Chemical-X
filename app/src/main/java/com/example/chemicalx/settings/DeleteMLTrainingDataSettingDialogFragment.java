package com.example.chemicalx.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

import java.io.File;
import java.util.Arrays;

public class DeleteMLTrainingDataSettingDialogFragment extends DialogFragment {
    private static final String PAST_EVENTS_DATA_FILENAME = "data/ml_training/past_events_data.json";
    private static final String DATA_POINTS_FILENAME = "data/ml_training/data_points.json";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete_ml_training_data_setting_dialog_title)
                .setMessage(R.string.delete_ml_training_data_setting_dialog_message)
                .setPositiveButton(R.string.delete_ml_training_data_setting_dialog_affirmative,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        deleteData();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        DeleteMLTrainingDataSettingDialogFragment.this.onCancel(dialog);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void deleteData() {
        for (String filename : Arrays.asList(PAST_EVENTS_DATA_FILENAME, DATA_POINTS_FILENAME)) {
            File pastEventsDataFile = new File(getContext().getFilesDir(), filename);
            pastEventsDataFile.delete();
        }
    }
}