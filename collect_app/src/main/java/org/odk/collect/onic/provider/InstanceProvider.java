/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.odk.collect.onic.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.database.helpers.InstancesDatabaseHelper;
import org.odk.collect.onic.utilities.MediaUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

public class InstanceProvider extends ContentProvider {
    private static HashMap<String, String> sInstancesProjectionMap;

    private static final int INSTANCES = 1;
    private static final int INSTANCE_ID = 2;

    private static final UriMatcher sUriMatcher;

    private InstancesDatabaseHelper databaseHelper;

    private InstancesDatabaseHelper getDbHelper() {
        // wrapper to test and reset/set the dbHelper based upon the attachment state of the device.
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            databaseHelper = null;
            return null;
        }

        if (databaseHelper != null) {
            return databaseHelper;
        }
        databaseHelper = new InstancesDatabaseHelper();
        return databaseHelper;
    }

    @Override
    public boolean onCreate() {
        // must be at the beginning of any activity that can be called from an external intent
        InstancesDatabaseHelper h = getDbHelper();
        return h != null;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(InstancesDatabaseHelper.INSTANCES_TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case INSTANCES:
                qb.setProjectionMap(sInstancesProjectionMap);
                break;

            case INSTANCE_ID:
                qb.setProjectionMap(sInstancesProjectionMap);
                qb.appendWhere(InstanceProviderAPI.InstanceColumns._ID + "=" + uri.getPathSegments().get(1));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case INSTANCES:
                return InstanceProviderAPI.InstanceColumns.CONTENT_TYPE;

            case INSTANCE_ID:
                return InstanceProviderAPI.InstanceColumns.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != INSTANCES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = System.currentTimeMillis();

        // Make sure that the fields are all set
        if (!values.containsKey(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE)) {
            values.put(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE, now);
        }

        if (!values.containsKey(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT)) {
            Date today = new Date();
            String text = getDisplaySubtext(InstanceProviderAPI.STATUS_INCOMPLETE, today);
            values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT, text);
        }

        if (!values.containsKey(InstanceProviderAPI.InstanceColumns.STATUS)) {
            values.put(InstanceProviderAPI.InstanceColumns.STATUS, InstanceProviderAPI.STATUS_INCOMPLETE);
        }

        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        long rowId = db.insert(InstancesDatabaseHelper.INSTANCES_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri instanceUri = ContentUris.withAppendedId(InstanceProviderAPI.InstanceColumns.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(instanceUri, null);
            Collect.getInstance().getActivityLogger().logActionParam(this, "insert",
                    instanceUri.toString(), values.getAsString(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
            return instanceUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    private String getDisplaySubtext(String state, Date date) {
        if (state == null) {
            return new SimpleDateFormat(getContext().getString(R.string.added_on_date_at_time),
                    Locale.getDefault()).format(date);
        } else if (InstanceProviderAPI.STATUS_INCOMPLETE.equalsIgnoreCase(state)) {
            return new SimpleDateFormat(getContext().getString(R.string.saved_on_date_at_time),
                    Locale.getDefault()).format(date);
        } else if (InstanceProviderAPI.STATUS_COMPLETE.equalsIgnoreCase(state)) {
            return new SimpleDateFormat(getContext().getString(R.string.finalized_on_date_at_time),
                    Locale.getDefault()).format(date);
        } else if (InstanceProviderAPI.STATUS_SUBMITTED.equalsIgnoreCase(state)) {
            return new SimpleDateFormat(getContext().getString(R.string.sent_on_date_at_time),
                    Locale.getDefault()).format(date);
        } else if (InstanceProviderAPI.STATUS_SUBMISSION_FAILED.equalsIgnoreCase(state)) {
            return new SimpleDateFormat(
                    getContext().getString(R.string.sending_failed_on_date_at_time),
                    Locale.getDefault()).format(date);
        } else {
            return new SimpleDateFormat(getContext().getString(R.string.added_on_date_at_time),
                    Locale.getDefault()).format(date);
        }
    }

    private void deleteAllFilesInDirectory(File directory) {
        if (directory.exists()) {
            // do not delete the directory if it might be an
            // ODK Tables instance data directory. Let ODK Tables
            // manage the lifetimes of its filled-in form data
            // media attachments.
            if (directory.isDirectory() && !Collect.isODKTablesInstanceDataDirectory(directory)) {
                // delete any media entries for files in this directory...
                int images = MediaUtils.deleteImagesInFolderFromMediaProvider(directory);
                int audio = MediaUtils.deleteAudioInFolderFromMediaProvider(directory);
                int video = MediaUtils.deleteVideoInFolderFromMediaProvider(directory);

                Timber.i("removed from content providers: %d image files, %d audio files,"
                        + " and %d video files.", images, audio, video);

                // delete all the files in the directory
                File[] files = directory.listFiles();
                for (File f : files) {
                    // should make this recursive if we get worried about
                    // the media directory containing directories
                    f.delete();
                }
            }
            directory.delete();
        }
    }

    /**
     * This method removes the entry from the content provider, and also removes any associated
     * files.
     * files:  form.xml, [formmd5].formdef, formname-media {directory}
     */
    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case INSTANCES:
                Cursor del = null;
                try {
                    del = this.query(uri, null, where, whereArgs, null);
                    if (del != null && del.getCount() > 0) {
                        del.moveToFirst();
                        do {
                            String instanceFile = del.getString(
                                    del.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
                            Collect.getInstance().getActivityLogger().logAction(this, "delete",
                                    instanceFile);
                            File instanceDir = (new File(instanceFile)).getParentFile();
                            deleteAllFilesInDirectory(instanceDir);
                        } while (del.moveToNext());
                    }
                } finally {
                    if (del != null) {
                        del.close();
                    }
                }
                count = db.delete(InstancesDatabaseHelper.INSTANCES_TABLE_NAME, where, whereArgs);
                break;

            case INSTANCE_ID:
                String instanceId = uri.getPathSegments().get(1);

                Cursor c = null;
                String status = null;
                try {
                    c = this.query(uri, null, where, whereArgs, null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        status = c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.STATUS));
                        do {
                            String instanceFile = c.getString(
                                    c.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
                            Collect.getInstance().getActivityLogger().logAction(this, "delete",
                                    instanceFile);
                            File instanceDir = (new File(instanceFile)).getParentFile();
                            deleteAllFilesInDirectory(instanceDir);
                        } while (c.moveToNext());
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }

                //We are going to update the status, if the form is submitted
                //We will not delete the record in table but we will delete the file
                if (status != null && status.equals(InstanceProviderAPI.STATUS_SUBMITTED)) {
                    ContentValues cv = new ContentValues();
                    cv.put(InstanceProviderAPI.InstanceColumns.DELETED_DATE, System.currentTimeMillis());
                    count = Collect.getInstance().getContentResolver().update(uri, cv, null, null);
                } else {
                    count =
                            db.delete(InstancesDatabaseHelper.INSTANCES_TABLE_NAME,
                                    InstanceProviderAPI.InstanceColumns._ID + "=" + instanceId
                                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                                    whereArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();

        Long now = System.currentTimeMillis();

        // Make sure that the fields are all set
        if (!values.containsKey(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE)) {
            values.put(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE, now);
        }

        int count;
        String status;
        switch (sUriMatcher.match(uri)) {
            case INSTANCES:
                if (values.containsKey(InstanceProviderAPI.InstanceColumns.STATUS)) {
                    status = values.getAsString(InstanceProviderAPI.InstanceColumns.STATUS);

                    if (!values.containsKey(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT)) {
                        Date today = new Date();
                        String text = getDisplaySubtext(status, today);
                        values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT, text);
                    }
                }

                count = db.update(InstancesDatabaseHelper.INSTANCES_TABLE_NAME, values, where, whereArgs);
                break;

            case INSTANCE_ID:
                String instanceId = uri.getPathSegments().get(1);

                if (values.containsKey(InstanceProviderAPI.InstanceColumns.STATUS)) {
                    status = values.getAsString(InstanceProviderAPI.InstanceColumns.STATUS);

                    if (!values.containsKey(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT)) {
                        Date today = new Date();
                        String text = getDisplaySubtext(status, today);
                        values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT, text);
                    }
                }

                count =
                        db.update(InstancesDatabaseHelper.INSTANCES_TABLE_NAME, values,
                                InstanceProviderAPI.InstanceColumns._ID + "=" + instanceId
                                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                                whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(InstanceProviderAPI.AUTHORITY, "instances", INSTANCES);
        sUriMatcher.addURI(InstanceProviderAPI.AUTHORITY, "instances/#", INSTANCE_ID);

        sInstancesProjectionMap = new HashMap<>();
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns._ID, InstanceProviderAPI.InstanceColumns._ID);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME, InstanceProviderAPI.InstanceColumns.DISPLAY_NAME);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.SUBMISSION_URI, InstanceProviderAPI.InstanceColumns.SUBMISSION_URI);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE,
                InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH,
                InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.JR_FORM_ID, InstanceProviderAPI.InstanceColumns.JR_FORM_ID);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.JR_VERSION, InstanceProviderAPI.InstanceColumns.JR_VERSION);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.STATUS, InstanceProviderAPI.InstanceColumns.STATUS);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE,
                InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT,
                InstanceProviderAPI.InstanceColumns.DISPLAY_SUBTEXT);
        sInstancesProjectionMap.put(InstanceProviderAPI.InstanceColumns.DELETED_DATE, InstanceProviderAPI.InstanceColumns.DELETED_DATE);
    }
}
