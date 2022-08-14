package com.atelieryl.wonderdroid;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.atelieryl.wonderdroid.utils.RomFilter;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class OnboardingActivity extends AppCompatActivity {

    private Context mContext;

    private TextView textView;
    private ListView listView;
    private Button nextButton;
    private Button refreshButton;
    private Button cancelButton;
    private ImageView heroImageView;
    private CheckBox noBoxArtCheckBox;
    private View migrateView;

    private SharedPreferences prefs;
    private String storagePath;
    private String oldStoragePath;

    private boolean upgrade;
    private boolean changeDrive;

    private ProgressBar migrateProgressBar;
    private TextView migrateCurrentFile;
    private AsyncTask backupTask;

    private enum Steps {
        START,
        UPGRADE_WELCOME,
        PRIVACY,
        CHOOSE_DRIVE,
        UPGRADE_PROMPT,
        MIGRATE
    }

    private Steps currentStep = Steps.START;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        mContext = this;

        // Views
        textView = findViewById(R.id.onboardingTextView);
        listView = findViewById(R.id.onboardingListView);
        nextButton = findViewById(R.id.onboardingNextButton);
        refreshButton = findViewById(R.id.onboardingRefreshButton);
        cancelButton = findViewById(R.id.onboardingCancelButton);
        heroImageView = findViewById(R.id.heroImageView);
        noBoxArtCheckBox = findViewById(R.id.noBoxArtCheckBox);
        migrateView = findViewById(R.id.migrateView);
        migrateProgressBar = findViewById(R.id.progressBar);
        migrateCurrentFile = findViewById(R.id.currentFile);

        // Prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        storagePath = prefs.getString("storage_path", "");

        // Listeners
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderNextStep();
            }
        });
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderCurrentStep();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backupTask.cancel(true);
                finish();
            }
        });
        noBoxArtCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("downloadboxart", !isChecked);
                editor.commit();
            }
        });

        // Intent extras (upgrading from WonderDroid X)
        Intent intent = getIntent();
        changeDrive = intent.getBooleanExtra("changedrive", false);
        if (changeDrive) currentStep = Steps.PRIVACY;

        // Check if user is upgrading from WonderDroid X.
        // Either storage permission is enabled or "setreversehorizontalorientation" is set, while storagePath is null
        boolean storagePermission = PackageManager.PERMISSION_GRANTED == ContextCompat
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean setreversehorizontalorientation = prefs.getBoolean("setreversehorizontalorientation", false);
        String romPath = prefs.getString("emu_rompath", "\0");
        upgrade = (storagePermission || setreversehorizontalorientation || !romPath.equals("\0")) && storagePath == "";
        renderNextStep();
    }

    @Override
    public void onBackPressed() {
        renderPrevStep();
    }

    public void renderNextStep() {
        switch (currentStep) {
            case START:
                if (upgrade)
                    currentStep = Steps.UPGRADE_WELCOME;
                else
                    currentStep = Steps.PRIVACY;
                break;
            case UPGRADE_WELCOME:
                currentStep = Steps.PRIVACY;
                break;
            case PRIVACY:
                currentStep = Steps.CHOOSE_DRIVE;
                break;
            case CHOOSE_DRIVE:
                if (changeDrive)
                    currentStep = Steps.MIGRATE;
                else if (upgrade)
                    currentStep = Steps.UPGRADE_PROMPT;
                else {
                    finish();
                    return;
                }
                break;
            case UPGRADE_PROMPT:
                Intent intent = new Intent(this, AddGameActivity.class);
                intent.putExtra("upgrade", true);
                startActivity(intent);
                finish();
                break;
        }
        renderCurrentStep();
    }

    private void renderPrevStep() {
        switch (currentStep) {
            case PRIVACY:
                if (upgrade)
                    currentStep = Steps.UPGRADE_WELCOME;
                break;
            case CHOOSE_DRIVE:
                if (!changeDrive)
                    currentStep = Steps.PRIVACY;
                else
                    finish();
                break;
            case UPGRADE_PROMPT:
                currentStep = Steps.CHOOSE_DRIVE;
                break;
            case MIGRATE:
                backupTask.cancel(true);
                finish();
                break;
        }
        renderCurrentStep();
    }

    public void renderCurrentStep() {
        switch (currentStep) {
            case UPGRADE_WELCOME:
                load_upgrade_welcome_screen();
                break;
            case PRIVACY:
                load_privacy_screen();
                break;
            case CHOOSE_DRIVE:
                load_choose_drive_screen();
                break;
            case UPGRADE_PROMPT:
                load_upgrade_prompt_screen();
                break;
            case MIGRATE:
                load_migrate_screen();
                break;
        }
    }

    private void load_upgrade_welcome_screen() {
        listView.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.GONE);
        textView.setText(R.string.upgrade_welcome);
        heroImageView.setImageDrawable(getResources().getDrawable(R.drawable.onboarding_upgrade));
        noBoxArtCheckBox.setVisibility(View.GONE);
    }

    private void load_privacy_screen() {
        listView.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.GONE);
        textView.setText(R.string.privacy);
        heroImageView.setImageDrawable(getResources().getDrawable(R.drawable.onboarding_privacy));
        noBoxArtCheckBox.setVisibility(View.VISIBLE);
    }

    private void load_choose_drive_screen() {
        SharedPreferences.Editor editor = prefs.edit();
        File[] externalStorageVolumes = ContextCompat.getExternalCacheDirs(getApplicationContext());
        storagePath = getFilesDir().getPath();
        if (externalStorageVolumes.length > 0 && externalStorageVolumes[0] != null && !externalStorageVolumes[0].getPath().equals("")) {
            storagePath = externalStorageVolumes[0].getPath();
        }
        editor.putString("storage_path", storagePath);
        editor.commit();
        renderNextStep();
//        listView.setVisibility(View.VISIBLE);
//        nextButton.setVisibility(View.GONE);
//        refreshButton.setVisibility(View.VISIBLE);
//        textView.setText(R.string.select_drive_prompt);
//        heroImageView.setImageDrawable(getResources().getDrawable(R.drawable.onboarding_drive));
//        noBoxArtCheckBox.setVisibility(View.GONE);
//
//        // Get storage path again
//        storagePath = prefs.getString("storage_path", "");
//
//        // Look up and display drives
//        ArrayList<String> driveStoragePaths = new ArrayList<>();
//        ArrayList<Float> driveFreeSpacesMB = new ArrayList<>();
//        ArrayList<Float> driveTotalSpacesMB = new ArrayList<>();
//        int currentDrive = -1;
//        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
//        for (int i = 0; i < externalStorageVolumes.length; i++) {
//            driveStoragePaths.add(externalStorageVolumes[i].getPath());
//            driveFreeSpacesMB.add(new Float(externalStorageVolumes[i].getUsableSpace() / 1000000));
//            driveTotalSpacesMB.add(new Float(externalStorageVolumes[i].getTotalSpace() / 1000000));
//        }
//        ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_2, android.R.id.text1, driveStoragePaths) {
//            @Override
//            public View getView(int position, View view, ViewGroup parent) {
//                if (view == null) {
//                    view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
//                }
//                //View view = super.getView(position, convertView, parent);
//                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
//                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
//
//                text1.setTextColor(Color.WHITE);
//                text2.setTextColor(Color.WHITE);
//
//                String inUseString = "";
//                if (storagePath.equals(driveStoragePaths.get(position)))
//                    inUseString = " (" + getString(R.string.in_use) + ")";
//
//                text1.setText(driveStoragePaths.get(position).replace("Android/data/" + getPackageName() + "/files", "…") + inUseString);
//                text2.setText(driveFreeSpacesMB.get(position).toString() + " MB " + getString(R.string.free_total) + " " + driveTotalSpacesMB.get(position).toString() + " MB");
//
//                return view;
//            }
//        };
//        listView.setAdapter(adapter);
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView parent, View view, int position, long id) {
//                oldStoragePath = storagePath;
//                storagePath = driveStoragePaths.get(position);
//                SharedPreferences.Editor editor = prefs.edit();
//                editor.putString("storage_path", storagePath);
//                editor.commit();
//                renderNextStep();
//            }
//        });
    }

    private void load_upgrade_prompt_screen() {
        listView.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.GONE);
        textView.setText(getString(R.string.upgrade_prompt_1) + "\n\n" + getString(R.string.upgrade_prompt_2));
        heroImageView.setImageDrawable(getResources().getDrawable(R.drawable.onboarding_prompt));
        noBoxArtCheckBox.setVisibility(View.GONE);
    }

    private void load_migrate_screen() {
        listView.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        migrateView.setVisibility(View.VISIBLE);

        if (!oldStoragePath.equals("") && !storagePath.equals("") && !oldStoragePath.equals(storagePath)) {
            ArrayList<File> filesToMigrate = new ArrayList<>();
            File dir = new File(oldStoragePath);
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.isFile()) continue;
                filesToMigrate.add(file);
            }
            backupTask = new BackupTask(Uri.parse(new File(storagePath).toString()), filesToMigrate, true, new ProgressUpdater(), mContext).execute();
            cancelButton.setVisibility(View.VISIBLE);
        } else {
            finish();
        }
    }

    private class ProgressUpdater implements BackupTask.ProgressUpdater {

        @Override
        public void progressUpdate(String currentFile, int numFilesProcessed, int numFilesTotal) {
            migrateCurrentFile.setText(currentFile);
            migrateProgressBar.setProgress((int) ((float) numFilesProcessed / numFilesTotal * 100));
        }

        @Override
        public void taskFinished(int numFailed) {
            if (numFailed > 0) {
                Toast.makeText(mContext, numFailed + " " + getString(R.string.not_backed_up), Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }
}