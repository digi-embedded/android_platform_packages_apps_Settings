/*
 * Copyright 2018, Digi International Inc.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.android.settings.deviceinfo;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import com.digi.android.firmwareupdate.FirmwareUpdateManager;
import com.digi.android.firmwareupdate.IFirmwareUpdateListener;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

public class FirmwareUpdate extends Activity {

    // Constants.
    private static final String PARENT_DIR = "..";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String ROOT_DIR = "/";

    private static final int ACTION_UPDATE_PROGRESS_DIALOG_TEXT = 0;
    private static final int ACTION_DISMISS_PROGRESS_DIALOG = 1;
    private static final int ACTION_SHOW_TOAST = 2;

    // Variables.
    private ListView list;

    private TextView pathText;

    private File currentPath;

    private String[] fileList;

    private FirmwareUpdateManager firmwareUpdateManager;

    private ProgressDialog progressDialog;

    /**
     * Listener used to retrieve progress from the firmware update process.
     */
    private IFirmwareUpdateListener firmwareUpdateListener = new IFirmwareUpdateListener() {

        @Override
        public void verifyStarted() {
            // Do nothing.
        }

        @Override
        public void verifyProgress(int progress) {
            // Update dialog message.
            handler.sendMessage(handler.obtainMessage(ACTION_UPDATE_PROGRESS_DIALOG_TEXT,
                    String.format(getResources().getString(R.string.verifying_update_package), progress)));
        }

        @Override
        public void verifyFinished() {
            // Do nothing.
        }

        @Override
        public void updateStarted() {
            // Dismiss the progress dialog.
            handler.sendEmptyMessage(ACTION_DISMISS_PROGRESS_DIALOG);
        }

        @Override
        public void updatePackageCopyStarted() {
            // Update dialog message.
            handler.sendMessage(handler.obtainMessage(ACTION_UPDATE_PROGRESS_DIALOG_TEXT,
                    getResources().getString(R.string.copying_update_package)));
        }

        @Override
        public void updatePackageCopyFinished() {
            // Do nothing.
        }

        @Override
        public void onError(String error) {
            // Dismiss the progress dialog.
            handler.sendEmptyMessage(ACTION_DISMISS_PROGRESS_DIALOG);
            // Display an error message.
            handler.sendMessage(handler.obtainMessage(ACTION_SHOW_TOAST, error));
        }
    };

    /**
     * Handler used to execute UI actions from other threads.
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ACTION_UPDATE_PROGRESS_DIALOG_TEXT:
                    if (progressDialog != null)
                        progressDialog.setMessage((String)msg.obj);
                    break;
                case ACTION_DISMISS_PROGRESS_DIALOG:
                    if (progressDialog != null)
                        progressDialog.dismiss();
                    break;
                case ACTION_SHOW_TOAST:
                    Toast.makeText(FirmwareUpdate.this, (String)msg.obj, Toast.LENGTH_LONG).show();
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.firmware_update);

        // Instantiate the FirmwareUpdateManager manager object.
        firmwareUpdateManager = new FirmwareUpdateManager(this);

        // Initialize UI.
        initializeUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Display the contents of root directory.
        refresh(new File(ROOT_DIR));
    }

    /**
     * Initializes the user interface.
     */
    private void initializeUI() {
        // Look for the list item.
        list = (ListView)findViewById(R.id.fs_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                String fileChosen = (String) list.getItemAtPosition(which);
                File chosenFile = getChosenFile(fileChosen);
                if (chosenFile.isDirectory())
                    refresh(chosenFile);
                else
                    handleFileSelected(chosenFile);
            }
        });
        // Check for the path text.
        pathText = (TextView)findViewById(R.id.path);
    }

    /**
     * Sorts, filters and displays the files for the given path.
     *
     * @param path File path to display.
     */
    private void refresh(File path) {
        this.currentPath = path;
        if (path.exists()) {
            // Retrieve all readable directories.
            File[] dirs = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return (file.isDirectory() && file.canRead());
                }
            });

            // Retrieve all readable files matching the specified extension.
            File[] files = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory()) {
                        if (!file.canRead())
                            return false;
                        else
                            return file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
                    } else
                        return false;
                }
            });

            // Convert the results into an array.
            int i = 0;
            if (path.getParentFile() == null)
                fileList = new String[dirs.length + files.length];
            else {
                fileList = new String[dirs.length + files.length + 1];
                fileList[i++] = PARENT_DIR;
            }

            // Sort files and folders alphabetically.
            Arrays.sort(dirs);
            Arrays.sort(files);

            // Add first folders and then the files to the list.
            for (File dir : dirs)
                fileList[i++] = dir.getName();
            for (File file : files )
                fileList[i++] = file.getName();

            // Refresh the user interface by setting the resulting array.
            list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList) {
                @Override
                public View getView(int pos, View view, ViewGroup parent) {
                    // Inflate the view.
                    LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v;
                    if (view == null)
                        v = inflater.inflate(R.layout.file_system_item, null);
                    else
                        v = view;
                    // Get file name.
                    String fileName = fileList[pos];
                    // Get view widgets.
                    TextView name = (TextView)v.findViewById(R.id.name);
                    ImageView icon = (ImageView)v.findViewById(R.id.icon);
                    // Set widgets values.
                    name.setText(fileName);
                    name.setSingleLine(true);
                    if (fileName.endsWith(ZIP_EXTENSION))
                        icon.setImageResource(R.drawable.file);
                    else if (fileName.startsWith(PARENT_DIR))
                        icon.setImageResource(R.drawable.folder_up);
                    else
                        icon.setImageResource(R.drawable.folder);
                    // Return the inflated view.
                    return v;
                }
            });

            // Set the path text.
            pathText.setText(path.toString());
        }
    }

    /**
     * Converts a relative filename into an actual File object.
     *
     * @param fileChosen Selected file path as String.
     *
     * @return File object corresponding to the given path String.
     */
    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(PARENT_DIR))
            return currentPath.getParentFile();
        else
            return new File(currentPath, fileChosen);
    }

    /**
     * Handles what happens when a valid file is selected.
     *
     * @param chosenFile The selected file.
     */
    private void handleFileSelected(final File chosenFile) {
        // Show a confirmation dialog.
        new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.firmware_update_title))
            .setMessage(String.format(getResources().getString(R.string.confirm_firmware_update), chosenFile.toString()))
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    performFirmwareUpdate(chosenFile);
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .show();
    }

    /**
     * Performs the firmware update using the given file.
     *
     * @param chosenFile The selected file.
     */
    private void performFirmwareUpdate(File chosenFile) {
        // Create the progress dialog.
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getResources().getString(R.string.installing_firmware_title));
        progressDialog.setMessage(getResources().getString(R.string.installing_firmware_title));
        progressDialog.show();
        // Start firmware update.
        firmwareUpdateManager.installUpdatePackage(chosenFile.toString(), firmwareUpdateListener, false, null);
    }
}
