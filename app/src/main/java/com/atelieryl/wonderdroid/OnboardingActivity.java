package com.atelieryl.wonderdroid;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class OnboardingActivity extends AppCompatActivity {

    private TextView textView;
    private ListView listView;
    private Button nextButton;
    private Button refreshButton;
    private ImageView heroImageView;
    private CheckBox noBoxArtCheckBox;

    private SharedPreferences prefs;
    private String storagePath;

    private boolean upgrade;

    private enum Steps {
        START,
        UPGRADE_WELCOME,
        PRIVACY,
        CHOOSE_DRIVE,
        UPGRADE_PROMPT
    }

    private Steps currentStep = Steps.START;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Views
        textView = findViewById(R.id.onboardingTextView);
        listView = findViewById(R.id.onboardingListView);
        nextButton = findViewById(R.id.onboardingNextButton);
        refreshButton = findViewById(R.id.onboardingRefreshButton);
        heroImageView = findViewById(R.id.heroImageView);
        noBoxArtCheckBox = findViewById(R.id.noBoxArtCheckBox);

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
        noBoxArtCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("no_box_art", isChecked);
                editor.commit();
            }
        });

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
                if (upgrade)
                    currentStep = Steps.UPGRADE_PROMPT;
                else
                    finish();
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
                currentStep = Steps.PRIVACY;
                break;
            case UPGRADE_PROMPT:
                currentStep = Steps.CHOOSE_DRIVE;
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
        listView.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
        textView.setText(R.string.select_drive_prompt);
        heroImageView.setImageDrawable(getResources().getDrawable(R.drawable.onboarding_drive));
        noBoxArtCheckBox.setVisibility(View.GONE);

        // Get storage path again
        storagePath = prefs.getString("storage_path", "");

        // Look up and display drives
        ArrayList<String> driveStoragePaths = new ArrayList<>();
        ArrayList<Float> driveFreeSpacesMB = new ArrayList<>();
        ArrayList<Float> driveTotalSpacesMB = new ArrayList<>();
        int currentDrive = -1;
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        for (int i = 0; i < externalStorageVolumes.length; i++) {
            driveStoragePaths.add(externalStorageVolumes[i].getPath());
            driveFreeSpacesMB.add(new Float(externalStorageVolumes[i].getUsableSpace() / 1000000));
            driveTotalSpacesMB.add(new Float(externalStorageVolumes[i].getTotalSpace() / 1000000));
        }
        ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_2, android.R.id.text1, driveStoragePaths) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                //View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.WHITE);

                String inUseString = "";
                if (storagePath.equals(driveStoragePaths.get(position)))
                    inUseString = " (" + getString(R.string.in_use) + ")";

                text1.setText(driveStoragePaths.get(position).replace("Android/data/" + getPackageName() + "/files", "…") + inUseString);
                text2.setText(driveFreeSpacesMB.get(position).toString() + " MB " + getString(R.string.free_total) + " " + driveTotalSpacesMB.get(position).toString() + " MB");

                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                storagePath = driveStoragePaths.get(position);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("storage_path", storagePath);
                editor.commit();
                renderNextStep();
            }
        });
    }

    private void load_upgrade_prompt_screen() {
        listView.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.GONE);
        textView.setText(getString(R.string.upgrade_prompt_1) + "\n\n" + getString(R.string.upgrade_prompt_2));
        heroImageView.setImageDrawable(getResources().getDrawable(R.drawable.onboarding_prompt));
        noBoxArtCheckBox.setVisibility(View.GONE);
    }
}