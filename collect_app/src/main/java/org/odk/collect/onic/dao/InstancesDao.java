/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.onic.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.dto.Instance;
import org.odk.collect.onic.provider.InstanceProviderAPI;
import org.odk.collect.onic.provider.InstanceProvider;
import org.odk.collect.onic.utilities.ApplicationConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to encapsulate all access to the @link InstanceProvider#DATABASE_NAME
 * For more information about this pattern go to https://en.wikipedia.org/wiki/Data_access_object
 */
public class InstancesDao {

    public Cursor getSentInstancesCursor() {
        String selection = InstanceProviderAPI.InstanceColumns.STATUS + " =? ";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC";

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getSentInstancesCursor(CharSequence charSequence, String sortOrder) {
        Cursor cursor;
        if (charSequence.length() == 0) {
            cursor = getSentInstancesCursor(sortOrder);
        } else {
            String selection =
                    InstanceProviderAPI.InstanceColumns.STATUS + " =? and "
                            + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {
                    InstanceProviderAPI.STATUS_SUBMITTED,
                    "%" + charSequence + "%"};

            cursor = getInstancesCursor(null, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    public Cursor getSentInstancesCursor(String sortOrder) {
        String selection = InstanceProviderAPI.InstanceColumns.STATUS + " =? ";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getUnsentInstancesCursor() {
        String selection = InstanceProviderAPI.InstanceColumns.STATUS + " !=? ";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceProviderAPI.InstanceColumns.STATUS + " DESC, " + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC";

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getUnsentInstancesCursor(String sortOrder) {
        String selection = InstanceProviderAPI.InstanceColumns.STATUS + " !=? ";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getUnsentInstancesCursor(CharSequence charSequence, String sortOrder) {
        Cursor cursor;
        if (charSequence.length() == 0) {
            cursor = getUnsentInstancesCursor(sortOrder);
        } else {
            String selection =
                    InstanceProviderAPI.InstanceColumns.STATUS + " !=? and "
                            + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {
                    InstanceProviderAPI.STATUS_SUBMITTED,
                    "%" + charSequence + "%"};

            cursor = getInstancesCursor(null, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    public Cursor getSavedInstancesCursor(String sortOrder) {
        String selection = InstanceProviderAPI.InstanceColumns.DELETED_DATE + " IS NULL ";

        return getInstancesCursor(null, selection, null, sortOrder);
    }

    public Cursor getSavedInstancesCursor(CharSequence charSequence, String sortOrder) {
        Cursor cursor;
        if (charSequence.length() == 0) {
            cursor = getSavedInstancesCursor(sortOrder);
        } else {
            String selection =
                    InstanceProviderAPI.InstanceColumns.DELETED_DATE + " IS NULL and "
                            + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {"%" + charSequence + "%"};
            cursor = getInstancesCursor(null, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    public Cursor getFinalizedInstancesCursor() {
        String selection = InstanceProviderAPI.InstanceColumns.STATUS + "=? or " + InstanceProviderAPI.InstanceColumns.STATUS + "=?";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_COMPLETE, InstanceProviderAPI.STATUS_SUBMISSION_FAILED};
        String sortOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC";

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getFinalizedInstancesCursor(String sortOrder) {
        String selection = InstanceProviderAPI.InstanceColumns.STATUS + "=? or " + InstanceProviderAPI.InstanceColumns.STATUS + "=?";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_COMPLETE, InstanceProviderAPI.STATUS_SUBMISSION_FAILED};

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getFinalizedInstancesCursor(CharSequence charSequence, String sortOrder) {
        Cursor cursor;
        if (charSequence.length() == 0) {
            cursor = getFinalizedInstancesCursor(sortOrder);
        } else {
            String selection =
                    "(" + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                            + InstanceProviderAPI.InstanceColumns.STATUS + "=?) and "
                            + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {
                    InstanceProviderAPI.STATUS_COMPLETE,
                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED,
                    "%" + charSequence + "%"};

            cursor = getInstancesCursor(null, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    public Cursor getInstancesCursorForFilePath(String path) {
        String selection = InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] selectionArgs = {path};

        return getInstancesCursor(null, selection, selectionArgs, null);
    }

    public Cursor getAllCompletedUndeletedInstancesCursor() {
        String selection = InstanceProviderAPI.InstanceColumns.DELETED_DATE + " IS NULL and ("
                + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                + InstanceProviderAPI.InstanceColumns.STATUS + "=?)";

        String[] selectionArgs = {InstanceProviderAPI.STATUS_COMPLETE,
                InstanceProviderAPI.STATUS_SUBMISSION_FAILED,
                InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC";

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getAllCompletedUndeletedInstancesCursor(String sortOrder) {
        String selection = InstanceProviderAPI.InstanceColumns.DELETED_DATE + " IS NULL and ("
                + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                + InstanceProviderAPI.InstanceColumns.STATUS + "=?)";

        String[] selectionArgs = {InstanceProviderAPI.STATUS_COMPLETE,
                InstanceProviderAPI.STATUS_SUBMISSION_FAILED,
                InstanceProviderAPI.STATUS_SUBMITTED};

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getCompletedUndeletedInstancesCursor(CharSequence charSequence, String sortOrder) {
        Cursor cursor;
        if (charSequence.length() == 0) {
            cursor = getAllCompletedUndeletedInstancesCursor(sortOrder);
        } else {
            String selection = InstanceProviderAPI.InstanceColumns.DELETED_DATE + " IS NULL and ("
                    + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                    + InstanceProviderAPI.InstanceColumns.STATUS + "=? or "
                    + InstanceProviderAPI.InstanceColumns.STATUS + "=?) and "
                    + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " LIKE ?";

            String[] selectionArgs = {
                    InstanceProviderAPI.STATUS_COMPLETE,
                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED,
                    InstanceProviderAPI.STATUS_SUBMITTED,
                    "%" + charSequence + "%"};

            cursor = getInstancesCursor(null, selection, selectionArgs, sortOrder);
        }
        return cursor;
    }

    public Cursor getInstancesCursorForId(String id) {
        String selection = InstanceProviderAPI.InstanceColumns._ID + "=?";
        String[] selectionArgs = {id};

        return getInstancesCursor(null, selection, selectionArgs, null);
    }

    public Cursor getInstancesCursor(String selection, String[] selectionArgs) {
        return getInstancesCursor(null, selection, selectionArgs, null);
    }

    public Cursor getInstancesCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return Collect.getInstance().getContentResolver()
                .query(InstanceProviderAPI.InstanceColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }

    public Uri saveInstance(ContentValues values) {
        return Collect.getInstance().getContentResolver().insert(InstanceProviderAPI.InstanceColumns.CONTENT_URI, values);
    }

    public int updateInstance(ContentValues values, String where, String[] whereArgs) {
        return Collect.getInstance().getContentResolver().update(InstanceProviderAPI.InstanceColumns.CONTENT_URI, values, where, whereArgs);
    }

    public void deleteInstancesDatabase() {
        Collect.getInstance().getContentResolver().delete(InstanceProviderAPI.InstanceColumns.CONTENT_URI, null, null);
    }

    public void deleteInstancesFromIDs(List<String> ids) {
        int count = ids.size();
        int counter = 0;
        while (count > 0) {
            String[] selectionArgs = null;
            if (count > ApplicationConstants.SQLITE_MAX_VARIABLE_NUMBER) {
                selectionArgs = new String[
                        ApplicationConstants.SQLITE_MAX_VARIABLE_NUMBER];
            } else {
                selectionArgs = new String[count];
            }

            StringBuilder selection = new StringBuilder();
            selection.append(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + " IN (");
            int j = 0;
            while (j < selectionArgs.length) {
                selectionArgs[j] = ids.get(
                        counter * ApplicationConstants.SQLITE_MAX_VARIABLE_NUMBER + j);
                selection.append("?");

                if (j != selectionArgs.length - 1) {
                    selection.append(",");
                }
                j++;
            }
            counter++;
            count -= selectionArgs.length;
            selection.append(")");
            Collect.getInstance().getContentResolver()
                    .delete(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                            selection.toString(), selectionArgs);

        }
    }

    public List<Instance> getInstancesFromCursor(Cursor cursor) {
        List<Instance> instances = new ArrayList<>();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int displayNameColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME);
                    int submissionUriColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.SUBMISSION_URI);
                    int canEditWhenCompleteIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE);
                    int instanceFilePathIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH);
                    int jrFormIdColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_FORM_ID);
                    int jrVersionColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_VERSION);
                    int statusColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.STATUS);
                    int lastStatusChangeDateColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE);
                    int displaySubtextColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT);
                    int deletedDateColumnIndex = cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.DELETED_DATE);

                    Instance instance = new Instance.Builder()
                            .displayName(cursor.getString(displayNameColumnIndex))
                            .submissionUri(cursor.getString(submissionUriColumnIndex))
                            .canEditWhenComplete(cursor.getString(canEditWhenCompleteIndex))
                            .instanceFilePath(cursor.getString(instanceFilePathIndex))
                            .jrFormId(cursor.getString(jrFormIdColumnIndex))
                            .jrVersion(cursor.getString(jrVersionColumnIndex))
                            .status(cursor.getString(statusColumnIndex))
                            .lastStatusChangeDate(cursor.getLong(lastStatusChangeDateColumnIndex))
                            .displaySubtext(cursor.getString(displaySubtextColumnIndex))
                            .deletedDate(cursor.getLong(deletedDateColumnIndex))
                            .build();

                    instances.add(instance);
                }
            } finally {
                cursor.close();
            }
        }
        return instances;
    }

    public ContentValues getValuesFromInstanceObject(Instance instance) {
        ContentValues values = new ContentValues();
        values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME, instance.getDisplayName());
        values.put(InstanceProviderAPI.InstanceColumns.SUBMISSION_URI, instance.getSubmissionUri());
        values.put(InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE, instance.getCanEditWhenComplete());
        values.put(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH, instance.getInstanceFilePath());
        values.put(InstanceProviderAPI.InstanceColumns.JR_FORM_ID, instance.getJrFormId());
        values.put(InstanceProviderAPI.InstanceColumns.JR_VERSION, instance.getJrVersion());
        values.put(InstanceProviderAPI.InstanceColumns.STATUS, instance.getStatus());
        values.put(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE, instance.getLastStatusChangeDate());
        values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT, instance.getDisplaySubtext());
        values.put(InstanceProviderAPI.InstanceColumns.DELETED_DATE, instance.getDeletedDate());

        return values;
    }
}
