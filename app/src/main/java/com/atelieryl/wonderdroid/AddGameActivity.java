package com.atelieryl.wonderdroid;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
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

import com.atelieryl.wonderdroid.utils.RomAdapter;
import com.atelieryl.wonderdroid.utils.RomFilter;
import com.atelieryl.wonderdroid.utils.ZipUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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
//    private Switch overwriteSwitch;
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
    private boolean noMatchingExt;
    private String storagePath;
    private String precheckError;
    private boolean openvgdbOnly;
    
    private Context mContext;

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
//        overwriteSwitch = findViewById(R.id.overwriteSwitch);
        copyModeSwitch = findViewById(R.id.copyModeSwitch);
        listView = findViewById(R.id.addGameListView);

        // Prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        includeSubfolders = prefs.getBoolean("include_subfolders", true);
        overwrite = prefs.getBoolean("overwrite", false);
        copyMode = prefs.getBoolean("copy_mode", true);
        lastFolderUri = prefs.getString("last_folder_uri", null);
        try {
            if (lastFolderUri != null) {
                uri = Uri.parse(lastFolderUri);
                lastFolderPath = uri.getPath().split(":")[1];
            }
        } catch (Exception e) {}
        noBoxArt = !prefs.getBoolean("downloadboxart", true);
        storagePath = prefs.getString("storage_path", "");

        // Set views
        includeSubfoldersCheckBox.setChecked(includeSubfolders);
//        overwriteSwitch.setChecked(overwrite);
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
//        overwriteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                overwrite = isChecked;
//                SharedPreferences.Editor editor = prefs.edit();
//                editor.putBoolean("overwrite", overwrite);
//                editor.commit();
//            }
//        });
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
                Intent intent = new Intent(mContext, SelectActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                Runtime.getRuntime().exit(0);
            }
        });

        // Context
        mContext = getApplicationContext();

        // Intent extras (upgrading from WonderDroid X)
        Intent intent = getIntent();
        upgrade = intent.getBooleanExtra("upgrade", false);
        openvgdbOnly = intent.getBooleanExtra("openvgdb", false);
        if (upgrade) {
            String romPathInternal = null;
            String romPath = prefs.getString("emu_rompath", "wonderdroid");
            if (!romPath.startsWith("/")) {
                romPathInternal = romPath;
            }
            selectFolder(romPathInternal);
        } else if (openvgdbOnly) {
            startFileOpTask();
        }

        // Set up ListView
        fileQueueDisplay = new ArrayList<>();
        fileQueueDisplayUpdated = new ArrayList<>();
        adapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_2, android.R.id.text1, fileQueueDisplayUpdated) {
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
                Toast.makeText(mContext, displayTextSplit[2], Toast.LENGTH_SHORT).show();
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
        if (romPath != null) {
            try {
                String scheme = "content://com.android.externalstorage.documents/document/primary";
                romPath = romPath.replace("/", "%2F");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                    intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
                    Uri uri = intent.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI);
                    scheme = uri.toString();
                    scheme = scheme.replace("/root/", "/document/");
                }
                scheme += "%3A" + romPath;
                uri = Uri.parse(scheme);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                startActivityForResult(intent, 1);
            } catch (Exception e) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 1);
            }
        } else {
            startActivityForResult(intent, 1);
        }
    }

    private void fileOrFolderSelected() {
        if (scanFolder) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_folder_uri", uri.toString());
            editor.commit();
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startFileOpTask();
    }

    private void startFileOpTask() {
        addGameChoices.setVisibility(View.GONE);
        addGamePrefs.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        fileOpTask = new FileOpTask().execute();
        if (!openvgdbOnly)
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
        } else if (upgrade) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.putExtra("upgrade_prompt", true);
            startActivity(intent);
            finish();
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
            fileQueue = new ArrayList<>();
            if (!openvgdbOnly) {
                if (scanFolder) {
                    // Scan folder
                    DocumentFile documentFile = DocumentFile.fromTreeUri(mContext, uri);
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
                                if (precheckDocumentFile(file)) {
                                    fileQueue.add(file);
                                    fileQueueDisplay.add("p\0" + file.getName());
                                }
                            }
                        }
                    }
                    if (fileQueue.size() == 0) {
                        noMatchingExt = true;
                    }
                } else {
                    // Single file
                    DocumentFile documentFile = DocumentFile.fromSingleUri(mContext, uri);
                    if (precheckDocumentFile(documentFile)) {
                        fileQueue.add(documentFile);
                        fileQueueDisplay.add("p\0" + documentFile.getName());
                    } else {
                        noMatchingExt = true;
                    }
                }
            }
            scanning = false;
            publishProgress();
            // Check for OpenVGDB
            if (!noBoxArt) {
                File openVGDBFile = new File(storagePath + "/" + OPENVGDB_FILENAME);
                if (!openVGDBFile.isFile()) {
                    fileQueue.add(0, null);
                    fileQueueDisplay.add(0, "p\0" + OPENVGDB_FILENAME);
                }
            }
            // Sort files
            for (int i = 0; i < fileQueue.size(); i++) {
                DocumentFile documentFile = fileQueue.get(i);
                if (documentFile != null) {
                    String filename = documentFile.getName();
                    if (filename.endsWith(".sav") || filename.endsWith(".mem")) {
                        fileQueue.remove(i);
                        fileQueueDisplay.remove(i);
                        fileQueue.add(documentFile);
                        fileQueueDisplay.add("p\0" + filename);
                    }
                }
            }
            // Perform file operations
            for (int i = 0; i < fileQueue.size(); i++) {
                publishProgress();
                if (isCancelled()) return setCanceled();
                DocumentFile srcFile = fileQueue.get(i);
                String filename;
                if (srcFile != null) {
                    filename = srcFile.getName();
                    // Check file
                    char check = checkDocumentFile(srcFile);
                    if (check != 'v') {
                        fileQueueDisplay.set(i, check + "\0" + filename);
                        continue;
                    }
                } else {
                    filename = OPENVGDB_FILENAME;
                }
                // Ref: https://stackoverflow.com/questions/36023334
                try {
                    File outFile = new File(storagePath + "/" + filename);
                    // copy output file
                    OutputStream out = getApplication().getContentResolver().openOutputStream(Uri.fromFile(outFile));
                    InputStream in;
                    long fileSize;
                    if (srcFile != null) {
                        in = getApplication().getContentResolver().openInputStream(srcFile.getUri());
                        fileSize = srcFile.length();
                    } else {
                        in = mContext.getAssets().open(OPENVGDB_FILENAME);
                        fileSize = OPENVGDB_FILE_SIZE;
                    }
                    long transferred = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (isCancelled() && srcFile != null) {
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
                    if ((!copyMode || upgrade) && srcFile != null) {
                        srcFile.delete();
                    }
                    i--;
                } catch (Exception e) {
                    fileQueueDisplay.set(i, "f\0" + filename + "\0" + e.getMessage());
                }
            }
            publishProgress();
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
            if (noMatchingExt) {
                if (precheckError != null) {
                    Toast.makeText(mContext, precheckError, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.no_matching_ext, Toast.LENGTH_SHORT).show();
                }
            }
            if (fileQueue.size() > 0) {
                actionBar.setTitle(R.string.errors);
                addGameCancelButton.setVisibility(View.GONE);
                addGameCloseButton.setVisibility(View.VISIBLE);
            } else {
                if (upgrade) {
                    String romPath = prefs.getString("emu_rompath", "wonderdroid");
                    String memPath = prefs.getString("emu_mempath", "wonderdroid/cartmem");
                    if (!memPath.equals(romPath + "/cartmem")) {
                        Toast.makeText(mContext, R.string.upgrade_save_warning, Toast.LENGTH_LONG).show();
                    }
                }
                Intent intent = new Intent(mContext, SelectActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                Runtime.getRuntime().exit(0);
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
                else if (copyMode && !upgrade)
                    title = getString(R.string.copy);
                else
                    title = getString(R.string.move);
                actionBar.setTitle(title + " (" + fileQueueDisplay.size() + " " + getString(R.string.remaining) + ")");
            }
        }
    }

    public boolean precheckDocumentFile(@NonNull DocumentFile documentFile) {
        String filename = documentFile.getName();
        if (filename == null) return false;
        if (filename.endsWith(".zip")) {
            // https://stackoverflow.com/questions/23869228
            try {
                InputStream inputStream = getContentResolver().openInputStream(documentFile.getUri());
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    for (String extension : RomAdapter.Rom.allRomExtensions) {
                        if (entryName.endsWith(extension)) return true;
                    }
                }
            } catch (Exception e) {
                precheckError = e.getMessage();
                return false;
            }
        }
        for (String extension : RomAdapter.Rom.allRomExtensions) {
            if (filename.endsWith(extension)) return true;
        }
        for (String extension : RomAdapter.Rom.stateExtensions) {
            if (filename.endsWith(extension)) return true;
        }
        for (String extension : RomAdapter.Rom.boxArtExtensions) {
            if (filename.endsWith(extension)) return true;
        }
        return filename.endsWith(".sav") || filename.endsWith(".mem");
    }

    public char fileExists(String filename, String currentZipName) {
        // v: file does not already exist
        // e: file exists
        // z: file exists in ZIP
        // Check if the file itself exists
        if (!overwrite) {
            File outFile = new File(storagePath + "/" + filename);
            if (outFile.exists()) {
                return 'e';
            }
        }
        // If filename is not a ZIP, check all ZIPs except currentZipName
        if (!filename.endsWith(".zip")) {
            File mRomDir = new File(storagePath);
            File[] sourceFiles = mRomDir.listFiles(new RomFilter(false, false, false));
            if (sourceFiles != null) {
                for (File sourceFile : sourceFiles) {
                    if (currentZipName == null || !sourceFile.getName().equals(currentZipName)) {
                        try {
                            for (String entry : ZipUtils.getValidEntries(new ZipFile(sourceFile), RomAdapter.Rom.allRomExtensions)) {
                                if (entry.equals(filename)) {
                                    return 'z';
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                }
            }
        }
        return 'v';
    }

    public char checkDocumentFile(@NonNull DocumentFile documentFile) {
        // v: okay
        // e: file exists
        // z: file exists in ZIP
        // n: no corresponding game
        String filename = documentFile.getName();
        // Check if file exists
        char exists = fileExists(filename, null);
        if (exists != 'v') return exists;
        if (filename.endsWith(".zip")) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(documentFile.getUri());
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    exists = fileExists(entryName, filename);
                    if (exists != 'v') return exists;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (filename.endsWith(".sav")) {
            // Mednafen backup save or WonderDroid X state
            Pattern pattern = Pattern.compile("\\d+-\\d+-\\d+_a?\\d_\\d.sav");
            if (!pattern.matcher(filename).matches() && fileExists(filename.substring(0, filename.length() - 4), null) == 'v') {
                return 'n';
            }
        } else if (filename.endsWith(".mem")) {
            // WonderDroid X backup save
            Pattern pattern = Pattern.compile("\\d+-\\d+-\\d+.mem");
            if (!pattern.matcher(filename).matches()) {
                return 'n';
            }
        } else {
            // Mednafen state?
            for (String extension : RomAdapter.Rom.stateExtensions) {
                if (filename.endsWith(extension) && fileExists(filename.substring(0, filename.length() - 4), null) == 'v')
                    return 'n';
            }
        }
        return 'v';
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