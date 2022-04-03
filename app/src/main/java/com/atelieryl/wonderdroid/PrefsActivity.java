
package com.atelieryl.wonderdroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class PrefsActivity extends PreferenceActivity {
    PreferenceActivity activity = this;
    boolean goBack = false;
    boolean mappingControls = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            goBack = savedInstanceState.getBoolean("goBack");
            mappingControls = savedInstanceState.getBoolean("mappingControls");
        } catch (Exception e) {

        }

        if (mappingControls && goBack) {
            this.addPreferencesFromResource(R.layout.prefs_map_controls);
        } else if (goBack) {
            this.addPreferencesFromResource(R.layout.prefs_size_position);
        } else {
            this.addPreferencesFromResource(R.layout.activity_prefs);
            findPreference("mapcontrols").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    goBack = true;
                    mappingControls = true;
                    activity.setPreferenceScreen(null);
                    activity.addPreferencesFromResource(R.layout.prefs_map_controls);
                    return true;
                }
            });

            findPreference("sizeposition").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    goBack = true;
                    activity.setPreferenceScreen(null);
                    activity.addPreferencesFromResource(R.layout.prefs_size_position);
                    return true;
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("goBack", goBack);
        savedInstanceState.putBoolean("mappingControls", mappingControls);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        goBack = savedInstanceState.getBoolean("goBack");
        mappingControls = savedInstanceState.getBoolean("mappingControls");
    }

    @Override
    public void onBackPressed() {
        if (goBack) {
            mappingControls = false;
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
        } else {
            finish();
        }
    }
}
