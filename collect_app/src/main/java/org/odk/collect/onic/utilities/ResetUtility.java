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

package org.odk.collect.onic.utilities;

import android.content.Context;
import android.preference.PreferenceManager;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.dao.FormsDao;
import org.odk.collect.onic.dao.InstancesDao;
import org.odk.collect.onic.database.ItemsetDbAdapter;
import org.odk.collect.onic.preferences.AdminPreferencesActivity;
import org.osmdroid.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResetUtility {

    private List<Integer> failedResetActions;

    public List<Integer> reset(final Context context, List<Integer> resetActions) {

        failedResetActions = new ArrayList<>();
        failedResetActions.addAll(resetActions);

        for (int action : resetActions) {
            switch (action) {
                case ResetAction.RESET_PREFERENCES:
                    resetPreferences(context);
                    break;
                case ResetAction.RESET_INSTANCES:
                    resetInstances();
                    break;
                case ResetAction.RESET_FORMS:
                    resetForms();
                    break;
                case ResetAction.RESET_LAYERS:
                    if (deleteFolderContents(Collect.OFFLINE_LAYERS)) {
                        failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_LAYERS));
                    }
                    break;
                case ResetAction.RESET_CACHE:
                    if (deleteFolderContents(Collect.CACHE_PATH)) {
                        failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_CACHE));
                    }
                    break;
                case ResetAction.RESET_OSM_DROID:
                    if (deleteFolderContents(Configuration.getInstance().getOsmdroidTileCache().getPath())) {
                        failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_OSM_DROID));
                    }
                    break;
            }
        }

        return failedResetActions;
    }

    private void resetPreferences(Context context) {
        boolean clearedDefaultPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .commit();

        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        boolean clearedAdminPreferences = context
                .getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0)
                .edit()
                .clear()
                .commit();

        boolean deletedSettingsFolderContest = !new File(Collect.SETTINGS).exists()
                || deleteFolderContents(Collect.SETTINGS);

        boolean deletedSettingsFile = !new File(Collect.ODK_ROOT + "/collect.settings").exists()
                || (new File(Collect.ODK_ROOT + "/collect.settings").delete());

        if (clearedDefaultPreferences && clearedAdminPreferences && deletedSettingsFolderContest && deletedSettingsFile) {
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_PREFERENCES));
        }
    }

    private void resetInstances() {
        new InstancesDao().deleteInstancesDatabase();

        if (deleteFolderContents(Collect.INSTANCES_PATH)) {
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_INSTANCES));
        }
    }

    private void resetForms() {
        new FormsDao().deleteFormsDatabase();

        File itemsetDbFile = new File(Collect.METADATA_PATH + File.separator + ItemsetDbAdapter.DATABASE_NAME);

        if (deleteFolderContents(Collect.FORMS_PATH) && (!itemsetDbFile.exists() || itemsetDbFile.delete())) {
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_FORMS));
        }
    }

    private boolean deleteFolderContents(String path) {
        boolean result = true;
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();

            for (File f : files) {
                result = deleteRecursive(f);
            }
        }
        return result;
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        return fileOrDirectory.delete();
    }

    public static class ResetAction {
        public static final int RESET_PREFERENCES = 0;
        public static final int RESET_INSTANCES = 1;
        public static final int RESET_FORMS = 2;
        public static final int RESET_LAYERS = 3;
        public static final int RESET_CACHE = 4;
        public static final int RESET_OSM_DROID = 5;
    }
}
