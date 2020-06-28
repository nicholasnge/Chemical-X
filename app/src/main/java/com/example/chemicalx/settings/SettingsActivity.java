package com.example.chemicalx.settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.chemicalx.FirebaseLoginActivity;
import com.example.chemicalx.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity
        implements DeleteAccountSettingDialogFragment.DeleteAccountSettingDialogListener {
    private static final String TAG = "SettingsActivity";
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsFragment = (SettingsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.settings_fragment);

        // toolbar
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void logout() {
        //sign out using firebase
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Intent intent = new Intent(SettingsActivity.this,
                                    FirebaseLoginActivity.class);
                            startActivity(intent);
                            finish(); // to prevent user from clicking back to this activity
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
                                                Toast.makeText(SettingsActivity.this,
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
        Toast.makeText(SettingsActivity.this, "Invalid password.",
                Toast.LENGTH_SHORT).show();

        DeleteAccountSettingDialogFragment deleteAccountSettingDialogFragment =
                new DeleteAccountSettingDialogFragment();
        deleteAccountSettingDialogFragment.show(getSupportFragmentManager(),
                "Delete Account Setting Dialog Fragment Retry");
    }

    @Override
    public void onDeleteAccountSettingDialogCancelClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }
}