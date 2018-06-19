package hu.am2.myway.ui.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import hu.am2.myway.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_preference);
    }
}
