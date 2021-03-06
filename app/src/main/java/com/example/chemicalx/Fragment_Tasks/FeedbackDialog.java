package com.example.chemicalx.Fragment_Tasks;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

import java.util.HashMap;

public class FeedbackDialog extends DialogFragment {
    //this is going to be main activity
    FeedbackDialogListener listener;
    HashMap<String, Object> currentTask;

    public FeedbackDialog(FeedbackDialogListener listener, HashMap<String, Object> currentTask) {
        this.listener = listener;
        this.currentTask = currentTask;
    }

    public interface FeedbackDialogListener {
        public void onFeedbackClick(DialogFragment dialog, int which, HashMap<String, Object> currentTask);
        public void onCancel(DialogFragment dialog, HashMap<String, Object> currentTask);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.feedback_dialog_question)
                .setItems(R.array.feedback_dialog, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        listener.onFeedbackClick(FeedbackDialog.this, which, currentTask);
                    }
                });
        return builder.create();

    }
//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        // Verify that the host activity implements the callback interface
//        try {
//            // Instantiate the NoticeDialogListener so we can send events to the host
//            listener = (FeedbackDialogListener) context;
//        } catch (ClassCastException e) {
//            // The activity doesn't implement the interface, throw exception
//            throw new ClassCastException(getActivity().toString()
//                    + " must implement NoticeDialogListener");
//        }
//    }
    @Override
    public void onCancel(DialogInterface dialog) {
        listener.onCancel(this, this.currentTask);
        super.onCancel(dialog);
    }
}

