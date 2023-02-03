package com.atelieryl.wonderdroid;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.widget.AbsListView.CHOICE_MODE_MULTIPLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.atelieryl.wonderdroid.utils.RomAdapter;
import com.atelieryl.wonderdroid.utils.ZipUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class FileMgmtActivity extends AppCompatActivity {

    private Context mContext;
    private SharedPreferences prefs;
    private String storagePath;
    private RomAdapter.Rom mRom;
    private ArrayList<String> mFiles;
    private ArrayList<String> mRomNames;
    private ArrayList<String> mHeaderInternalNames;
    private ListView fileList;
    private Button deleteButton;
    private Button renameGameButton;
    private Button selectAllButton;
    private Button selectNoneButton;
    private Button backUpButton;
    private Button cancelButton;
    private Button helpButton;
    private ProgressBar backupProgressBar;
    private TextView backupCurrentFile;
    private AsyncTask backupTask;

    final String OPENVGDB_FILENAME = "openvgdb.sqlite";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_mgmt);

        mContext = this;

        // Buttons
        deleteButton = findViewById(R.id.delete);
        renameGameButton = findViewById(R.id.renameGame);
        selectAllButton = findViewById(R.id.selectAll);
        selectNoneButton = findViewById(R.id.selectNone);
        backUpButton = findViewById(R.id.backUp);
        cancelButton = findViewById(R.id.fileMgmtCancel);
        helpButton = findViewById(R.id.help);

        // Intent extras
        try {
            mRom = (RomAdapter.Rom) this.getIntent().getExtras().getSerializable("rom");
            // Get all ROM names
            mRomNames = new ArrayList<>();
            mHeaderInternalNames = new ArrayList<>();
            if (mRom.sourcefile.getName().endsWith(".zip")) {
                try {
                    for (String entry : ZipUtils.getValidEntries(new ZipFile(mRom.sourcefile), RomAdapter.Rom.allRomExtensions)) {
                        mRomNames.add(entry);
                        if (isWsRom(mRom)) {
                            try {
                                WonderSwanHeader header = RomAdapter.Rom.getHeader(this, new RomAdapter.Rom(RomAdapter.Rom.Type.ZIP, mRom.sourcefile, entry, entry + " (ZIP)", mRom.sourcefile.lastModified()));
                                mHeaderInternalNames.add(header.internalname);
                            } catch (Exception ex) {}
                        }
                    }
                } catch (Exception ex) {}
            } else {
                mRomNames.add(mRom.fileName);
                if (isWsRom(mRom)) {
                    try {
                        WonderSwanHeader header = RomAdapter.Rom.getHeader(this, mRom);
                        mHeaderInternalNames.add(header.internalname);
                    } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {
            renameGameButton.setVisibility(View.GONE);
        }

        // Prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        storagePath = prefs.getString("storage_path", "");

        // Get files
        mFiles = new ArrayList<>();
        if (mRom != null) mFiles.add(mRom.sourcefile.getName());
        File dir = new File(storagePath);
        File[] files = dir.listFiles();
        long totalLength = 0;
        for (File file : files) {
            if (!file.isFile()) continue;
            String filename = file.getName();
            if (filename.equals(OPENVGDB_FILENAME)) continue;
            boolean includeFile = false;
            if (mRom != null) {
                for (String romName : mRomNames) {
                    if (file.getName().startsWith(romName + ".")) {
                        includeFile = true;
                        break;
                    }
                }
                // WonderDroid X states
                Pattern pattern = Pattern.compile("\\d+-\\d+-\\d+_a?\\d_\\d.sav");
                if (pattern.matcher(filename).matches()) {
                    for (String headerInternalName : mHeaderInternalNames) {
                        if (filename.startsWith(headerInternalName + "_")) {
                            includeFile = true;
                            break;
                        }
                    }
                }
                // WonderDroid X backups
                for (String headerInternalName : mHeaderInternalNames) {
                    if (filename.equals(headerInternalName + ".mem")) {
                        includeFile = true;
                        break;
                    }
                }
            } else {
                includeFile = true;
            }
            if (includeFile) {
                mFiles.add(file.getName());
                totalLength += file.length();
            }
        }

        // Show total file size
        double totalLengthMB = Math.round(((double) totalLength / 1000000) * 100.0) / 100.0;
        Toast.makeText(this, getString(R.string.total) + " " + totalLengthMB + " MB", Toast.LENGTH_SHORT).show();

        // ListView
        fileList = findViewById(R.id.fileList);
        fileList.setChoiceMode(CHOICE_MODE_MULTIPLE);
        fileList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, mFiles));

        // Backup progress
        backupProgressBar = findViewById(R.id.fileMgmtProgressBar);
        backupCurrentFile = findViewById(R.id.fileMgmtCurrentFile);

        // Listeners
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noFilesSelected()) return;
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.delete_confirm)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SparseBooleanArray checked = fileList.getCheckedItemPositions();
                                for (int i = 0; i < mFiles.size(); i++) {
                                    if (checked.get(i)) {
                                        File file = new File(storagePath + "/" + mFiles.get(i));
                                        try {
                                            file.delete();
                                        } catch (Exception ex) {}
                                    }
                                }
                                Intent intent = new Intent(mContext, SelectActivity.class);
                                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);
                                Runtime.getRuntime().exit(0);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .show();
            }
        });
        renameGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAll(true);
            }
        });
        selectNoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAll(false);
            }
        });
        backUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noFilesSelected()) return;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 1);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backupTask.cancel(true);
                finish();
            }
        });
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRom != null) {
                    openURL("http://yearbooklabs.com/sd/backup.html");
                } else {
                    openURL("http://yearbooklabs.com/sd/backupall.html");
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        try {
            backupTask.cancel(true);
        } catch (Exception ex) {}
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                ArrayList<File> filesToBackUp = new ArrayList<>();
                SparseBooleanArray checked = fileList.getCheckedItemPositions();
                for (int i = 0; i < mFiles.size(); i++) {
                    if (checked.get(i)) {
                        File file = new File(storagePath + "/" + mFiles.get(i));
                        filesToBackUp.add(file);
                    }
                }
                Uri uri = resultData.getData();
                backupTask = new BackupTask(uri, filesToBackUp, false, new ProgressUpdater(), mContext).execute();
                findViewById(R.id.fileList).setVisibility(View.GONE);
                findViewById(R.id.toolbar).setVisibility(View.GONE);
                findViewById(R.id.fileMgmtBackup).setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean isWsRom(RomAdapter.Rom mRom) {
        boolean result = false;
        for (String extension : RomAdapter.Rom.wsRomExtensions) {
            if (mRom.displayName.endsWith("." + extension)) {
                result = true;
            }
        }
        return result;
    }

    private void selectAll(boolean selectAll) {
        for (int i = 0; i < mFiles.size(); i++) {
            fileList.setItemChecked(i, selectAll);
        }
    }

    private boolean noFilesSelected() {
        SparseBooleanArray checked = fileList.getCheckedItemPositions();
        for (int i = 0; i < mFiles.size(); i++) {
            if (checked.get(i)) {
                return false;
            }
        }
        Toast.makeText(this, R.string.no_files_selected, Toast.LENGTH_SHORT).show();
        return true;
    }

    private void openURL(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(url);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.cannotopenurl)
                    .setMessage(R.string.cannotopenurldescription)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();
        }
    }

    private class ProgressUpdater implements BackupTask.ProgressUpdater {

        @Override
        public void progressUpdate(String currentFile, int numFilesProcessed, int numFilesTotal) {
            backupCurrentFile.setText(currentFile);
            backupProgressBar.setProgress((int) ((float) numFilesProcessed / numFilesTotal * 100));
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
