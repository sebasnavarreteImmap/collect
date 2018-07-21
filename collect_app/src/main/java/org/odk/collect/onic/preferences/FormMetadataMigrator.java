package org.odk.collect.onic.preferences;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import timber.log.Timber;

/** Migrates existing preference values to metadata */
public class FormMetadataMigrator {

    /** The migration flow, from source to target */
    static final String[][] sourceTargetValuePairs = new String[][]{
            {PreferenceKeys.KEY_USERNAME,                  PreferenceKeys.KEY_METADATA_USERNAME},
            {PreferenceKeys.KEY_SELECTED_GOOGLE_ACCOUNT,   PreferenceKeys.KEY_METADATA_EMAIL}
    };

    /** Migrates the form metadata if it hasn’t already been done */
    @SuppressLint("ApplySharedPref")
    public static void migrate(SharedPreferences sharedPreferences) {
        boolean migrationAlreadyDone = sharedPreferences.getBoolean(PreferenceKeys.KEY_METADATA_MIGRATED, false);
        Timber.i("migrate called, %s",
                (migrationAlreadyDone ? "migration already done" : "will migrate"));

        if (! migrationAlreadyDone) {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            for (String[] pair : sourceTargetValuePairs) {
                String migratingValue = sharedPreferences.getString(pair[0], "").trim();
                if (! migratingValue.isEmpty()) {
                    Timber.i("Copying %s from %s to %s", migratingValue, pair[0], pair[1]);
                    editor.putString(pair[1], migratingValue);
                }
            }

            // Save that we’ve migrated the values
            editor.putBoolean(PreferenceKeys.KEY_METADATA_MIGRATED, true);
            editor.commit();
        }
    }
}
