package com.example.chemicalx.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.chemicalx.FirebaseLoginActivity;
import com.example.chemicalx.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends PreferenceFragmentCompat
        implements DeleteAccountSettingDialogFragment.DeleteAccountSettingDialogListener {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference deleteAccount = getPreferenceManager().findPreference("delete_account");
        deleteAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DeleteAccountSettingDialogFragment deleteAccountSettingDialogFragment =
                        new DeleteAccountSettingDialogFragment(SettingsFragment.this);

                deleteAccountSettingDialogFragment.show(getParentFragmentManager(),
                        "Delete Account Setting Dialog Fragment");

                return false;
            }
        });

        Preference deleteMLTrainingData = getPreferenceManager()
                .findPreference("delete_ml_training_data");
        deleteMLTrainingData
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DeleteMLTrainingDataSettingDialogFragment deleteMLTrainingDataSettingDialogFragment =
                        new DeleteMLTrainingDataSettingDialogFragment();

                deleteMLTrainingDataSettingDialogFragment.show(getParentFragmentManager(),
                        "Delete ML Training Data Setting Dialog Fragment");

                return false;
            }
        });
    }

    private void logout() {
        //sign out using firebase
        AuthUI.getInstance().signOut(getContext())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Intent intent = new Intent(getActivity(),
                                    FirebaseLoginActivity.class);
                            startActivity(intent);
                            getActivity().finish(); // to prevent user from clicking back to this activity
                        } else {
                            Log.e(TAG, "onComplete: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onDeleteAccountSettingDialogDeleteClick(DialogFragment dialog, String password) {
        final String METHOD_TAG = TAG + " onDeleteAccountSettingDialogDeleteClick";
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (password == null || password.equals("")) {
            Log.d(METHOD_TAG, "Password null or empty.");
            onReauthenticationFailure();
            return;
        }

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(METHOD_TAG, "User re-authenticated.");
                            user.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(METHOD_TAG, "User account deleted.");
                                                Toast.makeText(getContext(),
                                                        "Account deleted.",
                                                        Toast.LENGTH_SHORT).show();
                                                logout();
                                            }
                                        }
                                    });
                        } else {
                            Log.d(METHOD_TAG, "User re-authentication failed.");
                            onReauthenticationFailure();
                        }
                    }
                });
    }

    private void onReauthenticationFailure() {
        Toast.makeText(getContext(), "Invalid password.",
                Toast.LENGTH_SHORT).show();

        DeleteAccountSettingDialogFragment deleteAccountSettingDialogFragment =
                new DeleteAccountSettingDialogFragment(this);
        deleteAccountSettingDialogFragment.show(getParentFragmentManager(),
                "Delete Account Setting Dialog Fragment Retry");
    }

    @Override
    public void onDeleteAccountSettingDialogCancelClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }
}