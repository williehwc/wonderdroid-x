package com.atelieryl.wonderdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

public class AddGameActivity extends AppCompatActivity {

    private ActionBar actionBar;
    private ScrollView addGameChoices;
    private LinearLayout addGamePrefs;
    private Button selectFileButton;
    private Button selectFolderButton;
    private Button rescanFolderButton;
    private Button addGameCancelButton;
    private Button addGameCloseButton;
    private CheckBox includeSubfoldersCheckBox;
    private TextView scanFolderNoteRescan;
    private Switch overwriteSwitch;
    private Switch copyModeSwitch;
    private ListView listView;

    private SharedPreferences prefs;
    private boolean includeSubfolders;
    private boolean overwrite;
    private boolean copyMode;
    private String lastFolderUri;
    private String lastFolderPath;
    private boolean scanFolder;
    private Uri uri;
    ArrayList<DocumentFile> fileQueue;
    private ArrayList<String> fileQueueDisplay;
    private ArrayList<String> fileQueueDisplayUpdated;
    private ArrayAdapter adapter;
    private boolean scanning;
    private boolean canceled;
    private AsyncTask fileOpTask;
    private boolean upgrade;
    private boolean noBoxArt;

    final int BUFFER_SIZE = 1000;
    final String OPENVGDB_FILENAME = "openvgdb.sqlite";
    final int OPENVGDB_FILE_SIZE = 42300000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_game);

        // Views
        actionBar = getSupportActionBar();
        addGameChoices = findViewById(R.id.addGameChoices);
        addGamePrefs = findViewById(R.id.addGamePrefs);
        selectFileButton = findViewById(R.id.selectFileButton);
        selectFolderButton = findViewById(R.id.selectFolderButton);
        rescanFolderButton = findViewById(R.id.rescanFolderButton);
        addGameCancelButton = findViewById(R.id.addGameCancelButton);
        addGameCloseButton = findViewById(R.id.addGameCloseButton);
        includeSubfoldersCheckBox = findViewById(R.id.includeSubfoldersCheckBox);
        scanFolderNoteRescan = findViewById(R.id.scanFolderNoteRescan);
        overwriteSwitch = findViewById(R.id.overwriteSwitch);
        copyModeSwitch = findViewById(R.id.copyModeSwitch);
        listView = findViewById(R.id.addGameListView);

        // Prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        includeSubfolders = prefs.getBoolean("include_subfolders", true);
        overwrite = prefs.getBoolean("overwrite", false);
        copyMode = prefs.getBoolean("copy_mode", true);
        lastFolderUri = prefs.getString("last_folder_uri", null);
        if (lastFolderUri != null) {
            uri = Uri.parse(lastFolderUri);
            lastFolderPath = uri.getPath().split(":")[1];
        }
        noBoxArt = prefs.getBoolean("no_box_art", false);

        // Set views
        includeSubfoldersCheckBox.setChecked(includeSubfolders);
        overwriteSwitch.setChecked(overwrite);
        copyModeSwitch.setChecked(copyMode);
        if (lastFolderPath != null) {
            scanFolderNoteRescan.setText(getString(R.string.scan_folder_note_rescan) + " " + lastFolderPath);
            scanFolderNoteRescan.setVisibility(View.VISIBLE);
            rescanFolderButton.setVisibility(View.VISIBLE);
        }

        // Listeners
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });
        selectFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFolder(null);
            }
        });
        rescanFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanFolder = true;
                fileOrFolderSelected();
            }
        });
        includeSubfoldersCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeSubfolders = isChecked;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("include_subfolders", includeSubfolders);
                editor.commit();
            }
        });
        overwriteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                overwrite = isChecked;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("overwrite", overwrite);
                editor.commit();
            }
        });
        copyModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                copyMode = isChecked;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("copy_mode", copyMode);
                editor.commit();
            }
        });
        addGameCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileOpTask.cancel(true);
            }
        });
        addGameCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Intent extras (upgrading from WonderDroid X)
        Intent intent = getIntent();
        upgrade = intent.getBooleanExtra("upgrade", false);
        if (upgrade) {
            String romPath = null;
            File romdirx = ((WonderDroid)getApplication()).getRomDir();
            if (romdirx != null) {
                String sdpath = romdirx.getAbsolutePath();
                romPath = prefs.getString("emu_rompath", "wonderdroid");
                if (!romPath.startsWith("/")) {
                    romPath = sdpath + "/" + romPath;
                }
            }
            selectFolder(romPath);
        }

        // Set up ListView
        fileQueueDisplay = new ArrayList<>();
        fileQueueDisplayUpdated = new ArrayList<>();
        adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_2, android.R.id.text1, fileQueueDisplayUpdated) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                }

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.WHITE);

                try {
                    String[] displayTextSplit = fileQueueDisplayUpdated.get(position).split("\0");

                    if (displayTextSplit[1].equals(OPENVGDB_FILENAME))
                        text1.setText(getString(R.string.openvgdb));
                    else
                        text1.setText(displayTextSplit[1]);

                    switch (displayTextSplit[0]) {
                        case "i":
                            text2.setText(getString(R.string.in_progress) + " " + displayTextSplit[2]);
                            break;
                        case "p":
                            text2.setText(getString(R.string.pending));
                            break;
                        case "e":
                            text2.setText(getString(R.string.file_exists));
                            break;
                        case "f":
                            text2.setText(getString(R.string.file_error));
                            break;
                        case "u":
                            text2.setText(getString(R.string.unrecognized_extension));
                            break;
                        case "n":
                            text2.setText(getString(R.string.no_corresponding_game));
                            break;
                        case "c":
                            text2.setText(getString(R.string.canceled));
                            break;
                    }
                } catch (Exception e) {}

                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] displayTextSplit = fileQueueDisplayUpdated.get(position).split("\0");
                if (!displayTextSplit[0].equals("f")) return;
                Toast.makeText(getApplicationContext(), displayTextSplit[2], Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, 0);
    }

    public void selectFolder(String romPath) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (romPath != null)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, romPath);
        startActivityForResult(intent, 1);
    }

    private void fileOrFolderSelected() {
        if (scanFolder) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_folder_uri", uri.toString());
            editor.commit();
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        addGameChoices.setVisibility(View.GONE);
        addGamePrefs.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        fileOpTask = new FileOpTask().execute();
        addGameCancelButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                uri = resultData.getData();
                scanFolder = (requestCode == 1);
                fileOrFolderSelected();
            }
        }
    }

    public class FileOpTask extends AsyncTask<Void, Void, Void> {
        private Void setCanceled() {
            canceled = true;
            publishProgress();
            return null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Scan for files
            scanning = true;
            publishProgress();
            fileQueue = new ArrayList<>();
            if (scanFolder) {
                // Scan folder
                DocumentFile documentFile = DocumentFile.fromTreeUri(getApplicationContext(), uri);
                ArrayList<DocumentFile> dirQueue = new ArrayList<>();
                dirQueue.add(documentFile);
                if (includeSubfolders) {
                    for (DocumentFile file : documentFile.listFiles()) {
                        if (isCancelled()) return setCanceled();
                        if (file.isDirectory())
                            dirQueue.add(file);
                    }
                }
                for (DocumentFile dir : dirQueue) {
                    for (DocumentFile file : dir.listFiles()) {
                        if (isCancelled()) return setCanceled();
                        if (!file.isDirectory()) {
                            fileQueue.add(file);
                            fileQueueDisplay.add("p\0" + file.getName());
                        }
                    }
                }
            } else {
                // Single file
                DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), uri);
                fileQueue.add(documentFile);
                fileQueueDisplay.add("p\0" + documentFile.getName());
            }
            scanning = false;
            publishProgress();
            // Get destination path
            String storagePath = prefs.getString("storage_path", "");
            // Check for OpenVGDB
            if (!noBoxArt) {
                File openVGDBFile = new File(storagePath + "/" + OPENVGDB_FILENAME);
                if (!openVGDBFile.exists()) {
                    fileQueue.add(0, null);
                    fileQueueDisplay.add(0, "p\0" + OPENVGDB_FILENAME);
                }
            }
            // Perform file operations
            for (int i = 0; i < fileQueue.size(); i++) {
                if (isCancelled()) return setCanceled();
                DocumentFile srcFile = fileQueue.get(i);
                String filename;
                if (srcFile != null) {
                    filename = srcFile.getName();
                } else {
                    filename = OPENVGDB_FILENAME;
                }
                // Ref: https://stackoverflow.com/questions/36023334
                try {
                    File outFile = new File(storagePath + "/" + filename);
                    // check if file exists
                    if (!overwrite && outFile.exists()) {
                        fileQueueDisplay.set(i, "e\0" + filename);
                    } else {
                        // copy output file
                        OutputStream out = getApplication().getContentResolver().openOutputStream(Uri.fromFile(outFile));
                        InputStream in;
                        long fileSize;
                        if (srcFile != null) {
                            in = getApplication().getContentResolver().openInputStream(srcFile.getUri());
                            fileSize = srcFile.length();
                        } else {
                            in = getApplicationContext().getAssets().open(OPENVGDB_FILENAME);
                            fileSize = OPENVGDB_FILE_SIZE;
                        }
                        long transferred = 0;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            if (isCancelled()) {
                                out.flush();
                                out.close();
                                outFile.delete();
                                return setCanceled();
                            }
                            out.write(buffer, 0, read);
                            transferred += BUFFER_SIZE;
                            fileQueueDisplay.set(i, "i\0" + filename + "\0" + (int) (((double) transferred / fileSize) * 100) + "%");
                            publishProgress();
                        }
                        in.close();
                        // write output file
                        out.flush();
                        out.close();
                        fileQueue.remove(i);
                        fileQueueDisplay.remove(i);
                        // delete original file
                        if (!copyMode && srcFile != null) {
                            srcFile.delete();
                        }
                        i--;
                    }
                } catch (Exception e) {
                    fileQueueDisplay.set(i, "f\0" + filename + "\0" + e.getMessage());
                }
                publishProgress();
            }
            return null;
        }
        @Override
        protected void onCancelled(Void result) {
            addGameCancelButton.setVisibility(View.GONE);
            addGameCloseButton.setVisibility(View.VISIBLE);
            for (int i = 0; i < fileQueueDisplay.size(); i++) {
                DocumentFile srcFile = fileQueue.get(i);
                String filename = srcFile.getName();
                fileQueueDisplay.set(i, "c\0" + filename);
            }
            onProgressUpdate();
        }
        @Override
        protected void onPostExecute(Void result) {
            if (fileQueue.size() > 0) {
                actionBar.setTitle(R.string.errors);
                addGameCancelButton.setVisibility(View.GONE);
                addGameCloseButton.setVisibility(View.VISIBLE);
            } else {
                if (upgrade) {
                    String romPath = prefs.getString("emu_rompath", "wonderdroid");
                    String memPath = prefs.getString("emu_mempath", "wonderdroid/cartmem");
                    if (!memPath.equals(romPath + "/cartmem")) {
                        Toast.makeText(getApplicationContext(), R.string.upgrade_save_warning, Toast.LENGTH_LONG).show();
                    }
                }
                finish();
            }
        }
        @Override
        protected void onProgressUpdate(Void... progress) {
            fileQueueDisplayUpdated.clear();
            fileQueueDisplayUpdated.addAll(fileQueueDisplay);
            adapter.notifyDataSetChanged();
            if (scanning)
                actionBar.setTitle(R.string.scanning);
            else {
                String title;
                if (isCancelled())
                    title = getString(R.string.canceled);
                else if (copyMode)
                    title = getString(R.string.copy);
                else
                    title = getString(R.string.move);
                actionBar.setTitle(title + " (" + fileQueueDisplay.size() + " " + getString(R.string.remaining) + ")");
            }
        }
    }

    // Syntax is status\0filename\0extra
    // i    In progress; percent as extra
    // p    Pending
    // e    File already exists and overwrite is disabled
    // f    File error; error as extra
    // u    Unrecognized file extension
    // z    File already exists in a ZIP
    // n    No corresponding game exists
    // c    Canceled
}