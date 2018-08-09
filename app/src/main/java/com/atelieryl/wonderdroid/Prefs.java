
package com.atelieryl.wonderdroid;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.layout.prefs);

        EditTextPreference override = (EditTextPreference)findPreference("emu_overriderompath");
        //getActionBar().setIcon(android.R.color.transparent);
    }
}
