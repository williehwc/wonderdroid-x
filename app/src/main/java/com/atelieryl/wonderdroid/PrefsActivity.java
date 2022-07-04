
package com.atelieryl.wonderdroid;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.atelieryl.wonderdroid.utils.RomFilter;

import java.io.File;

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

            findPreference("changedrive").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getApplicationContext(), OnboardingActivity.class);
                    intent.putExtra("changedrive", true);
                    startActivity(intent);
                    return true;
                }
            });

            findPreference("downloadboxart").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = PrefsActivity.this;
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    Boolean newValueBoolean = (Boolean) newValue;
                    if (!newValueBoolean) {
                        // Off
                        AlertDialog.Builder builder;
                        builder = new AlertDialog.Builder(context);
                        builder.setMessage(R.string.deleteboxart)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String storagePath = prefs.getString("storage_path", "");
                                    // Delete openvgdb
                                    File openvgdb = new File(storagePath + "/openvgdb.sqlite");
                                    openvgdb.delete();
                                    // Delete all box art
                                    File storageDir = new File(storagePath);
                                    File[] sourceFiles = storageDir.listFiles(new RomFilter(false, false, true));
                                    for (File sourceFile : sourceFiles) {
                                        sourceFile.delete();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Revert setting
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("downloadboxart", true);
                                    editor.commit();
                                    recreate();
                                }
                            })
                            .show();
                    } else {
                        // On
                        Intent intent = new Intent(context, AddGameActivity.class);
                        intent.putExtra("openvgdb", true);
                        context.startActivity(intent);
                    }
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
