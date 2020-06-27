package com.example.chemicalx.settings;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.chemicalx.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference deleteAccount = getPreferenceManager().findPreference("delete_account");
        deleteAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DeleteAccountSettingDialogFragment deleteAccountSettingDialogFragment =
                        new DeleteAccountSettingDialogFragment();

                deleteAccountSettingDialogFragment.show(getParentFragmentManager(),
                        "Delete Account Setting Dialog Fragment");

                return false;
            }
        });
    }
}