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

package org.odk.collect.onic.tasks;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.DefaultAnswerResolver;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.database.ItemsetDbAdapter;
import org.odk.collect.onic.external.ExternalAnswerResolver;
import org.odk.collect.onic.external.ExternalDataHandler;
import org.odk.collect.onic.external.ExternalDataManager;
import org.odk.collect.onic.external.ExternalDataManagerImpl;
import org.odk.collect.onic.external.ExternalDataReader;
import org.odk.collect.onic.external.ExternalDataReaderImpl;
import org.odk.collect.onic.external.handler.ExternalDataHandlerPull;
import org.odk.collect.onic.listeners.FormLoaderListener;
import org.odk.collect.onic.logic.FileReferenceFactory;
import org.odk.collect.onic.logic.FormController;
import org.odk.collect.onic.utilities.FileUtils;
import org.odk.collect.onic.utilities.ZipUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import timber.log.Timber;

/**
 * Background task for loading a form.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class FormLoaderTask extends AsyncTask<String, String, FormLoaderTask.FECWrapper> {
    private static final String ITEMSETS_CSV = "itemsets.csv";

    private FormLoaderListener stateListener;
    private String errorMsg;
    private String instancePath;
    private final String xpath;
    private final String waitingXPath;
    private boolean pendingActivityResult = false;
    private int requestCode = 0;
    private int resultCode = 0;
    private Intent intent = null;
    private ExternalDataManager externalDataManager;

    protected class FECWrapper {
        FormController controller;
        boolean usedSavepoint;

        protected FECWrapper(FormController controller, boolean usedSavepoint) {
            this.controller = controller;
            this.usedSavepoint = usedSavepoint;
        }

        protected FormController getController() {
            return controller;
        }

        protected boolean hasUsedSavepoint() {
            return usedSavepoint;
        }

        protected void free() {
            controller = null;
        }
    }

    FECWrapper data;

    public FormLoaderTask(String instancePath, String xpath, String waitingXPath) {
        this.instancePath = instancePath;
        this.xpath = xpath;
        this.waitingXPath = waitingXPath;
    }

    /**
     * Initialize {@link FormEntryController} with {@link FormDef} from binary or
     * from XML. If given an instance, it will be used to fill the {@link FormDef}.
     */
    @Override
    protected FECWrapper doInBackground(String... path) {
        errorMsg = null;

        final String formPath = path[0];
        final File formXml = new File(formPath);

        final FormDef formDef = createFormDefFromCacheOrXml(formPath, formXml);

        if (errorMsg != null || formDef == null) {
            return null;
        }

        // set paths to /sdcard/odk/forms/formfilename-media/
        final String formFileName = formXml.getName().substring(0, formXml.getName().lastIndexOf("."));
        final File formMediaDir = new File(formXml.getParent(), formFileName + "-media");

        externalDataManager = new ExternalDataManagerImpl(formMediaDir);

        // add external data function handlers
        ExternalDataHandler externalDataHandlerPull = new ExternalDataHandlerPull(
                externalDataManager);
        formDef.getEvaluationContext().addFunctionHandler(externalDataHandlerPull);

        try {
            loadExternalData(formMediaDir);
        } catch (Exception e) {
            Timber.e(e, "Exception thrown while loading external data");
            errorMsg = e.getMessage();
            return null;
        }

        if (isCancelled()) {
            // that means that the user has cancelled, so no need to go further
            return null;
        }

        // create FormEntryController from formdef
        final FormEntryModel fem = new FormEntryModel(formDef);
        final FormEntryController fec = new FormEntryController(fem);

        boolean usedSavepoint = false;

        try {
            Timber.i("Importing existing data");
            final long start = System.currentTimeMillis();
            usedSavepoint = importExistingData(formDef, fec);
            Timber.i("Imported in %.3f seconds.", (System.currentTimeMillis() - start) / 1000F);
        } catch (RuntimeException e) {
            Timber.e(e);
            if (e.getCause() instanceof XPathTypeMismatchException) {
                // this is a case of
                // https://bitbucket.org/m
                // .sundt/javarosa/commits/e5d344783e7968877402bcee11828fa55fac69de
                // the data are imported, the survey will be unusable
                // but we should give the option to the user to edit the form
                // otherwise the survey will be TOTALLY inaccessible.
                Timber.w("We have a syntactically correct instance, but the data threw an "
                                + "exception inside JR. We should allow editing.");
            } else {
                errorMsg = e.getMessage();
                return null;
            }
        }

        // Remove previous forms
        ReferenceManager.instance().clearSession();

        processItemSets(formMediaDir);

        // This should get moved to the Application Class
        if (ReferenceManager.instance().getFactories().length == 0) {
            // this is /sdcard/odk
            ReferenceManager.instance().addReferenceFactory(new FileReferenceFactory(Collect.ODK_ROOT));
        }

        // Set jr://... to point to /sdcard/odk/forms/filename-media/
        ReferenceManager.instance().addSessionRootTranslator(
                new RootTranslator("jr://images/", "jr://file/forms/" + formFileName + "-media/"));
        ReferenceManager.instance().addSessionRootTranslator(
                new RootTranslator("jr://image/", "jr://file/forms/" + formFileName + "-media/"));
        ReferenceManager.instance().addSessionRootTranslator(
                new RootTranslator("jr://audio/", "jr://file/forms/" + formFileName + "-media/"));
        ReferenceManager.instance().addSessionRootTranslator(
                new RootTranslator("jr://video/", "jr://file/forms/" + formFileName + "-media/"));

        final FormController fc = new FormController(formMediaDir, fec, instancePath == null ? null
                : new File(instancePath));
        if (xpath != null) {
            // we are resuming after having terminated -- set index to this
            // position...
            FormIndex idx = fc.getIndexFromXPath(xpath);
            fc.jumpToIndex(idx);
        }
        if (waitingXPath != null) {
            FormIndex idx = fc.getIndexFromXPath(waitingXPath);
            fc.setIndexWaitingForData(idx);
        }
        data = new FECWrapper(fc, usedSavepoint);
        return data;
    }

    private FormDef createFormDefFromCacheOrXml(String formPath, File formXml) {
        final String formHash = FileUtils.getMd5Hash(formXml);

        publishProgress(
                Collect.getInstance().getString(R.string.survey_loading_reading_form_message));

        final File cachedForm = new File(Collect.CACHE_PATH + File.separator + formHash + ".formdef");
        if (cachedForm.exists()) {
            Timber.i("Attempting to load %s from cached file: %s.",
                    formXml.getName(), cachedForm.getAbsolutePath());
            final long start = System.currentTimeMillis();
            final FormDef deserializedFormDef = deserializeFormDef(cachedForm);
            if (deserializedFormDef != null) {
                Timber.i("Loaded in %.3f seconds.", (System.currentTimeMillis() - start) / 1000F);

                return deserializedFormDef;
            }

            // An error occurred with deserialization. Remove the file, and make a
            // new .formdef from xml.
            Timber.w("Deserialization FAILED! Deleting cache file: %s",
                    cachedForm.getAbsolutePath());
            cachedForm.delete();
        }

        FileInputStream fis = null;
        // no binary, read from xml
        try {
            Timber.i("Attempting to load from: %s", formXml.getAbsolutePath());
            final long start = System.currentTimeMillis();
            fis = new FileInputStream(formXml);
            FormDef formDefFromXml = XFormUtils.getFormFromInputStream(fis);
            if (formDefFromXml == null) {
                errorMsg = "Error reading XForm file";
            } else {
                Timber.i("Loaded in %.3f seconds. Now saving to cache.",
                        (System.currentTimeMillis() - start) / 1000F);
                final long start2 = System.currentTimeMillis();
                serializeFormDef(formDefFromXml, formPath);
                Timber.i("Saved to cache in %.3f seconds.",
                        (System.currentTimeMillis() - start2) / 1000F);
                return formDefFromXml;
            }
        } catch (Exception e) {
            Timber.e(e);
            errorMsg = e.getMessage();
        } finally {
            IOUtils.closeQuietly(fis);
        }

        return null;
    }

    private void processItemSets(File formMediaDir) {
        // for itemsets.csv, we only check to see if the itemset file has been
        // updated
        final File csv = new File(formMediaDir.getAbsolutePath() + "/" + ITEMSETS_CSV);
        String csvmd5 = null;
        if (csv.exists()) {
            csvmd5 = FileUtils.getMd5Hash(csv);
            boolean readFile = false;
            final ItemsetDbAdapter ida = new ItemsetDbAdapter();
            ida.open();
            // get the database entry (if exists) for this itemsets.csv, based
            // on the path
            final Cursor c = ida.getItemsets(csv.getAbsolutePath());
            if (c != null) {
                if (c.getCount() == 1) {
                    c.moveToFirst(); // should be only one, ever, if any
                    final String oldmd5 = c.getString(c.getColumnIndex("hash"));
                    if (oldmd5.equals(csvmd5)) {
                        // they're equal, do nothing
                    } else {
                        // the csv has been updated, delete the old entries
                        ida.dropTable(ItemsetDbAdapter.getMd5FromString(csv.getAbsolutePath()),
                                csv.getAbsolutePath());
                        // and read the new
                        readFile = true;
                    }
                } else {
                    // new csv, add it
                    readFile = true;
                }
                c.close();
            }
            ida.close();
            if (readFile) {
                readCSV(csv, csvmd5, ItemsetDbAdapter.getMd5FromString(csv.getAbsolutePath()));
            }
        }
    }

    private boolean importExistingData(FormDef formDef, FormEntryController fec) {
        final InstanceInitializationFactory instanceInit = new InstanceInitializationFactory();
        boolean usedSavepoint = false;
        if (instancePath != null) {
            File instance = new File(instancePath);
            final File shadowInstance = SaveToDiskTask.savepointFile(instance);
            if (shadowInstance.exists() && (shadowInstance.lastModified()
                    > instance.lastModified())) {
                // the savepoint is newer than the saved value of the instance.
                // use it.
                usedSavepoint = true;
                instance = shadowInstance;
                Timber.w("Loading instance from shadow file: %s", shadowInstance.getAbsolutePath());
            }
            if (instance.exists()) {
                // This order is important. Import data, then initialize.
                try {
                    importData(instance, fec);
                    formDef.initialize(false, instanceInit);
                } catch (RuntimeException e) {
                    Timber.e(e);

                    // SCTO-633
                    if (usedSavepoint
                            && !(e.getCause() instanceof XPathTypeMismatchException)) {
                        // this means that the .save file is corrupted or 0-sized, so
                        // don't use it.
                        usedSavepoint = false;
                        instancePath = null;
                        formDef.initialize(true, instanceInit);
                    } else {
                        // this means that the saved instance is corrupted.
                        throw e;
                    }
                }
            } else {
                formDef.initialize(true, instanceInit);
            }
        } else {
            formDef.initialize(true, instanceInit);
        }
        return usedSavepoint;
    }

    @SuppressWarnings("unchecked")
    private void loadExternalData(File mediaFolder) {
        // SCTO-594
        File[] zipFiles = mediaFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().toLowerCase(Locale.US).endsWith(".zip");
            }
        });

        if (zipFiles != null) {
            ZipUtils.unzip(zipFiles);
            for (File zipFile : zipFiles) {
                boolean deleted = zipFile.delete();
                if (!deleted) {
                    Timber.w("Cannot delete %s. It will be re-unzipped next time. :(", zipFile.toString());
                }
            }
        }

        File[] csvFiles = mediaFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String lowerCaseName = file.getName().toLowerCase(Locale.US);
                return lowerCaseName.endsWith(".csv") && !lowerCaseName.equalsIgnoreCase(
                        ITEMSETS_CSV);
            }
        });

        Map<String, File> externalDataMap = new HashMap<String, File>();

        if (csvFiles != null) {

            for (File csvFile : csvFiles) {
                String dataSetName = csvFile.getName().substring(0,
                        csvFile.getName().lastIndexOf("."));
                externalDataMap.put(dataSetName, csvFile);
            }

            if (externalDataMap.size() > 0) {

                publishProgress(Collect.getInstance()
                        .getString(R.string.survey_loading_reading_csv_message));

                ExternalDataReader externalDataReader = new ExternalDataReaderImpl(this);
                externalDataReader.doImport(externalDataMap);
            }
        }
    }

    public void publishExternalDataLoadingProgress(String message) {
        publishProgress(message);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        synchronized (this) {
            if (stateListener != null && values != null) {
                if (values.length == 1) {
                    stateListener.onProgressStep(values[0]);
                }
            }
        }
    }

    private boolean importData(File instanceFile, FormEntryController fec) {
        publishProgress(
                Collect.getInstance().getString(R.string.survey_loading_reading_data_message));

        // convert files into a byte array
        byte[] fileBytes = FileUtils.getFileAsBytes(instanceFile);

        // get the root of the saved and template instances
        TreeElement savedRoot = XFormParser.restoreDataModel(fileBytes, null).getRoot();
        TreeElement templateRoot = fec.getModel().getForm().getInstance().getRoot().deepCopy(true);

        // weak check for matching forms
        if (!savedRoot.getName().equals(templateRoot.getName()) || savedRoot.getMult() != 0) {
            Timber.e("Saved form instance does not match template form definition");
            return false;
        } else {
            // populate the data model
            TreeReference tr = TreeReference.rootRef();
            tr.add(templateRoot.getName(), TreeReference.INDEX_UNBOUND);

            // Here we set the Collect's implementation of the IAnswerResolver.
            // We set it back to the default after select choices have been populated.
            XFormParser.setAnswerResolver(new ExternalAnswerResolver());
            templateRoot.populate(savedRoot, fec.getModel().getForm());
            XFormParser.setAnswerResolver(new DefaultAnswerResolver());

            // populated model to current form
            fec.getModel().getForm().getInstance().setRoot(templateRoot);

            // fix any language issues
            // :
            // http://bitbucket.org/javarosa/main/issue/5/itext-n-appearing-in-restored-instances
            if (fec.getModel().getLanguages() != null) {
                fec.getModel().getForm()
                        .localeChanged(fec.getModel().getLanguage(),
                                fec.getModel().getForm().getLocalizer());
            }

            return true;

        }
    }

    /**
     * Read serialized {@link FormDef} from file and recreate as object.
     *
     * @param formDef serialized FormDef file
     * @return {@link FormDef} object
     */
    public FormDef deserializeFormDef(File formDef) {

        // TODO: any way to remove reliance on jrsp?
        FileInputStream fis = null;
        FormDef fd = null;
        try {
            // create new form def
            fd = new FormDef();
            fis = new FileInputStream(formDef);
            DataInputStream dis = new DataInputStream(fis);

            // read serialized formdef into new formdef
            fd.readExternal(dis, ExtUtil.defaultPrototypes());
            dis.close();

        } catch (Exception e) {
            Timber.e(e);
            fd = null;
        }

        return fd;
    }

    /**
     * Write the FormDef to the file system as a binary blog.
     *
     * @param filepath path to the form file
     */
    public void serializeFormDef(FormDef fd, String filepath) {
        // calculate unique md5 identifier
        String hash = FileUtils.getMd5Hash(new File(filepath));
        File formDef = new File(Collect.CACHE_PATH + File.separator + hash + ".formdef");

        // formdef does not exist, create one.
        if (!formDef.exists()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(formDef);
                DataOutputStream dos = new DataOutputStream(fos);
                fd.writeExternal(dos);
                dos.flush();
                dos.close();
            } catch (IOException e) {
                Timber.e(e);
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        if (externalDataManager != null) {
            externalDataManager.close();
        }
    }

    @Override
    protected void onPostExecute(FECWrapper wrapper) {
        synchronized (this) {
            try {
                if (stateListener != null) {
                    if (wrapper == null) {
                        stateListener.loadingError(errorMsg);
                    } else {
                        stateListener.loadingComplete(this);
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    public void setFormLoaderListener(FormLoaderListener sl) {
        synchronized (this) {
            stateListener = sl;
        }
    }

    public FormController getFormController() {
        return (data != null) ? data.getController() : null;
    }

    public ExternalDataManager getExternalDataManager() {
        return externalDataManager;
    }

    public boolean hasUsedSavepoint() {
        return (data != null) && data.hasUsedSavepoint();
    }

    public void destroy() {
        if (data != null) {
            data.free();
            data = null;
        }
    }

    public boolean hasPendingActivityResult() {
        return pendingActivityResult;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public int getResultCode() {
        return resultCode;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setActivityResult(int requestCode, int resultCode, Intent intent) {
        this.pendingActivityResult = true;
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.intent = intent;
    }

    private void readCSV(File csv, String formHash, String pathHash) {

        CSVReader reader;
        ItemsetDbAdapter ida = new ItemsetDbAdapter();
        ida.open();
        boolean withinTransaction = false;

        try {
            reader = new CSVReader(new FileReader(csv));

            String[] nextLine;
            String[] columnHeaders = null;
            int lineNumber = 0;
            while ((nextLine = reader.readNext()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    // first line of csv is column headers
                    columnHeaders = nextLine;
                    ida.createTable(formHash, pathHash, columnHeaders,
                            csv.getAbsolutePath());
                    continue;
                }
                // add the rest of the lines to the specified database
                // nextLine[] is an array of values from the line
                // System.out.println(nextLine[4] + "etc...");
                if (lineNumber == 2) {
                    // start a transaction for the inserts
                    withinTransaction = true;
                    ida.beginTransaction();
                }
                ida.addRow(pathHash, columnHeaders, nextLine);

            }
        } catch (IOException e) {
            Timber.e(e, "Exception thrown while reading csv file");
        } finally {
            if (withinTransaction) {
                ida.commit();
            }
            ida.close();
        }
    }
}
