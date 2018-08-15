
package com.atelieryl.wonderdroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity {
    PreferenceActivity activity = this;
    boolean goBack = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.layout.prefs);

        EditTextPreference override = (EditTextPreference)findPreference("emu_overriderompath");
        //getActionBar().setIcon(android.R.color.transparent);

        findPreference("mapcontrols").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                goBack = true;
                activity.setPreferenceScreen(null);
                activity.addPreferencesFromResource(R.layout.mapcontrols);
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (goBack) {
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
        } else {
            finish();
        }
    }
}
