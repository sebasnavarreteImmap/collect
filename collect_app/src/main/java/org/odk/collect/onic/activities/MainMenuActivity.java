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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.dao.InstancesDao;
import org.odk.collect.onic.listeners.FormDownloaderListener;
import org.odk.collect.onic.logic.FormDetails;
import org.odk.collect.onic.preferences.AboutPreferencesActivity;
import org.odk.collect.onic.preferences.AdminKeys;
import org.odk.collect.onic.preferences.AdminPreferencesActivity;
import org.odk.collect.onic.preferences.AutoSendPreferenceMigrator;
import org.odk.collect.onic.preferences.GeneralSharedPreferences;
import org.odk.collect.onic.preferences.PreferenceKeys;
import org.odk.collect.onic.preferences.PreferencesActivity;
import org.odk.collect.onic.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.onic.tasks.DownloadFormListTask;
import org.odk.collect.onic.tasks.DownloadFormsTask;
import org.odk.collect.onic.utilities.ApplicationConstants;
import org.odk.collect.onic.utilities.AuthDialogUtility;
import org.odk.collect.onic.utilities.PlayServicesUtil;
import org.odk.collect.onic.utilities.ToastUtils;
import org.odk.collect.onic.utilities.SharedPreferencesUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import timber.log.Timber;

//Inicio CreadoJorge
import org.odk.collect.onic.listeners.FormListDownloaderListener;
import org.odk.collect.onic.listeners.FormDownloaderListener;
import org.odk.collect.onic.utilities.WebUtils;


//Fin CreadoJorge

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class MainMenuActivity extends AppCompatActivity implements FormListDownloaderListener, FormDownloaderListener {

    private static final int PASSWORD_DIALOG = 1;

    private static final boolean EXIT = true;
    // buttons
    private Button enterDataButton;
    private Button manageFilesButton;
    private Button sendDataButton;
    private Button viewSentFormsButton;
    private Button reviewDataButton;
    private Button getFormsButton;
    private ImageView userProfileButton;

    ////Inicio CreadoJorge
    private static final String FORMLIST = "formlist";

    private Button to_backButton;
    private HashMap<String, FormDetails> formNamesAndURLs = new HashMap<String, FormDetails>();
    private ArrayList<HashMap<String, String>> formList;
    private ArrayList<HashMap<String, String>> filteredFormList = new ArrayList<>();

    private String alertMsg;
    private boolean alertShowing = false;
    private String alertTitle;

    private boolean shouldExit;

    private ProgressDialog progressDialog;
    private static final int PROGRESS_DIALOG = 1;
    private DownloadFormListTask downloadFormListTask;
    private DownloadFormsTask downloadFormsTask;


    private static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    private static final String FORMID_DISPLAY = "formiddisplay";

    private static final String FORM_ID_KEY = "formid";
    private static final String FORM_VERSION_KEY = "formversion";

    private String id_odk_module_institucional;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();




    //Fin CreadoJorge

    private View reviewSpacer;
    private View getFormsSpacer;
    private AlertDialog alertDialog;
    private SharedPreferences adminPreferences;
    private int completedCount;
    private int savedCount;
    private int viewSentCount;
    private Cursor finalizedCursor;
    private Cursor savedCursor;
    private Cursor viewSentCursor;
    private IncomingHandler handler = new IncomingHandler(this);
    private MyContentObserver contentObserver = new MyContentObserver();

    // private static boolean DO_NOT_EXIT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
       // initToolbar();

        //CreadoJorge: get idProjectodk from InstitucionalModuleSelectActivity
        TextView mensajeIdOdk = (TextView) findViewById(R.id.txtIdKobo);

        Bundle id_module_institucional_odk = this.getIntent().getExtras();
        if(id_module_institucional_odk!=null){
            id_odk_module_institucional = id_module_institucional_odk.getString("idProjectodk");
            mensajeIdOdk.setText("EL ID DEL KOBO SELECCIONADO: "+id_odk_module_institucional);
        }

        //CreadoJorge: ImagenButton, go to UserPRofileActivity
        userProfileButton = (ImageView) findViewById(R.id.userProfileButton);
        userProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainMenuActivity.this,UserProfileActivity.class));
            }
        });


        // enter data button. expects a result.
        enterDataButton = (Button) findViewById(R.id.enter_data);
        enterDataButton.setText(getString(R.string.enter_data_button));
        enterDataButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //CreadoJorge //Descarga Formulario correspondiente al modulo seleccionado y lleva a NuevoFormulario
                downloadSelectedFiles();

                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        FormChooserList.class);
                //En Extras del intent que paso a FormChooserList, paso nameProjectodk para que filtre y solo muestre 1
                Bundle name_ins_module_bundle = new Bundle();
                name_ins_module_bundle.putString("idProjectodk",id_odk_module_institucional);
                i.putExtras(name_ins_module_bundle);
                startActivity(i);
            }
        });

        // review data button. expects a result.
        reviewDataButton = (Button) findViewById(R.id.review_data);
        reviewDataButton.setText(getString(R.string.review_data_button));
        reviewDataButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, ApplicationConstants.FormModes.EDIT_SAVED, "click");
                Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                        ApplicationConstants.FormModes.EDIT_SAVED);
                startActivity(i);
            }
        });

        // send data button. expects a result.
        sendDataButton = (Button) findViewById(R.id.send_data);
        sendDataButton.setText(getString(R.string.send_data_button));
        sendDataButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "uploadForms", "click");
                Intent i = new Intent(getApplicationContext(),
                        InstanceUploaderList.class);
                startActivity(i);
            }
        });

        //View sent forms
        viewSentFormsButton = (Button) findViewById(R.id.view_sent_forms);
        viewSentFormsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger().logAction(this,
                        ApplicationConstants.FormModes.VIEW_SENT, "click");
                Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                        ApplicationConstants.FormModes.VIEW_SENT);
                startActivity(i);
            }
        });

        // manage forms button. no result expected.
        getFormsButton = (Button) findViewById(R.id.get_forms);
        getFormsButton.setText(getString(R.string.get_forms));
        getFormsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "downloadBlankForms", "click");
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(MainMenuActivity.this);
                String protocol = sharedPreferences.getString(
                        PreferenceKeys.KEY_PROTOCOL, getString(R.string.protocol_odk_default));
                Intent i = null;
                if (protocol.equalsIgnoreCase(getString(R.string.protocol_google_sheets))) {
                    if (PlayServicesUtil.isGooglePlayServicesAvailable(MainMenuActivity.this)) {
                        i = new Intent(getApplicationContext(),
                                GoogleDriveActivity.class);
                    } else {
                        PlayServicesUtil.showGooglePlayServicesAvailabilityErrorDialog(MainMenuActivity.this);
                        return;
                    }
                } else {
                    i = new Intent(getApplicationContext(),
                            FormDownloadList.class);
                }
                startActivity(i);

            }
        });

        // manage forms button. no result expected.
        manageFilesButton = (Button) findViewById(R.id.manage_forms);
        manageFilesButton.setText(getString(R.string.manage_files));
        manageFilesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "deleteSavedForms", "click");
                Intent i = new Intent(getApplicationContext(),
                        FileManagerTabs.class);
                startActivity(i);
            }
        });


        //To BACK button. CreadoJorge
        to_backButton = (Button) findViewById(R.id.to_backButton);
        to_backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");

                //Firebase actual User
                Integer userRol = 1;

                String email = user.getEmail();

                if(email.equals("usuarioparticular@gmail.com")){ //Particular User

                    finish();

                    Intent i = new Intent(getApplicationContext(),
                            SelectUserTypeActivity.class); // back to SelectUserType

                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    startActivity(i);

                }else { //Institucional User



                    Intent i = new Intent(getApplicationContext(),
                            InstitucionalModuleSelectActivity.class); // back to InstitucionalModuleSelectActivity
                    startActivity(i);



                }


            }
        });

        // must be at the beginning of any activity that can be called from an
        // external intent
        Timber.i("Starting up, creating directories");
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

       /* {
            // dynamically construct the "ODK Collect vA.B" string
            TextView mainMenuMessageLabel = (TextView) findViewById(R.id.main_menu_header);
            mainMenuMessageLabel.setText(Collect.getInstance()
                    .getVersionedAppName());
        }*/

        File f = new File(Collect.ODK_ROOT + "/collect.settings");
        File j = new File(Collect.ODK_ROOT + "/collect.settings.json");
        // Give JSON file preference
        if (j.exists()) {
            SharedPreferencesUtils sharedPrefs = new SharedPreferencesUtils();
            boolean success = sharedPrefs.loadSharedPreferencesFromJSONFile(j);
            if (success) {
                ToastUtils.showLongToast(R.string.settings_successfully_loaded_file_notification);
                j.delete();

                // Delete settings file to prevent overwrite of settings from JSON file on next startup
                if (f.exists()) {
                    f.delete();
                }
            } else {
                ToastUtils.showLongToast(R.string.corrupt_settings_file_notification);
            }
        } else if (f.exists()) {
            boolean success = loadSharedPreferencesFromFile(f);
            if (success) {
                ToastUtils.showLongToast(R.string.settings_successfully_loaded_file_notification);
                f.delete();
            } else {
                ToastUtils.showLongToast(R.string.corrupt_settings_file_notification);
            }
        }

        reviewSpacer = findViewById(R.id.review_spacer);
        getFormsSpacer = findViewById(R.id.get_forms_spacer);

        adminPreferences = this.getSharedPreferences(
                AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        InstancesDao instancesDao = new InstancesDao();

        // count for finalized instances
        try {
            finalizedCursor = instancesDao.getFinalizedInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        if (finalizedCursor != null) {
            startManagingCursor(finalizedCursor);
        }
        completedCount = finalizedCursor != null ? finalizedCursor.getCount() : 0;
        getContentResolver().registerContentObserver(InstanceColumns.CONTENT_URI, true,
                contentObserver);
        // finalizedCursor.registerContentObserver(contentObserver);

        // count for saved instances
        try {
            savedCursor = instancesDao.getUnsentInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        if (savedCursor != null) {
            startManagingCursor(savedCursor);
        }
        savedCount = savedCursor != null ? savedCursor.getCount() : 0;

        //count for view sent form
        try {
            viewSentCursor = instancesDao.getSentInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }
        if (viewSentCursor != null) {
            startManagingCursor(viewSentCursor);
        }
        viewSentCount = viewSentCursor != null ? viewSentCursor.getCount() : 0;

        updateButtons();
        setupGoogleAnalytics();

        //CreadoJorge
        if (savedInstanceState != null && savedInstanceState.containsKey(FORMLIST)) {
            formList =
                    (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(
                            FORMLIST);
        } else {
            formList = new ArrayList<HashMap<String, String>>();
        }
        downloadFormList();
        //Cierra CreadoJorge
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setTitle(getString(R.string.main_menu));
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        boolean edit = sharedPreferences.getBoolean(
                AdminKeys.KEY_EDIT_SAVED, true);
        if (!edit) {
            if (reviewDataButton != null) {
                reviewDataButton.setVisibility(View.GONE);
            }
            if (reviewSpacer != null) {
                reviewSpacer.setVisibility(View.GONE);
            }
        } else {
            if (reviewDataButton != null) {
                reviewDataButton.setVisibility(View.VISIBLE);
            }
            if (reviewSpacer != null) {
                reviewSpacer.setVisibility(View.VISIBLE);
            }
        }

        boolean send = sharedPreferences.getBoolean(
                AdminKeys.KEY_SEND_FINALIZED, true);
        if (!send) {
            if (sendDataButton != null) {
                sendDataButton.setVisibility(View.GONE);
            }
        } else {
            if (sendDataButton != null) {
                sendDataButton.setVisibility(View.VISIBLE);
            }
        }

        boolean viewSent = sharedPreferences.getBoolean(
                AdminKeys.KEY_VIEW_SENT, true);
        if (!viewSent) {
            if (viewSentFormsButton != null) {
                viewSentFormsButton.setVisibility(View.GONE);
            }
        } else {
            if (viewSentFormsButton != null) {
                viewSentFormsButton.setVisibility(View.VISIBLE);
            }
        }

        boolean getBlank = sharedPreferences.getBoolean(
                AdminKeys.KEY_GET_BLANK, true);
        if (!getBlank) {
            if (getFormsButton != null) {
                getFormsButton.setVisibility(View.GONE);
            }
            if (getFormsSpacer != null) {
                getFormsSpacer.setVisibility(View.GONE);
            }
        } else {
            if (getFormsButton != null) {
                getFormsButton.setVisibility(View.VISIBLE);
            }
            if (getFormsSpacer != null) {
                getFormsSpacer.setVisibility(View.VISIBLE);
            }
        }

        boolean deleteSaved = sharedPreferences.getBoolean(
                AdminKeys.KEY_DELETE_SAVED, true);
        if (!deleteSaved) {
            if (manageFilesButton != null) {
                manageFilesButton.setVisibility(View.GONE);
            }
        } else {
            if (manageFilesButton != null) {
                manageFilesButton.setVisibility(View.VISIBLE);
            }
        }

        ((Collect) getApplication())
                .getDefaultTracker()
                .enableAutoActivityTracking(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "onCreateOptionsMenu", "show");
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                Collect.getInstance()
                        .getActivityLogger()
                        .logAction(this, "onOptionsItemSelected",
                                "MENU_ABOUT");
                Intent aboutIntent = new Intent(this, AboutPreferencesActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.menu_general_preferences:
                Collect.getInstance()
                        .getActivityLogger()
                        .logAction(this, "onOptionsItemSelected",
                                "MENU_PREFERENCES");
                Intent ig = new Intent(this, PreferencesActivity.class);
                startActivity(ig);
                return true;
            case R.id.menu_admin_preferences:
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "onOptionsItemSelected", "MENU_ADMIN");
                String pw = adminPreferences.getString(
                        AdminKeys.KEY_ADMIN_PW, "");
                if ("".equalsIgnoreCase(pw)) {
                    Intent i = new Intent(getApplicationContext(),
                            AdminPreferencesActivity.class);
                    startActivity(i);
                } else {
                    showDialog(PASSWORD_DIALOG);
                    Collect.getInstance().getActivityLogger()
                            .logAction(this, "createAdminPasswordDialog", "show");
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "createErrorDialog", "show");
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance()
                                .getActivityLogger()
                                .logAction(this, "createErrorDialog",
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
            case PASSWORD_DIALOG:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final AlertDialog passwordDialog = builder.create();
                passwordDialog.setTitle(getString(R.string.enter_admin_password));
                LayoutInflater inflater = this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialogbox_layout, null);
                passwordDialog.setView(dialogView, 20, 10, 20, 10);
                final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.checkBox);
                final EditText input = (EditText) dialogView.findViewById(R.id.editText);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (!checkBox.isChecked()) {
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        }
                    }
                });
                passwordDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                        getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                String value = input.getText().toString();
                                String pw = adminPreferences.getString(
                                        AdminKeys.KEY_ADMIN_PW, "");
                                if (pw.compareTo(value) == 0) {
                                    Intent i = new Intent(getApplicationContext(),
                                            AdminPreferencesActivity.class);
                                    startActivity(i);
                                    input.setText("");
                                    passwordDialog.dismiss();
                                } else {
                                    ToastUtils.showShortToast(R.string.admin_password_incorrect);
                                    Collect.getInstance()
                                            .getActivityLogger()
                                            .logAction(this, "adminPasswordDialog",
                                                    "PASSWORD_INCORRECT");
                                }
                            }
                        });

                passwordDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                        getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance()
                                        .getActivityLogger()
                                        .logAction(this, "adminPasswordDialog",
                                                "cancel");
                                input.setText("");
                            }
                        });

                passwordDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                return passwordDialog;

        }
        return null;
    }

    // This flag must be set each time the app starts up
    private void setupGoogleAnalytics() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect
                .getInstance());
        boolean isAnalyticsEnabled = settings.getBoolean(PreferenceKeys.KEY_ANALYTICS, true);
        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(getApplicationContext());
        googleAnalytics.setAppOptOut(!isAnalyticsEnabled);
    }

    private void updateButtons() {
        if (finalizedCursor != null && !finalizedCursor.isClosed()) {
            finalizedCursor.requery();
            completedCount = finalizedCursor.getCount();
            if (completedCount > 0) {
                sendDataButton.setText(
                        getString(R.string.send_data_button, String.valueOf(completedCount)));
            } else {
                sendDataButton.setText(getString(R.string.send_data));
            }
        } else {
            sendDataButton.setText(getString(R.string.send_data));
            Timber.w("Cannot update \"Send Finalized\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }

        if (savedCursor != null && !savedCursor.isClosed()) {
            savedCursor.requery();
            savedCount = savedCursor.getCount();
            if (savedCount > 0) {
                reviewDataButton.setText(getString(R.string.review_data_button,
                        String.valueOf(savedCount)));
            } else {
                reviewDataButton.setText(getString(R.string.review_data));
            }
        } else {
            reviewDataButton.setText(getString(R.string.review_data));
            Timber.w("Cannot update \"Edit Form\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }

        if (viewSentCursor != null && !viewSentCursor.isClosed()) {
            viewSentCursor.requery();
            viewSentCount = viewSentCursor.getCount();
            if (viewSentCount > 0) {
                viewSentFormsButton.setText(
                        getString(R.string.view_sent_forms_button, String.valueOf(viewSentCount)));
            } else {
                viewSentFormsButton.setText(getString(R.string.view_sent_forms));
            }
        } else {
            viewSentFormsButton.setText(getString(R.string.view_sent_forms));
            Timber.w("Cannot update \"View Sent\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }
    }

    private boolean loadSharedPreferencesFromFile(File src) {
        // this should probably be in a thread if it ever gets big
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(
                    this).edit();
            prefEdit.clear();
            // first object is preferences
            Map<String, ?> entries = (Map<String, ?>) input.readObject();

            AutoSendPreferenceMigrator.migrate(entries);

            for (Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    prefEdit.putBoolean(key, (Boolean) v);
                } else if (v instanceof Float) {
                    prefEdit.putFloat(key, (Float) v);
                } else if (v instanceof Integer) {
                    prefEdit.putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    prefEdit.putLong(key, (Long) v);
                } else if (v instanceof String) {
                    prefEdit.putString(key, ((String) v));
                }
            }
            prefEdit.apply();
            AuthDialogUtility.setWebCredentialsFromPreferences();

            // second object is admin options
            Editor adminEdit = getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES,
                    0).edit();
            adminEdit.clear();
            // first object is preferences
            Map<String, ?> adminEntries = (Map<String, ?>) input.readObject();
            for (Entry<String, ?> entry : adminEntries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    adminEdit.putBoolean(key, (Boolean) v);
                } else if (v instanceof Float) {
                    adminEdit.putFloat(key, (Float) v);
                } else if (v instanceof Integer) {
                    adminEdit.putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    adminEdit.putLong(key, (Long) v);
                } else if (v instanceof String) {
                    adminEdit.putString(key, ((String) v));
                }
            }
            adminEdit.apply();

            res = true;
        } catch (IOException | ClassNotFoundException e) {
            Timber.e(e, "Exception while loading preferences from file due to : %s ", e.getMessage());
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                Timber.e(ex, "Exception thrown while closing an input stream due to: %s ", ex.getMessage());
            }
        }
        return res;
    }

    /*
     * Used to prevent memory leaks
     */
    static class IncomingHandler extends Handler {
        private final WeakReference<MainMenuActivity> target;

        IncomingHandler(MainMenuActivity target) {
            this.target = new WeakReference<MainMenuActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainMenuActivity target = this.target.get();
            if (target != null) {
                target.updateButtons();
            }
        }
    }

    /**
     * notifies us that something changed
     */
    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            handler.sendEmptyMessage(0);
        }
    }


    //Inicio CreadoJorge
    //Conexión al servidor y descarga de formularios. Duplicado de FormDownloadList.java Actividad
    private void downloadFormList() {
        //CreadoJorge
        /*
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(MainMenuActivity.this);
        String protocol = sharedPreferences.getString(
                PreferenceKeys.KEY_PROTOCOL, getString(R.string.protocol_odk_default));


        String usuario = sharedPreferences.getString(
                PreferenceKeys.KEY_USERNAME, "snit");
        String password = sharedPreferences.getString(
                PreferenceKeys.KEY_PASSWORD, "navarino");
                */


        //Valores los tomo de la autenticacion o base datos Firebase

        //String userName = "snavarrete";
        //String password = "toxicity.1";
        //String userName = "Sebastian_Navarrete";
        //String password = "sebasnavarrete2020";
        //String url = "";
        //url = "https://monitoreoterritorial-onic.co:8443";
        //url = "https://kc.humanitarianresponse.info/snavarrete";
        //

        Integer rolUser = 1; //toma el rol del currentuser

        /*

        if(rolUser == 1){
            Log.e("ESCOGE ROL 1",rolUser.toString());
             userName = "...";
             password = "...";

        }else if(rolUser == 2){
             userName = "snit";
             password = "navarino";
             url = "https://kc.humanitarianresponse.info/snit";
        }else{
            finish();
        }*/


        //GeneralSharedPreferences.getInstance().save(PreferenceKeys.KEY_USERNAME, userName);
        //String nameuser = "";
        //nameuser = GeneralSharedPreferences.getInstance().get(PreferenceKeys.KEY_USERNAME);

        //GeneralSharedPreferences.getInstance().save(PreferenceKeys.KEY_PASSWORD, password);
        //GeneralSharedPreferences.getInstance().save(PreferenceKeys.KEY_SERVER_URL , url);
        //GeneralSharedPreferences.getInstance().save(PreferenceKeys.KEY_SUBMISSION_URL , url);

        //WebUtils.addCredentials(username, password, host);


        Log.e("EN DOWNLOADFORMLIST!","EN DOWNLOADFORMLIST!");

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

        Log.e("NETWORK INFO: ",ni.toString());



        if (ni == null || !ni.isConnected()) {
            ToastUtils.showShortToast(R.string.no_connection);
        } else {

            formNamesAndURLs = new HashMap<String, FormDetails>();
            if (progressDialog != null) {
                // This is needed because onPrepareDialog() is broken in 1.6.
                progressDialog.setMessage(getString(R.string.please_wait));
            }
            //ComentadoJorge//showDialog(PROGRESS_DIALOG);

            if (downloadFormListTask != null
                    && downloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
                return; // we are already doing the download!!!
            } else if (downloadFormListTask != null) {
                downloadFormListTask.setDownloaderListener(null);
                downloadFormListTask.cancel(true);
                downloadFormListTask = null;
            }

            downloadFormListTask = new DownloadFormListTask();
            downloadFormListTask.setDownloaderListener(this);
            downloadFormListTask.execute();

        }
    }

    /*
     * Called when the form list has finished downloading. results will either contain a set of
     * <formname, formdetails> tuples, or one tuple of DL.ERROR.MSG and the associated message.
     */
    public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
        //CreadoJorge
        Log.e("EN FORMLISTDOWNL!","EN FORMLISTDOWNL!");

        //ComentadoJorge//dismissDialog(PROGRESS_DIALOG);
        downloadFormListTask.setDownloaderListener(null);
        downloadFormListTask = null;

        if (result == null) {
            Timber.e("Formlist Downloading returned null.  That shouldn't happen");
            // Just displayes "error occured" to the user, but this should never happen.
            //CommentadoJorge
            /*createAlertDialog(getString(R.string.load_remote_form_error),
                    getString(R.string.error_occured), EXIT); */
            return;
        }

        if (result.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED)) {

            // need authorization
            //ComentadoJorge//showDialog(AUTH_DIALOG);
        } else if (result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            Log.e("MENSAJE ERROR","MENSAJE ERROR");

            //ComentadoJorge
            /*
            // Download failed
            String dialogMessage =
                    getString(R.string.list_failed_with_error,
                            result.get(DownloadFormListTask.DL_ERROR_MSG).errorStr);
            String dialogTitle = getString(R.string.load_remote_form_error);
             createAlertDialog(dialogTitle, dialogMessage, DO_NOT_EXIT);
             */
        } else {
            Log.e("CREA LISTA","LISTA FORMULARIOS");
            // Everything worked. Clear the list and add the results.
            formNamesAndURLs = result;

            //ComentadoJorge//
            formList.clear();

            ArrayList<String> ids = new ArrayList<String>(formNamesAndURLs.keySet());
            for (int i = 0; i < result.size(); i++) {
                String formDetailsKey = ids.get(i);
                FormDetails details = formNamesAndURLs.get(formDetailsKey);
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(FORMNAME, details.formName);
                item.put(FORMID_DISPLAY,
                        ((details.formVersion == null) ? "" : (getString(R.string.version) + " "
                                + details.formVersion + " ")) + "ID: " + details.formID);
                item.put(FORMDETAIL_KEY, formDetailsKey);
                item.put(FORM_ID_KEY, details.formID);
                item.put(FORM_VERSION_KEY, details.formVersion);

                // Insert the new form in alphabetical order.
               //ComentadoJorge//
                if (formList.size() == 0) {
                    Log.e("LISTA IGUAL A 0","LISTA 0!");
                    formList.add(item);
                //ComentadoJorge
                 } else {
                    Log.e("LISTA NO ES 0","LISTA NO 0!");
                    int j;
                    for (j = 0; j < formList.size(); j++) {
                        HashMap<String, String> compareMe = formList.get(j);
                        String name = compareMe.get(FORMNAME);
                        if (name.compareTo(formNamesAndURLs.get(ids.get(i)).formName) > 0) {
                            break;
                        }
                    }
                    formList.add(j, item);
                }
            }
            filteredFormList.addAll(formList);
            //ComentadoJorge:
            /*updateAdapter();
            selectSupersededForms();
            form .notifyDataSetChanged();
            downloadButton.setEnabled(listView.getCheckedItemCount() > 0);
            toggleButtonLabel(toggleButton, listView);
            */

            //CreadoJorge
            Log.e("FILTERED FORM LIST",filteredFormList.toString());
        }
    }


    /**
     * starts the task to download the selected forms, also shows progress dialog
     */
    @SuppressWarnings("unchecked")
    private void downloadSelectedFiles() {
        int totalCount = 0;
        ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();

        //ComentadoJoge//SparseBooleanArray sba = listView.getCheckedItemPositions();
        //ComentadoJoge//for (int i = 0; i < listView.getCount(); i++) {


        for (int i = 0; i < filteredFormList.size(); i++) {
            //CreadoJorge
            Log.e("ESTO ES i: ", String.valueOf(i));
            //Log.e("ESTO ES sba: ", String.valueOf(sba));

            //OcutadoJorge
            // if (sba.get(i, false)) {
            //ComentadoJorge//HashMap<String, String> item =  (HashMap<String, String>) listView.getAdapter().getItem(i);
            HashMap<String, String> item =  (HashMap<String, String>)filteredFormList.get(i);

            //CreadoJorge:
            String valorForm = item.get(FORMDETAIL_KEY);
            Log.e("VALOR FORM: ", valorForm);
            Log.e("VALOR KOBO MODULO: ",id_odk_module_institucional);

            Log.e("ESTE ES EL VALORFORM",valorForm);
            Log.e("ESTE ES ODKSELEC",id_odk_module_institucional);
            if(valorForm.equals(id_odk_module_institucional)){
                Log.e("SON IGUALES","SON IGUALES");
                //Compara con el valor del id del formulario correspondiente al modulo seleccionado

                Log.e("INFO DEL FORMULARIO: ", item.get(FORMDETAIL_KEY));
                FormDetails detallesitem = formNamesAndURLs.get(item.get(FORMDETAIL_KEY));
                Log.e("DETALLES ITEM: ", detallesitem.toString());
                //Termina Creado Jorge

                filesToDownload.add(formNamesAndURLs.get(item.get(FORMDETAIL_KEY)));

                //CreadoJorge Log:
                Log.e("LALISTAAAAA LISTA: ", filesToDownload.toString());
            }

        }
        totalCount = filesToDownload.size();

        Collect.getInstance().getActivityLogger().logAction(this, "downloadSelectedFiles",
                Integer.toString(totalCount));

        if (totalCount > 0) {
            Log.e("TOTAL COUNT: ","ES MAYOR VOY A DESCARGAR");
            // show dialog box
            //ComentadoJorge//showDialog(PROGRESS_DIALOG);

            downloadFormsTask = new DownloadFormsTask();
            downloadFormsTask.setDownloaderListener(this);
            downloadFormsTask.execute(filesToDownload);
        } else {
            ToastUtils.showShortToast(R.string.noselect_error);
        }
    }

    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        alertMsg = getString(R.string.fetching_file, currentFile, String.valueOf(progress), String.valueOf(total));
        //ComentadoJorge //progressDialog.setMessage(alertMsg);
    }

    @Override
    public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
        //CreadoJorge
        Log.e("ENTRA A FORMSDOWN","FORMSDOWNLOADING COMPLETE");

        if (downloadFormsTask != null) {
            downloadFormsTask.setDownloaderListener(null);
        }
        //ComentadoJorge
        /*
        if (progressDialog.isShowing()) {
            // should always be true here
            progressDialog.dismiss();
        }*/


        //System.out.println(result.keySet());
        //Log.e("RESULT-","RESULT--");

        Set<FormDetails> keys = result.keySet();
        StringBuilder b = new StringBuilder();
        for (FormDetails k : keys) {
            //System.out.println(k);
            //Log.e("K RESULT KEYS","KEYS--");
            b.append(k.formName + " ("
                    + ((k.formVersion != null)
                    ? (this.getString(R.string.version) + ": " + k.formVersion + " ")
                    : "") + "ID: " + k.formID + ") - " + result.get(k));
            b.append("\n\n");
        }

        //ComentadoJorge
         //createAlertDialog(getString(R.string.download_forms_result), b.toString().trim(),EXIT);
    }


    private void createAlertDialog(String title, String message, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "show");
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // ok
                        Collect.getInstance().getActivityLogger().logAction(this,
                                "createAlertDialog", "OK");
                        // just close the dialog
                        alertShowing = false;
                        // successful download, so quit
                        //ComentadoJorge
                        /*
                        if (shouldExit) {
                            finish();
                        } */
                        break;
                }
            }
        };
        alertDialog.setCancelable(false);
        alertDialog.setButton(getString(R.string.ok), quitListener);
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertMsg = message;
        alertTitle = title;
        alertShowing = true;
        this.shouldExit = shouldExit;
        alertDialog.show();
    }




    //Fin CreadoJorge


}