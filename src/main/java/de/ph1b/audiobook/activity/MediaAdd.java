package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FileAdapter;
import de.ph1b.audiobook.helper.CommonTasks;
import de.ph1b.audiobook.helper.NaturalOrderComparator;


public class MediaAdd extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "MediaAdd";
    public static final String BOOK_PROPERTIES_DEFAULT_NAME = "defaultName";

    private ArrayList<File> fileList;
    private final LinkedList<String> link = new LinkedList<String>();
    public static final int AUDIO = 1;
    public static final int IMAGE = 2;
    public final static String FILES_AS_STRING = "filesAsString";
    private ListView fileListView;
    private FileAdapter adapter;

    private ActionMode actionMode;
    private ActionMode.Callback mActionModeCallback;

    private static ArrayList<File> endList;
    private ArrayList<File> dirAddList;


    private static ArrayList<String> audioTypes;

    private final ArrayList<String> dirs = new ArrayList<String>();


    public static final FileFilter filterShowAudioAndFolder = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden() && (pathname.isDirectory() || isAudio(pathname.getName()));
        }
    };


    private static boolean isAudio(String name) {
        for (String s : audioTypes)
            if (name.endsWith(s))
                return true;
        return false;
    }

    public static boolean isImage(String s) {
        return s.endsWith(".jpg") || s.endsWith(".png");
    }

    private ArrayList<String> genAudioTypes() {
        ArrayList<String> audioTypes = new ArrayList<String>();
        audioTypes.add(".3gp");
        audioTypes.add(".mp4");
        audioTypes.add(".m4a");
        audioTypes.add(".mp3");
        audioTypes.add(".mid");
        audioTypes.add(".xmf");
        audioTypes.add(".mxmf");
        audioTypes.add(".rtttl");
        audioTypes.add(".rtx");
        audioTypes.add(".ota");
        audioTypes.add(".imy");
        audioTypes.add(".ogg");
        audioTypes.add(".wav");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            audioTypes.add(".aac");
            audioTypes.add(".flac");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            audioTypes.add(".mkv");

        return audioTypes;
    }

    private void addPathToSpinner(String path) {
        if (!dirs.contains(path) && new File(path).isDirectory())
            dirs.add(path);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioTypes = genAudioTypes();

        setContentView(R.layout.activity_file_chooser);

        PreferenceManager.setDefaultValues(this, R.xml.preference_screen, false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner);

        addPathToSpinner("/storage/extSdCard");
        addPathToSpinner(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (BuildConfig.DEBUG)
            addPathToSpinner("/storage/sdcard0/Audiobooks");
        addPathToSpinner("/storage/emulated/0");

        if (dirs.size() > 1) {
            dirSpinner.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.sd_spinner_layout, dirs);
            spinnerAdapter.setDropDownViewResource(R.layout.sd_spinner_layout);
            dirSpinner.setAdapter(spinnerAdapter);
            dirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "onItemSelected for chooser was called!");
                    link.clear();
                    link.add(dirs.get(position));
                    populateList(dirs.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            dirSpinner.setVisibility(View.GONE);
        }

        if (dirs.size() > 0) {
            link.add(dirs.get(0)); //first element of file hierarchy
            populateList(dirs.get(0)); //Setting path to external storage directory to list it
        }
    }

    private void addMediaBundle(ArrayList<File> dirAddList) {
        Collections.sort(dirAddList, new NaturalOrderComparator<File>());
        CommonTasks.logD(TAG, "Sorted dirAddList :");
        for (File f : dirAddList) {
            CommonTasks.logD(TAG, f.getAbsolutePath());
        }

        ArrayList<File> files = dirsToFiles(filterShowAudioAndFolder, dirAddList, AUDIO);
        if (files.size() != 0) {
            String defaultName = dirAddList.get(0).getName();
            if (!dirAddList.get(0).isDirectory())
                defaultName = defaultName.substring(0, defaultName.length() - 4);

            ArrayList<String> dirAddAsString = new ArrayList<String>();
            for (File f : files) {
                dirAddAsString.add(f.getAbsolutePath());
            }

            Intent intent = new Intent(getApplicationContext(), BookAdd.class);
            intent.putExtra(BOOK_PROPERTIES_DEFAULT_NAME, defaultName);
            intent.putStringArrayListExtra(FILES_AS_STRING, dirAddAsString);
            startActivity(intent);

        } else {
            CharSequence text = getString(R.string.book_no_media);
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (link.getLast().equals(link.getFirst())) {
            Intent intent = new Intent(this, MediaView.class);
            startActivity(intent);
        }
        link.removeLast();
        String now = link.getLast();
        link.removeLast();
        populateList(now);
    }

    private void populateList(String path) {

        //finishing action mode on populating new folder
        if (actionMode != null) {
            actionMode.finish();
        }
        if (BuildConfig.DEBUG)
            Log.e(TAG, "Populate this folder: " + path);

        link.add(path);
        File f = new File(path);
        File[] files = f.listFiles(filterShowAudioAndFolder);
        fileList = new ArrayList<File>(Arrays.asList(files));

        //fileList = new ArrayList<File>();
        Collections.sort(fileList, new NaturalOrderComparator<File>());
        adapter = new FileAdapter(fileList, this);
        fileListView = (ListView) findViewById(R.id.listView1);
        fileListView.setAdapter(adapter);

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fileList.get(position).isDirectory()) {
                    populateList(fileList.get(position).getAbsolutePath());
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_media_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static ArrayList<File> dirsToFiles(FileFilter filter, ArrayList<File> dir, int choice) {
        endList = new ArrayList<File>();
        for (File f : dir) {
            if (choice == AUDIO && isAudio(f.getName())) {
                endList.add(f);
            } else if (choice == IMAGE && isImage(f.getName())) {
                endList.add(f);
            }
            addDirRec(f, filter);
        }
        return endList;
    }

    private static void addDirRec(File file, FileFilter filter) {
        ArrayList<File> returnList = new ArrayList<File>();
        if (file.isDirectory()) {
            File[] tempList = file.listFiles(filter);

            if (tempList != null) {
                Collections.sort(Arrays.asList(tempList), new NaturalOrderComparator<File>());
                for (File f : tempList) {
                    if (f.isDirectory()) {
                        addDirRec(f, filter);
                    }
                }
                for (File f : tempList) {
                    if (!f.isDirectory())
                        returnList.add(f);
                }
            }
        }
        Collections.sort(returnList, new NaturalOrderComparator<File>());
        for (File f : returnList)
            CommonTasks.logD(TAG, f.getAbsolutePath());
        endList.addAll(returnList);
    }

    @Override
    public void onResume() {
        super.onResume();

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CommonTasks.logD(TAG, String.valueOf(isChecked));
        fileListView.getPositionForView(buttonView);
    }

    public void checkStateChanged(ArrayList<File> dirAddList) {
        this.dirAddList = dirAddList;
        CommonTasks.logD(TAG, "checkStateChange was called");
        if (dirAddList.size() > 0 && mActionModeCallback == null) {
            CommonTasks.logD(TAG, "Starting new ActionMode");
            mActionModeCallback = new ActionMode.Callback() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.action_mode_mediaadd, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_add_badge:
                            for (File f : MediaAdd.this.dirAddList)
                                CommonTasks.logD(TAG, "Adding: " + f.getAbsolutePath());
                            addMediaBundle(MediaAdd.this.dirAddList);
                            mode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    if (adapter != null)
                        adapter.clearCheckBoxes();
                    mActionModeCallback = null;
                }
            };

            actionMode = startSupportActionMode(mActionModeCallback);
        } else if (dirAddList.size() == 0) {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }
}
