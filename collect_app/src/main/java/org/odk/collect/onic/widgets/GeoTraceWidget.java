/*
 * Copyright (C) 2015 GeoODK
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

package org.odk.collect.onic.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.onic.R;
import org.odk.collect.onic.activities.GeoTraceGoogleMapActivity;
import org.odk.collect.onic.activities.GeoTraceOsmMapActivity;
import org.odk.collect.onic.preferences.PreferenceKeys;
import org.odk.collect.onic.utilities.PlayServicesUtil;
import org.odk.collect.onic.widgets.interfaces.BinaryWidget;

import static org.odk.collect.onic.utilities.ApplicationConstants.RequestCodes;

/**
 * GeoShapeTrace is the widget that allows the user to get Collect multiple GPS points based on the
 * locations.
 * <p>
 * Date
 *
 * @author Jon Nordling (jonnordling@gmail.com)
 */

@SuppressLint("ViewConstructor")
public class GeoTraceWidget extends QuestionWidget implements BinaryWidget {

    public static final String GOOGLE_MAP_KEY = "google_maps";
    public static final String TRACE_LOCATION = "gp";

    public SharedPreferences sharedPreferences;
    public String mapSDK;

    private Button createTraceButton;
    private TextView answerDisplay;

    public GeoTraceWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mapSDK = sharedPreferences.getString(PreferenceKeys.KEY_MAP_SDK, GOOGLE_MAP_KEY);

        answerDisplay = getCenteredAnswerTextView();

        createTraceButton = getSimpleButton(getContext().getString(R.string.get_trace));
        createTraceButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                waitForData();
                startGeoTraceActivity();
            }
        });

        if (prompt.isReadOnly()) {
            createTraceButton.setEnabled(false);
        }

        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(createTraceButton);
        answerLayout.addView(answerDisplay);
        addAnswerView(answerLayout);

        boolean dataAvailable = false;
        String s = prompt.getAnswerText();
        if (s != null && !s.equals("")) {
            dataAvailable = true;
            setBinaryData(s);
        }

        updateButtonLabelsAndVisibility(dataAvailable);
    }

    private void startGeoTraceActivity() {
        Intent i;
        if (mapSDK.equals(GOOGLE_MAP_KEY)) {
            if (PlayServicesUtil.isGooglePlayServicesAvailable(getContext())) {
                i = new Intent(getContext(), GeoTraceGoogleMapActivity.class);
            } else {
                PlayServicesUtil.showGooglePlayServicesAvailabilityErrorDialog(getContext());
                return;
            }
        } else {
            i = new Intent(getContext(), GeoTraceOsmMapActivity.class);
        }
        String s = answerDisplay.getText().toString();
        if (s.length() != 0) {
            i.putExtra(TRACE_LOCATION, s);
        }
        ((Activity) getContext()).startActivityForResult(i, RequestCodes.GEOTRACE_CAPTURE);
    }

    private void updateButtonLabelsAndVisibility(boolean dataAvailable) {
        if (dataAvailable) {
            createTraceButton.setText(
                    getContext().getString(R.string.geotrace_view_change_location));
        } else {
            createTraceButton.setText(getContext().getString(R.string.get_trace));
        }
    }

    @Override
    public void setBinaryData(Object answer) {
        answerDisplay.setText(answer.toString());
        cancelWaitingForData();
    }

    @Override
    public IAnswerData getAnswer() {
        String s = answerDisplay.getText().toString();
        return !s.equals("")
                ? new StringData(s)
                : null;
    }

    @Override
    public void clearAnswer() {
        answerDisplay.setText(null);
        updateButtonLabelsAndVisibility(false);
    }

    @Override
    public void setFocus(Context context) {
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        createTraceButton.setOnLongClickListener(l);
        answerDisplay.setOnLongClickListener(l);
    }
}