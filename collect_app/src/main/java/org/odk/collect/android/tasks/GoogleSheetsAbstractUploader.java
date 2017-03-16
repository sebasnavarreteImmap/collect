/*
 * Copyright (C) 2017 Nafundi
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

package org.odk.collect.android.tasks;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.Xml;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.exception.FormException;
import org.odk.collect.android.picasa.AlbumEntry;
import org.odk.collect.android.picasa.AlbumFeed;
import org.odk.collect.android.picasa.PhotoEntry;
import org.odk.collect.android.picasa.PicasaClient;
import org.odk.collect.android.picasa.PicasaUrl;
import org.odk.collect.android.picasa.UserFeed;
import org.odk.collect.android.preferences.PreferenceKeys;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author carlhartung (chartung@nafundi.com)
 */
public abstract class GoogleSheetsAbstractUploader<Params, Progress, Result> extends
        GoogleSheetsTask<Long, Integer, HashMap<String, String>> {

    protected static final String picasa_fail = "Picasa Error: ";
    protected static final String oauth_fail = "OAUTH Error: ";
    protected static final String form_fail = "Form Error: ";
    private final static String TAG = "GoogleSheetsUploadTask";
    // needed in case of rate limiting
    private static final int GOOGLE_SLEEP_TIME = 1000;
    protected HashMap<String, String> mResults;

    /**
     * @param selection
     * @param selectionArgs
     * @param token
     */
    protected void uploadInstances(String selection, String[] selectionArgs, String token) {

        Cursor c = null;
        try {
            c = new InstancesDao().getInstancesCursor(selection, selectionArgs);

            if (c.getCount() > 0) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    if (isCancelled()) {
                        return;
                    }
                    String instance = c.getString(c
                            .getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));
                    String id = c.getString(c.getColumnIndex(InstanceColumns._ID));
                    String jrformid = c.getString(c.getColumnIndex(InstanceColumns.JR_FORM_ID));
                    Uri toUpdate = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, id);
                    ContentValues cv = new ContentValues();

                    Cursor formcursor = new FormsDao().getFormsCursorForFormId(jrformid);
                    String md5 = null;
                    String formFilePath = null;
                    if (formcursor.getCount() > 0) {
                        formcursor.moveToFirst();
                        md5 = formcursor
                                .getString(formcursor.getColumnIndex(FormsColumns.MD5_HASH));
                        formFilePath = formcursor.getString(formcursor
                                .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                    }

                    if (md5 == null) {
                        // fail and exit
                        Log.e(TAG, "no md5");
                        return;
                    }

                    publishProgress(c.getPosition() + 1, c.getCount());
                    if (!uploadOneSubmission(id, instance, jrformid, token, formFilePath)) {
                        cv.put(InstanceColumns.STATUS,
                                InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                        Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                        return;
                    } else {
                        cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMITTED);
                        Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
                    }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private boolean uploadOneSubmission(String id, String instanceFilePath, String jrFormId,
                                        String token, String formFilePath) {
        // if the token is null fail immediately
        if (token == null) {
            mResults.put(id, oauth_fail + Collect.getInstance().getString(R.string.invalid_oauth));
            return false;
        }

        HashMap<String, String> answersToUpload = new HashMap<>();
        HashMap<String, String> photosToUpload = new HashMap<>();
        HashMap<String, PhotoEntry> uploadedPhotos = new HashMap<>();

        HttpTransport h = AndroidHttp.newCompatibleTransport();
        GoogleCredential gc = new GoogleCredential();
        gc.setAccessToken(token);

        PicasaClient client = new PicasaClient(h.createRequestFactory(gc));

        // get instance file
        File instanceFile = new File(instanceFilePath);

        // first check to see how many columns we have:
        ArrayList<String> columnNames = new ArrayList<String>();
        try {
            getColumns(formFilePath, columnNames);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            mResults.put(id, e2.getMessage());
            return false;
        } catch (XmlPullParserException e2) {
            e2.printStackTrace();
            mResults.put(id, e2.getMessage());
            return false;
        } catch (IOException e2) {
            e2.printStackTrace();
            mResults.put(id, e2.getMessage());
            return false;
        } catch (FormException e2) {
            e2.printStackTrace();
            mResults.put(id, e2.getMessage());
            return false;
        }

        if (columnNames.size() == 0) {
            mResults.put(id,
                    form_fail + "No columns found in the form to upload");
            return false;
        }

        if (columnNames.size() > 255) {
            mResults.put(id,
                    Collect.getInstance()
                            .getString(R.string.sheets_max_columns, String.valueOf(columnNames.size())));
            return false;
        }

        // make sure column names are legal
        for (String n : columnNames) {
            if (!isValidGoogleSheetsString(n)) {
                mResults.put(id,
                        Collect.getInstance().getString(R.string.google_sheets_invalid_column_form,
                                n));
                return false;
            }
        }

        // parses the instance file and populates the answers and photos
        // hashmaps.
        try {
            processInstanceXML(instanceFile, answersToUpload, photosToUpload);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        } catch (FormException e) {
            mResults.put(id,
                    form_fail + Collect.getInstance().getString(R.string.google_repeat_error));
            return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        }

        try {
            Thread.sleep(GOOGLE_SLEEP_TIME);
        } catch (InterruptedException e3) {
            e3.printStackTrace();
        }

        // make sure column names in submission are legal (may be different than form)
        for (String n : answersToUpload.keySet()) {
            if (!isValidGoogleSheetsString(n)) {
                mResults.put(id, Collect.getInstance().getString(
                        R.string.google_sheets_invalid_column_instance, n));
                return false;
            }
        }

        // if we have any photos to upload,
        // get the picasa album or create a new one
        // then upload the photos
        if (photosToUpload.size() > 0) {
            // First set up a picasa album to upload to:
            // maybe we should move this, because if we don't have any
            // photos we don't care...
            AlbumEntry albumToUse;
            try {
                albumToUse = getOrCreatePicasaAlbum(client, jrFormId);
            } catch (IOException e) {
                e.printStackTrace();
                GoogleAuthUtil.invalidateToken(Collect.getInstance(), token);
                mResults.put(id, picasa_fail + e.getMessage());
                return false;
            }

            try {
                uploadPhotosToPicasa(photosToUpload, uploadedPhotos, client, albumToUse,
                        instanceFile);
            } catch (IOException e1) {
                e1.printStackTrace();
                mResults.put(id, picasa_fail + e1.getMessage());
                return false;
            }
        }

        // All photos have been sent to picasa (if there were any)
        // now upload data to Google Sheet

        Cursor cursor = null;
        String urlString = null;
        try {
            // see if the submission element was defined in the form
            cursor = new InstancesDao().getInstancesCursorForId(id);

            if (cursor.getCount() > 0) {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    int subIdx = cursor.getColumnIndex(InstanceColumns.SUBMISSION_URI);
                    urlString = cursor.isNull(subIdx) ? null : cursor.getString(subIdx);

                    // if we didn't find one in the content provider,
                    // try to get from settings
                    if (urlString == null) {
                        SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(Collect.getInstance());
                        urlString = settings
                                .getString(PreferenceKeys.KEY_GOOGLE_SHEETS_URL, Collect
                                        .getInstance()
                                        .getString(R.string.default_google_sheets_url));
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // now parse the url string if we have one
        final String googleHeader = "docs.google.com/spreadsheets/d/";
        String spreadsheetId;
        if (urlString == null || urlString.length() < googleHeader.length()) {
            mResults.put(
                    id,
                    form_fail
                            + Collect.getInstance().getString(R.string.invalid_sheet_id,
                            urlString));
            return false;
        } else {
            int start = urlString.indexOf(googleHeader) + googleHeader.length();
            int end = urlString.indexOf("/", start);
            if (end == -1) {
                // if there wasn't a "/", just try to get the end
                end = urlString.length();
            }
            if (start == -1 || end == -1) {
                mResults.put(
                        id,
                        form_fail
                                + Collect.getInstance().getString(R.string.invalid_sheet_id,
                                urlString));
                return false;
            }
            spreadsheetId = urlString.substring(start, end);
        }

        Spreadsheet response = null;
        try {
            response = mService.spreadsheets()
                    .get(spreadsheetId)
                    .setIncludeGridData(false)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        }

        String spreadsheetName = response.getSheets().get(0).getProperties().getTitle();

        List<List<Object>> values;
        List headerFeed = null;

        try {
            values = getHeaderFeed(spreadsheetId, spreadsheetName);
            if (values == null || values.size() == 0) {
                mResults.put(id, form_fail + "No data found");
            } else {
                headerFeed = values.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        }

        // check the headers....
        boolean emptyheaders = true;

        // go through headers
        // if they're empty, resize and add
        if (headerFeed != null) {
            for (Object c : headerFeed) {
                if (c != null) {
                    emptyheaders = false;
                    break;
                }
            }
        }

        if (emptyheaders) {
            // if the headers were empty, resize the spreadsheet
            // and add the headers

            ArrayList<Object> list = new ArrayList<>();
            for (String column : columnNames)
                list.add(column);

            ArrayList<List<Object>> content = new ArrayList<>();
            content.add(list);
            ValueRange row = new ValueRange();
            row.setValues(content);


            /**
             *  Appends the data at the last row
             *
             *  append(spreadsheetId, range, ValueRange)
             *
             *  spreadsheetId   :   Unique sheet id
             *  range           :   A1 notation range. It specifies the range within which the
             *                      spreadsheet should be searched.
             *              hint   "Giving only sheetName in range searches in the complete sheet"
             *  ValueRange      :   Content that needs to be appended  (List<List<Object>>)
             *
             *  For more info   :   https://developers.google.com/sheets/api/reference/rest/
             */

            // Send the new row to the API for insertion.
            // write the headers
            try {
                mService.spreadsheets().values()
                        .append(spreadsheetId, spreadsheetName, row)
                        .setIncludeValuesInResponse(true)
                        .setValueInputOption("USER_ENTERED").execute();
            } catch (IOException e) {
                e.printStackTrace();
                mResults.put(id, form_fail + e.getMessage());
                return false;
            }
        }

        // we may have updated the feed, so get a new one
        // update the feed

        try {
            values = getHeaderFeed(spreadsheetId, spreadsheetName);
            if (values == null || values.size() == 0) {
                mResults.put(id, form_fail + "No data found");
                return false;
            } else {
                headerFeed = values.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        }

        //check if any column name is blank

        boolean hasEmptyColumn = false;
        for (Object column : headerFeed) {
            if (column.equals("")) {
                hasEmptyColumn = true;
                break;
            }
        }

        // replace blank column name with a single space

        if (hasEmptyColumn) {
            ArrayList<Object> list = new ArrayList<>();
            for (Object column : headerFeed) {
                if (column.equals("")) {
                    list.add(" ");
                } else {
                    list.add(column);
                }
            }

            ArrayList<List<Object>> content = new ArrayList<>();
            content.add(list);
            ValueRange row = new ValueRange();
            row.setValues(content);

            try {
                mService.spreadsheets().values()
                        .update(spreadsheetId, spreadsheetName + "!A1:1", row)
                        .setValueInputOption("USER_ENTERED").execute();
            } catch (IOException e) {
                e.printStackTrace();
                mResults.put(id, form_fail + e.getMessage());
                return false;
            }
        }

        // we may have updated the feed, so get a new one
        // update the feed

        try {
            values = getHeaderFeed(spreadsheetId, spreadsheetName);
            if (values == null || values.size() == 0) {
                mResults.put(id, form_fail + "No data found");
                return false;
            } else {
                headerFeed = values.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        }

        // first, get all the columns in the spreadsheet
        ArrayList<String> sheetCols = new ArrayList<>();
        if (headerFeed != null) {
            for (Object column : headerFeed) {
                sheetCols.add(column.toString());
            }
        } else {
            mResults.put(id, form_fail + "couldn't get header feed");
            return false;
        }

        ArrayList<String> missingColumns = new ArrayList<>();
        for (String col : columnNames) {
            if (!sheetCols.contains(col)) {
                missingColumns.add(col);
            }
        }

        if (missingColumns.size() > 0) {
            // we had some missing columns, so error out
            String missingString = "";
            for (int i = 0; i < missingColumns.size(); i++) {
                missingString += missingColumns.get(i);
                if (i < missingColumns.size() - 1) {
                    missingString += ", ";
                }
            }
            mResults.put(
                    id,
                    form_fail
                            + Collect.getInstance().getString(
                            R.string.google_sheets_missing_columns, missingString));
            return false;
        }

        // if we get here.. all has matched
        // so write the values

        // add photos to answer set
        for (String key : uploadedPhotos.keySet()) {
            String url = uploadedPhotos.get(key).getImageLink();
            answersToUpload.put(key, url);
        }

        ArrayList<Object> list = new ArrayList<>();

        for (String path : sheetCols) {
            String answer = "";
            if (path.equals(" ") || !columnNames.contains(path)) {
                //ignores the blank fields and extra fields
            }
            else if (columnNames.contains(path)) { // if column present in sheet
                if (answersToUpload.containsKey(path)) {
                    answer = answersToUpload.get(path);
                    // Check to see if answer is a location, if so, get rid of accuracy
                    // and altitude
                    // try to match a fairly specific pattern to determine
                    // if it's a location
                    // [-]#.# [-]#.# #.# #.#
                    Pattern p = Pattern
                            .compile(
                                    "^-?[0-9]+\\.[0-9]+\\s-?[0-9]+\\.[0-9]+\\s-?[0-9]+\\"
                                            + ".[0-9]+\\s[0-9]+\\.[0-9]+$");
                    Matcher m = p.matcher(answer);
                    if (m.matches()) {
                        // get rid of everything after the second space
                        int firstSpace = answer.indexOf(" ");
                        int secondSpace = answer.indexOf(" ", firstSpace + 1);
                        answer = answer.substring(0, secondSpace);
                        answer = answer.replace(' ', ',');
                    }
                }
            }
            list.add(answer);
        }
        ArrayList<List<Object>> content = new ArrayList<>();
        content.add(list);

        ValueRange row = new ValueRange();
        row.setValues(content);


        // Send the new row to the API for insertion.
        try {
            mService.spreadsheets().values()
                    .append(spreadsheetId, spreadsheetName, row)
                    .setValueInputOption("USER_ENTERED").execute();
        } catch (IOException e) {
            e.printStackTrace();
            mResults.put(id, form_fail + e.getMessage());
            return false;
        }

        mResults.put(id, Collect.getInstance().getString(R.string.success));
        return true;
    }

    private void uploadPhotosToPicasa(HashMap<String, String> photos,
                                      HashMap<String, PhotoEntry> uploaded, PicasaClient client, AlbumEntry albumToUse,
                                      File instanceFile) throws IOException {
        for (String key : photos.keySet()) {
            String filename = instanceFile.getParentFile() + "/" + photos.get(key);
            File toUpload = new File(filename);

            // first check the local content provider
            // to see if this photo already has a picasa_id
            String selection = Images.Media.DATA + "=?";
            String[] selectionArgs = {
                    filename
            };
            Cursor c = Collect.getInstance().getContentResolver()
                    .query(Images.Media.EXTERNAL_CONTENT_URI, null, selection, selectionArgs, null);
            if (c.getCount() != 1) {
                c.close();
                throw new FileNotFoundException(picasa_fail
                        + Collect.getInstance().getString(R.string.picasa_upload_error, filename));
            }

            // assume it's not already in picasa
            boolean inPicasa = false;

            // this will contain the link to the photo
            PhotoEntry picasaPhoto = null;

            c.moveToFirst();
            String picasa_id = c.getString(c.getColumnIndex(Images.Media.PICASA_ID));
            if (picasa_id == null || picasa_id.equalsIgnoreCase("")) {
                // not in picasa, so continue
            } else {
                // got a picasa ID, make sure it exists in this
                // particular album online
                // if it does, go on
                // if it doesn't, upload it and update the picasa_id
                if (albumToUse.numPhotos != 0) {
                    PicasaUrl photosUrl = new PicasaUrl(albumToUse.getFeedLink());
                    AlbumFeed albumFeed = client.executeGetAlbumFeed(photosUrl);

                    for (PhotoEntry photo : albumFeed.photos) {
                        if (picasa_id.equals(photo.id)) {
                            // already in picasa, no need to upload
                            inPicasa = true;
                            picasaPhoto = photo;
                        }
                    }
                }
            }

            // wasn't already there, so upload a new copy and update the
            // content provder with its picasa_id
            if (!inPicasa) {
                String fileName = toUpload.getAbsolutePath();
                File file = new File(fileName);
                String mimetype = URLConnection.guessContentTypeFromName(file.getName());
                InputStreamContent content = new InputStreamContent(mimetype, new FileInputStream(
                        file));

                picasaPhoto = client.executeInsertPhotoEntry(
                        new PicasaUrl(albumToUse.getFeedLink()), content, toUpload.getName());

                ContentValues cv = new ContentValues();
                cv.put(Images.Media.PICASA_ID, picasaPhoto.id);

                // update the content provider picasa_id once we upload
                String where = Images.Media.DATA + "=?";
                String[] whereArgs = {
                        toUpload.getAbsolutePath()
                };
                Collect.getInstance().getContentResolver()
                        .update(Images.Media.EXTERNAL_CONTENT_URI, cv, where, whereArgs);
            }

            // uploadedPhotos keeps track of the uploaded URL
            // relative to the path
            uploaded.put(key, picasaPhoto);
        }
    }

    private AlbumEntry getOrCreatePicasaAlbum(PicasaClient client, String jrFormId)
            throws IOException {
        AlbumEntry albumToUse = null;
        PicasaUrl url = PicasaUrl.relativeToRoot("feed/api/user/default");
        UserFeed feed;

        feed = client.executeGetUserFeed(url);

        // Find an album with a title matching the form_id
        // Technically there could be multiple albums that match
        // We just use the first one that matches
        if (feed.albums != null) {
            for (AlbumEntry album : feed.albums) {
                if (jrFormId.equals(album.title)) {
                    albumToUse = album;
                    break;
                }
            }
        }

        // no album exited, so create one
        if (albumToUse == null) {
            AlbumEntry newAlbum = new AlbumEntry();
            newAlbum.access = "private";
            newAlbum.title = jrFormId;
            newAlbum.summary = "Images for form: " + jrFormId;
            albumToUse = client.executeInsert(feed, newAlbum);
        }
        return albumToUse;
    }

    private void getColumns(String filePath, ArrayList<String> columns)
            throws XmlPullParserException, IOException, FormException {
        File formFile = new File(filePath);
        FileInputStream in;

        in = new FileInputStream(formFile);
        XmlPullParser parser = Xml.newPullParser();

        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        readFormFeed(parser, columns);
        in.close();
    }

    private void readFormFeed(XmlPullParser parser, ArrayList<String> columns)
            throws XmlPullParserException, IOException, FormException {
        ArrayList<String> path = new ArrayList<String>();

        // we put path names in here as we go, and if we hit a duplicate we
        // blow up
        boolean getPaths = false;
        boolean inBody = false;
        int event = parser.next();
        int depth = 0;
        int lastpush = 0;
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equalsIgnoreCase("body")
                            || parser.getName().equalsIgnoreCase("h:body")) {
                        inBody = true;
                    } else if (inBody && parser.getName().equalsIgnoreCase("repeat")) {
                        throw new FormException(Collect.getInstance().getString(
                                R.string.google_repeat_error));
                    } else if (parser.getName().equalsIgnoreCase("upload")) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeName(i);
                            if (attr.startsWith("mediatype")) {
                                String attrValue = parser.getAttributeValue(i);
                                if (attrValue.startsWith("audio")) {
                                    throw new FormException(Collect.getInstance().getString(
                                            R.string.google_audio_error));
                                } else if (attrValue.startsWith("video")) {
                                    throw new FormException(Collect.getInstance().getString(
                                            R.string.google_video_error));
                                }
                            }
                        }
                    }
                    if (getPaths) {
                        path.add(parser.getName());
                        depth++;
                        lastpush = depth;
                    }
                    if (parser.getName().equals("instance")) {
                        getPaths = true;
                    }
                    break;
                case XmlPullParser.TEXT:
                    // skip it
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getName().equals("body") || parser.getName().equals("h:body")) {
                        inBody = false;
                    }
                    if (parser.getName().equals("instance")) {
                        getPaths = false;
                    }
                    if (getPaths) {
                        if (depth == lastpush) {
                            columns.add(getPath(path));
                        } else {
                            lastpush--;
                        }
                        path.remove(path.size() - 1);
                        depth--;
                    }
                    break;
                default:
                    Log.i(TAG, "DEFAULTING: " + parser.getName() + " :: " + parser.getEventType());
                    break;
            }
            event = parser.next();
        }
    }

    private void processInstanceXML(File instanceFile,
                                    HashMap<String, String> answersToUpload,
                                    HashMap<String, String> photosToUpload)
            throws XmlPullParserException, IOException,
            FormException {
        FileInputStream in;

        in = new FileInputStream(instanceFile);
        XmlPullParser parser = Xml.newPullParser();

        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        readInstanceFeed(parser, answersToUpload, photosToUpload);
        in.close();
    }

    private void readInstanceFeed(XmlPullParser parser,
                                  HashMap<String, String> answersToUpload,
                                  HashMap<String, String> photosToUpload)
            throws XmlPullParserException, IOException,
            FormException {
        ArrayList<String> path = new ArrayList<String>();

        int event = parser.next();
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG:
                    path.add(parser.getName());
                    break;
                case XmlPullParser.TEXT:
                    String answer = parser.getText();
                    if (answer.endsWith(".jpg") || answer.endsWith(".png")) {
                        photosToUpload.put(getPath(path), answer);
                    } else {
                        answersToUpload.put(getPath(path), answer);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    path.remove(path.size() - 1);
                    break;
                default:
                    Log.i(TAG, "DEFAULTING: " + parser.getName() + " :: " + parser.getEventType());
                    break;
            }
            event = parser.next();
        }
    }

    private String getPath(ArrayList<String> path) {
        String currentPath = "";
        boolean first = true;
        for (String node : path) {
            if (first) {
                currentPath = node;
                first = false;
            } else {
                currentPath += "-" + node;
            }
        }
        return currentPath;
    }

    @Override
    protected void onPostExecute(HashMap<String, String> results) {
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.uploadingComplete(results);

                if (results != null && !results.isEmpty()) {
                    StringBuilder selection = new StringBuilder();
                    Set<String> keys = results.keySet();
                    Iterator<String> it = keys.iterator();

                    String[] selectionArgs = new String[keys.size() + 1];
                    int i = 0;
                    selection.append("(");
                    while (it.hasNext()) {
                        String id = it.next();
                        selection.append(InstanceColumns._ID + "=?");
                        selectionArgs[i++] = id;
                        if (i != keys.size()) {
                            selection.append(" or ");
                        }
                    }

                    selection.append(") and status=?");
                    selectionArgs[i] = InstanceProviderAPI.STATUS_SUBMITTED;

                    Cursor uploadResults = null;
                    try {
                        uploadResults = new InstancesDao().getInstancesCursor(selection.toString(),
                                selectionArgs);
                        if (uploadResults.getCount() > 0) {
                            Long[] toDelete = new Long[uploadResults.getCount()];
                            uploadResults.moveToPosition(-1);

                            int cnt = 0;
                            while (uploadResults.moveToNext()) {
                                toDelete[cnt] = uploadResults.getLong(uploadResults
                                        .getColumnIndex(InstanceColumns._ID));
                                cnt++;
                            }

                            boolean deleteFlag = PreferenceManager.getDefaultSharedPreferences(
                                    Collect.getInstance().getApplicationContext()).getBoolean(
                                    PreferenceKeys.KEY_DELETE_AFTER_SEND, false);
                            if (deleteFlag) {
                                DeleteInstancesTask dit = new DeleteInstancesTask();
                                dit.setContentResolver(Collect.getInstance().getContentResolver());
                                dit.execute(toDelete);
                            }

                        }
                    } finally {
                        if (uploadResults != null) {
                            uploadResults.close();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (this) {
            if (mStateListener != null) {
                // update progress and total
                mStateListener.progressUpdate(values[0], values[1]);
            }
        }
    }

    /**
     * Google sheets currently only allows a-zA-Z0-9 and dash
     */
    private boolean isValidGoogleSheetsString(String name) {
        Pattern p = Pattern
                .compile("^[a-zA-Z0-9\\-]+$");
        Matcher m = p.matcher(name);
        return m.matches();
    }

    /**
     * Fetches the spreadsheet with the provided spreadsheetId
     * <p>
     * get(sheetId, range) method requires two parameters
     * <p>
     * since we want to search the whole sheet so we provide only the sheet name as range
     * <p>
     * range is in A1 notation
     * eg. Sheet1!A1:G7
     * <p>
     * For more info   :   https://developers.google.com/sheets/api/reference/rest/
     *
     * @param spreadsheetId
     * @param spreadsheetName
     * @return
     * @throws IOException
     */
    private List<List<Object>> getHeaderFeed(String spreadsheetId, String spreadsheetName)
            throws IOException {
        ValueRange response = mService.spreadsheets()
                .values()
                .get(spreadsheetId, spreadsheetName)
                .execute();
        return response.getValues();
    }
}
