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

package org.odk.collect.onic.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
//import android.support.annotation.Nullable;
//import android.support.multidex.MultiDex;
//import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.crash.FirebaseCrash;

import net.danlew.android.joda.JodaTimeAndroid;

import org.odk.collect.onic.BuildConfig;
import org.odk.collect.onic.R;
import org.odk.collect.onic.database.ActivityLogger;
import org.odk.collect.onic.external.ExternalDataManager;
import org.odk.collect.onic.logic.FormController;
import org.odk.collect.onic.logic.PropertyManager;
import org.odk.collect.onic.preferences.AutoSendPreferenceMigrator;
import org.odk.collect.onic.preferences.FormMetadataMigrator;
import org.odk.collect.onic.preferences.GeneralSharedPreferences;
import org.odk.collect.onic.preferences.PreferenceKeys;
import org.odk.collect.onic.utilities.AgingCredentialsProvider;
import org.odk.collect.onic.utilities.AuthDialogUtility;
import org.odk.collect.onic.utilities.LocaleHelper;
import org.odk.collect.onic.utilities.PRNGFixes;
import org.opendatakit.httpclientandroidlib.client.CookieStore;
import org.opendatakit.httpclientandroidlib.client.CredentialsProvider;
import org.opendatakit.httpclientandroidlib.client.protocol.HttpClientContext;
import org.opendatakit.httpclientandroidlib.impl.client.BasicCookieStore;
import org.opendatakit.httpclientandroidlib.protocol.BasicHttpContext;
import org.opendatakit.httpclientandroidlib.protocol.HttpContext;

import java.io.File;
import java.util.Locale;

import timber.log.Timber;

import static org.odk.collect.onic.logic.PropertyManager.PROPMGR_USERNAME;
import static org.odk.collect.onic.logic.PropertyManager.SCHEME_USERNAME;
import static org.odk.collect.onic.preferences.PreferenceKeys.KEY_USERNAME;

/**
 * The Open Data Kit Collect application.
 *
 * @author carlhartung
 */
public class Collect extends Application {

    // Storage paths
    public static final String ODK_ROOT = Environment.getExternalStorageDirectory()
            + File.separator + "odk";
    public static final String FORMS_PATH = ODK_ROOT + File.separator + "forms";
    public static final String INSTANCES_PATH = ODK_ROOT + File.separator + "instances";
    public static final String CACHE_PATH = ODK_ROOT + File.separator + ".cache";
    public static final String METADATA_PATH = ODK_ROOT + File.separator + "metadata";
    public static final String TMPFILE_PATH = CACHE_PATH + File.separator + "tmp.jpg";
    public static final String TMPDRAWFILE_PATH = CACHE_PATH + File.separator + "tmpDraw.jpg";
    public static final String LOG_PATH = ODK_ROOT + File.separator + "log";
    public static final String DEFAULT_FONTSIZE = "21";
    public static final int DEFAULT_FONTSIZE_INT = 21;
    public static final String OFFLINE_LAYERS = ODK_ROOT + File.separator + "layers";
    public static final String SETTINGS = ODK_ROOT + File.separator + "settings";
    public static String defaultSysLanguage;
    private static Collect singleton = null;

    // share all session cookies across all sessions...
    private CookieStore cookieStore = new BasicCookieStore();
    // retain credentials for 7 minutes...
    private CredentialsProvider credsProvider = new AgingCredentialsProvider(7 * 60 * 1000);
    private ActivityLogger activityLogger;

    @Nullable
    private FormController formController = null;
    private ExternalDataManager externalDataManager;
    private Tracker tracker;

    public static Collect getInstance() {
        return singleton;
    }

    public static int getQuestionFontsize() {
        // For testing:
        Collect instance = Collect.getInstance();
        if (instance == null) {
            return Collect.DEFAULT_FONTSIZE_INT;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(instance);
        if (settings == null) {
            return Collect.DEFAULT_FONTSIZE_INT;
        }

        String questionFont = settings.getString(PreferenceKeys.KEY_FONT_SIZE,
                Collect.DEFAULT_FONTSIZE);

        return Integer.parseInt(questionFont);
    }

    /**
     * Creates required directories on the SDCard (or other external storage)
     *
     * @throws RuntimeException if there is no SDCard or the directory exists as a non directory
     */
    public static void createODKDirs() throws RuntimeException {
        String cardstatus = Environment.getExternalStorageState();
        if (!cardstatus.equals(Environment.MEDIA_MOUNTED)) {
            throw new RuntimeException(
                    Collect.getInstance().getString(R.string.sdcard_unmounted, cardstatus));
        }

        String[] dirs = {
                ODK_ROOT, FORMS_PATH, INSTANCES_PATH, CACHE_PATH, METADATA_PATH, OFFLINE_LAYERS
        };

        for (String dirName : dirs) {
            File dir = new File(dirName);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new RuntimeException("ODK reports :: Cannot create directory: "
                            + dirName);
                }
            } else {
                if (!dir.isDirectory()) {
                    throw new RuntimeException("ODK reports :: " + dirName
                            + " exists, but is not a directory");
                }
            }
        }
    }

    /**
     * Predicate that tests whether a directory path might refer to an
     * ODK Tables instance data directory (e.g., for media attachments).
     */
    public static boolean isODKTablesInstanceDataDirectory(File directory) {
        /*
         * Special check to prevent deletion of files that
         * could be in use by ODK Tables.
         */
        String dirPath = directory.getAbsolutePath();
        if (dirPath.startsWith(Collect.ODK_ROOT)) {
            dirPath = dirPath.substring(Collect.ODK_ROOT.length());
            String[] parts = dirPath.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
            // [appName, instances, tableId, instanceId ]
            if (parts.length == 4 && parts[1].equals("instances")) {
                return true;
            }
        }
        return false;
    }

    public ActivityLogger getActivityLogger() {
        return activityLogger;
    }

    @Nullable
    public FormController getFormController() {
        return formController;
    }

    public void setFormController(@Nullable FormController controller) {
        formController = controller;
    }

    public ExternalDataManager getExternalDataManager() {
        return externalDataManager;
    }

    public void setExternalDataManager(ExternalDataManager externalDataManager) {
        this.externalDataManager = externalDataManager;
    }

    public String getVersionedAppName() {
        String versionName = BuildConfig.VERSION_NAME;
        versionName = " " + versionName.replaceFirst("-", "\n");
        return getString(R.string.app_name) + versionName;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getInstance()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo currentNetworkInfo = manager.getActiveNetworkInfo();
        return currentNetworkInfo != null && currentNetworkInfo.isConnected();
    }

    /*
        Adds support for multidex support library. For more info check out the link below,
        https://developer.android.com/studio/build/multidex.html
    */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Construct and return a session context with shared cookieStore and credsProvider so a user
     * does not have to re-enter login information.
     */
    public synchronized HttpContext getHttpContext() {

        // context holds authentication state machine, so it cannot be
        // shared across independent activities.
        HttpContext localContext = new BasicHttpContext();

        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        localContext.setAttribute(HttpClientContext.CREDS_PROVIDER, credsProvider);

        return localContext;
    }

    public CredentialsProvider getCredentialsProvider() {
        return credsProvider;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard(View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PRNGFixes.apply();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        JodaTimeAndroid.init(this);

        defaultSysLanguage = Locale.getDefault().getLanguage();
        new LocaleHelper().updateLocale(this);
        singleton = this;

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        FormMetadataMigrator.migrate(PreferenceManager.getDefaultSharedPreferences(this));
        AutoSendPreferenceMigrator.migrate();

        initProperties();

        AuthDialogUtility.setWebCredentialsFromPreferences();
        if (BuildConfig.BUILD_TYPE.equals("odkCollectRelease")) {
            Timber.plant(new CrashReportingTree());
        } else {
            Timber.plant(new NotLoggingTree());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //noinspection deprecation
        defaultSysLanguage = newConfig.locale.getLanguage();
        boolean isUsingSysLanguage = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PreferenceKeys.KEY_APP_LANGUAGE, "").equals("");
        if (!isUsingSysLanguage) {
            new LocaleHelper().updateLocale(this);
        }
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     *
     * @return tracker
     */
    public synchronized Tracker getDefaultTracker() {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            tracker = analytics.newTracker(R.xml.global_tracker);
        }
        return tracker;
    }

    private class NotLoggingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
        }
    }

    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return;
            }

            FirebaseCrash.logcat(priority, tag, message);

            if (t != null && priority == Log.ERROR) {
                FirebaseCrash.report(t);
            }
        }
    }

    public void initProperties() {
        PropertyManager mgr = new PropertyManager(this);

        // Use the server username by default if the metadata username is not defined
        if ((mgr.getSingularProperty(PROPMGR_USERNAME) == null || mgr.getSingularProperty(PROPMGR_USERNAME).isEmpty())) {
            mgr.putProperty(PROPMGR_USERNAME, SCHEME_USERNAME, (String) GeneralSharedPreferences.getInstance().get(KEY_USERNAME));
        }

        FormController.initializeJavaRosa(mgr);
        activityLogger = new ActivityLogger(mgr.getSingularProperty(PropertyManager.PROPMGR_DEVICE_ID));
    }
}
