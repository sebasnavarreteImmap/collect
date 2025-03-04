/*
 * Copyright (C) 2017 University of Washington
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

package org.odk.collect.onic.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.odk.collect.onic.R;
import org.odk.collect.onic.dao.FormsDao;
import org.odk.collect.onic.listeners.DeleteFormsListener;
import org.odk.collect.onic.listeners.DiskSyncListener;
import org.odk.collect.onic.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.onic.tasks.DeleteFormsTask;
import org.odk.collect.onic.tasks.DiskSyncTask;
import org.odk.collect.onic.utilities.ToastUtils;
import org.odk.collect.onic.utilities.VersionHidingCursorAdapter;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Responsible for displaying and deleting all the valid forms in the forms
 * directory.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class FormManagerList extends FormListFragment implements DiskSyncListener,
        DeleteFormsListener, View.OnClickListener {
    private static final String FORM_MANAGER_LIST_SORTING_ORDER = "formManagerListSortingOrder";
    BackgroundTasks backgroundTasks; // handled across orientation changes
    private AlertDialog alertDialog;

    public static FormManagerList newInstance() {
        return new FormManagerList();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {

        deleteButton.setOnClickListener(this);
        toggleButton.setOnClickListener(this);

        setupAdapter();

        if (backgroundTasks == null) {
            backgroundTasks = new BackgroundTasks();
            backgroundTasks.diskSyncTask = new DiskSyncTask();
            backgroundTasks.diskSyncTask.setDiskSyncListener(this);
            backgroundTasks.diskSyncTask.execute((Void[]) null);
        }
        super.onViewCreated(rootView, savedInstanceState);
    }

    @Override
    public void onResume() {
        // hook up to receive completion events
        backgroundTasks.diskSyncTask.setDiskSyncListener(this);
        if (backgroundTasks.deleteFormsTask != null) {
            backgroundTasks.deleteFormsTask.setDeleteListener(this);
        }
        super.onResume();
        // async task may have completed while we were reorienting...
        if (backgroundTasks.diskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncComplete(backgroundTasks.diskSyncTask.getStatusMessage());
        }
        if (backgroundTasks.deleteFormsTask != null
                && backgroundTasks.deleteFormsTask.getStatus() == AsyncTask.Status.FINISHED) {
            deleteComplete(backgroundTasks.deleteFormsTask.getDeleteCount());
        }
    }

    @Override
    public void onPause() {
        backgroundTasks.diskSyncTask.setDiskSyncListener(null);
        if (backgroundTasks.deleteFormsTask != null) {
            backgroundTasks.deleteFormsTask.setDeleteListener(null);
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        super.onPause();
    }

    private void setupAdapter() {
        List<Long> checkedForms = new ArrayList<>();
        for (long a : getListView().getCheckedItemIds()) {
            checkedForms.add(a);
        }
        String[] data = new String[]{FormsColumns.DISPLAY_NAME, FormsColumns.DISPLAY_SUBTEXT, FormsColumns.JR_VERSION};
        int[] view = new int[]{R.id.text1, R.id.text2, R.id.text3};

        listAdapter = new VersionHidingCursorAdapter(
                FormsColumns.JR_VERSION, getActivity(),
                R.layout.two_item_multiple_choice, getCursor(), data, view);
        setListAdapter(listAdapter);
        checkPreviouslyCheckedItems();
    }

    @Override
    protected String getSortingOrderKey() {
        return FORM_MANAGER_LIST_SORTING_ORDER;
    }

    @Override
    protected void updateAdapter() {
        listAdapter.changeCursor(getCursor());
        super.updateAdapter();
    }

    private Cursor getCursor() {
        return new FormsDao().getFormsCursor(getFilterText(), getSortingOrder());
    }

    /**
     * Create the form delete dialog
     */
    private void createDeleteFormsDialog() {
        logger.logAction(this, "createDeleteFormsDialog", "show");
        alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle(getString(R.string.delete_file));
        alertDialog.setMessage(getString(R.string.delete_confirm,
                String.valueOf(getCheckedCount())));
        DialogInterface.OnClickListener dialogYesNoListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        switch (i) {
                            case DialogInterface.BUTTON_POSITIVE: // delete
                                logger.logAction(this, "createDeleteFormsDialog", "delete");
                                deleteSelectedForms();
                                if (getListView().getCount() == getCheckedCount()) {
                                    toggleButton.setEnabled(false);
                                }
                                break;
                            case DialogInterface.BUTTON_NEGATIVE: // do nothing
                                logger.logAction(this, "createDeleteFormsDialog", "cancel");
                                break;
                        }
                    }
                };
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.delete_yes),
                dialogYesNoListener);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.delete_no),
                dialogYesNoListener);
        alertDialog.show();
    }

    /**
     * Deletes the selected files.First from the database then from the file
     * system
     */
    private void deleteSelectedForms() {
        // only start if no other task is running
        if (backgroundTasks.deleteFormsTask == null) {
            backgroundTasks.deleteFormsTask = new DeleteFormsTask();
            backgroundTasks.deleteFormsTask
                    .setContentResolver(getActivity().getContentResolver());
            backgroundTasks.deleteFormsTask.setDeleteListener(this);
            backgroundTasks.deleteFormsTask.execute(getCheckedIdObjects());
        } else {
            ToastUtils.showLongToast(R.string.file_delete_in_progress);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long rowId) {
        super.onListItemClick(l, v, position, rowId);
    }

    @Override
    public void syncComplete(String result) {
        Timber.i("Disk scan complete");
        TextView tv = (TextView) rootView.findViewById(R.id.status_text);
        tv.setText(result);
    }

    @Override
    public void deleteComplete(int deletedForms) {
        Timber.i("Delete forms complete");
        logger.logAction(this, "deleteComplete", Integer.toString(deletedForms));
        final int toDeleteCount = backgroundTasks.deleteFormsTask.getToDeleteCount();

        if (deletedForms == toDeleteCount) {
            // all deletes were successful
            ToastUtils.showShortToast(getString(R.string.file_deleted_ok, String.valueOf(deletedForms)));
        } else {
            // had some failures
            Timber.e("Failed to delete %d forms", (toDeleteCount - deletedForms));
            ToastUtils.showLongToast(getString(R.string.file_deleted_error, String.valueOf(getCheckedCount()
                    - deletedForms), String.valueOf(getCheckedCount())));
        }
        backgroundTasks.deleteFormsTask = null;
        getListView().clearChoices(); // doesn't unset the checkboxes
        for (int i = 0; i < getListView().getCount(); ++i) {
            getListView().setItemChecked(i, false);
        }
        deleteButton.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.delete_button:
                logger.logAction(this, "deleteButton", Integer.toString(getCheckedCount()));

                if (areCheckedItems()) {
                    createDeleteFormsDialog();
                } else {
                    ToastUtils.showShortToast(R.string.noselect_error);
                }
                break;

            case R.id.toggle_button:
                ListView lv = getListView();
                boolean allChecked = toggleChecked(lv);
                toggleButtonLabel(toggleButton, getListView());
                deleteButton.setEnabled(allChecked);
                break;
        }
    }

    private static class BackgroundTasks {
        DiskSyncTask diskSyncTask = null;
        DeleteFormsTask deleteFormsTask = null;

        BackgroundTasks() {
        }
    }
}
