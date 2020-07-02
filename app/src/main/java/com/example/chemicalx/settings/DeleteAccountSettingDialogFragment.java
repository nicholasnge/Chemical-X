package com.example.chemicalx.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.R;

public class DeleteAccountSettingDialogFragment extends DialogFragment {
    private DeleteAccountSettingDialogListener listener;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DeleteAccountSettingDialogListener {
        public void onDeleteAccountSettingDialogDeleteClick(DialogFragment dialog, String password);
        public void onDeleteAccountSettingDialogCancelClick(DialogFragment dialog);
    }

    // Override the Fragment.onAttach() method to instantiate the DeleteAccountSettingDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DeleteAccountSettingDialogListener so we can send events to the host
            listener = (DeleteAccountSettingDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement DeleteAccountSettingDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        final View view = inflater
                .inflate(R.layout.fragment_dialog_setting_account_delete, null);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete_account_setting_dialog_title)
                .setView(view)
                .setMessage(R.string.delete_account_setting_dialog_message)
                .setPositiveButton(R.string.delete_account_setting_dialog_affirmative,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditText passwordEditText = view.findViewById(R.id.fragment_dialog_setting_account_delete_password_edittext);
                        String password = passwordEditText.getText().toString();
                        listener.onDeleteAccountSettingDialogDeleteClick(
                                DeleteAccountSettingDialogFragment.this, password);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDeleteAccountSettingDialogCancelClick(
                                DeleteAccountSettingDialogFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}