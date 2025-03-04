/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.onic.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.TextView;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.dao.FormsDao;
import org.odk.collect.onic.listeners.DiskSyncListener;
import org.odk.collect.onic.tasks.DiskSyncTask;
import org.odk.collect.onic.tasks.FormLoaderTask;
import org.odk.collect.onic.utilities.ApplicationConstants;
import org.odk.collect.onic.utilities.VersionHidingCursorAdapter;
import org.odk.collect.onic.provider.FormsProviderAPI;

import timber.log.Timber;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores the path to
 * selected form for use by {@link MainMenuActivity}.
 *
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FormChooserList extends FormListActivity implements DiskSyncListener, AdapterView.OnItemClickListener {
    private static final String FORM_CHOOSER_LIST_SORTING_ORDER = "formChooserListSortingOrder";

    private static final boolean EXIT = true;
    private static final String syncMsgKey = "syncmsgkey";

    private DiskSyncTask diskSyncTask;

    private String id_odk_module_form;

    //Dialog creado Jorge
    private static final int PROGRESS_DIALOG = 1;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // must be at the beginning of any activity that can be called from an external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        setContentView(R.layout.chooser_list_layout);
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.enter_data));

        //Recibe el intent extras de main menu y da a id_odk_module_form el valor del formulario seleccionado para filtrar y mostrar solo 1
        Bundle id_module_institucional_odk = this.getIntent().getExtras();
        if(id_module_institucional_odk!=null){
            id_odk_module_form = id_module_institucional_odk.getString("idProjectodk");
        }

        setupAdapter();

        if (savedInstanceState != null && savedInstanceState.containsKey(syncMsgKey)) {
            TextView tv = (TextView) findViewById(R.id.status_text);
            tv.setText((savedInstanceState.getString(syncMsgKey)).trim());

        }

        // DiskSyncTask checks the disk for any forms not already in the content provider
        // that is, put here by dragging and dropping onto the SDCard

        //diskSyncTask = (DiskSyncTask) getLastNonConfigurationInstance(); //nuevojorge
        diskSyncTask = (DiskSyncTask) getLastCustomNonConfigurationInstance(); //comentadojorge


        //diskSyncTask = null; //agregadoJorge
        if (diskSyncTask == null) { //comentadoJorge
            //Log.e("LLAMO A DISKSYNCTAS","LLAMO A DISKSYNC");
            Timber.i("Starting new disk sync task");
            diskSyncTask = new DiskSyncTask();
            diskSyncTask.setDiskSyncListener(this);
            diskSyncTask.execute((Void[]) null);

            showDialog(PROGRESS_DIALOG); //creadoJorge Mostrar dialogo
        } //comentadoJorge
        sortingOptions = new String[]{
                getString(R.string.sort_by_name_asc), getString(R.string.sort_by_name_desc),
                getString(R.string.sort_by_date_asc), getString(R.string.sort_by_date_desc),
        };


    }


    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // pass the thread on restart
        return diskSyncTask;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView tv = (TextView) findViewById(R.id.status_text);
        outState.putString(syncMsgKey, tv.getText().toString().trim());
    }


    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // get uri to form
        long idFormsTable = listView.getAdapter().getItemId(position);
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, idFormsTable);

        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick",
                formUri.toString());

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)) {
            // caller is waiting on a picked form
            setResult(RESULT_OK, new Intent().setData(formUri));
        } else {
            // caller wants to view/edit a form, so launch formentryactivity
            Intent intent = new Intent(Intent.ACTION_EDIT, formUri);
            intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
            startActivity(intent);
        }

        finish();
    }


    @Override
    protected void onResume() {
        //Log.e("EN onRESUME??---","RESUME");
        diskSyncTask.setDiskSyncListener(this);
        super.onResume();

        if (diskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncComplete(diskSyncTask.getStatusMessage());

        }
    }


    @Override
    protected void onPause() {
        diskSyncTask.setDiskSyncListener(null);
        super.onPause();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Collect.getInstance().getActivityLogger().logOnStart(this);
    }

    @Override
    protected void onStop() {
        Collect.getInstance().getActivityLogger().logOnStop(this);
        super.onStop();
    }


    /**
     * Called by DiskSyncTask when the task is finished
     */

    @Override
    public void syncComplete(String result) {
        Timber.i("Disk sync task complete");
        TextView tv = (TextView) findViewById(R.id.status_text);
        tv.setText(result.trim());
        dismissDialog(PROGRESS_DIALOG); //creadoJorge
    }

    private void setupAdapter() {


        String[] data = new String[]{
                FormsProviderAPI.FormsColumns.DISPLAY_NAME, FormsProviderAPI.FormsColumns.DISPLAY_SUBTEXT, FormsProviderAPI.FormsColumns.JR_VERSION
        };
        int[] view = new int[]{
                R.id.text1, R.id.text2, R.id.text3
        };
        //Log.e("PASO A GET CURSOR","EL ODK ID");
        //Log.e("SETUPADAPTER--",id_odk_module_form);
        listAdapter =
                new VersionHidingCursorAdapter(FormsProviderAPI.FormsColumns.JR_VERSION, this, R.layout.two_item, getCursor(), data, view);

        listView.setAdapter(listAdapter);

    }

    @Override
    protected String getSortingOrderKey() {
        return FORM_CHOOSER_LIST_SORTING_ORDER;
    }

    @Override
    protected void updateAdapter() {
        listAdapter.changeCursor(getCursor());
    }

    private Cursor getCursor() {
        return new FormsDao().getFormsCursor(id_odk_module_form, getSortingOrder());
       // Log.e("VALOR DEL ID ODK: ",id_odk_module_form);
        //return new FormsDao().getFormsCursor(getFilterText(), getSortingOrder());
        //Cursor formdao = new FormsDao().getFormsCursor(id_odk_module_form, getSortingOrder());
       // Log.e("CURSOR EN GET: ",formdao.toString());
        //creado jorge


    }

    /**
     * Creates a dialog with the given message. Will exit the activity when the user preses "ok" if
     * shouldExit is set to true.
     */
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {

        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger().logAction(this,
                                "createErrorDialog",
                                shouldExit ? "exitApplication" : "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        alertDialog.setCancelable(false);
        alertDialog.setButton(getString(R.string.ok), errorListener);
        alertDialog.show();
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                Timber.i("Creating PROGRESS_DIALOG");
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onCreateDialog.PROGRESS_DIALOG",
                                "show");
                progressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logInstanceAction(this,
                                                "onCreateDialog.PROGRESS_DIALOG", "cancel");
                                dialog.dismiss();
                                /*
                                formLoaderTask.setFormLoaderListener(null);
                                FormLoaderTask t = formLoaderTask;
                                formLoaderTask = null;
                                t.cancel(true);
                                t.destroy();
                                finish();*/

                                //diskSyncTask = null; //agregadoJorge
                                if (diskSyncTask != null) { //comentadoJorge
                                    //Log.e("LLAMO A DISKSYNCTAS","LLAMO A DISKSYNC");
                                    Timber.i("Starting new disk sync task");

                                    //diskSyncTask.setDiskSyncListener(null);
                                    //diskSyncTask.cancel(true);
                                    //diskSyncTask = null;
                                } //comentadoJorge
                            }
                        };
                progressDialog.setTitle("Buscando formulario");
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setButton(getString(R.string.cancel_loading_form),
                        loadingButtonListener);
                return progressDialog;
                /*
            case SAVING_DIALOG:
                Timber.i("Creating SAVING_DIALOG");
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "onCreateDialog.SAVING_DIALOG",
                                "show");
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle(getString(R.string.saving_form));
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.setIndeterminate(true);
                progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Collect.getInstance()
                                .getActivityLogger()
                                .logInstanceAction(this,
                                        "onCreateDialog.SAVING_DIALOG", "OnDismissListener");
                        cancelSaveToDiskTask();
                    }
                });
                return progressDialog;
                */


        }
        return null;
    }
}
