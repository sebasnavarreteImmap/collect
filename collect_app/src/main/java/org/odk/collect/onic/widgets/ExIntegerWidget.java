/*
 * Copyright (C) 2012 University of Washington
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.external.ExternalAppsUtils;

import java.util.Locale;

import static org.odk.collect.onic.utilities.ApplicationConstants.RequestCodes;

/**
 * Launch an external app to supply an integer value. If the app
 * does not launch, enable the text area for regular data entry.
 * <p>
 * See {@link ExStringWidget} for usage.
 *
 * @author mitchellsundt@gmail.com
 */
public class ExIntegerWidget extends ExStringWidget {

    public ExIntegerWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        answer.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);

        // only allows numbers and no periods
        answer.setKeyListener(new DigitsKeyListener(true, false));

        // ints can only hold 2,147,483,648. we allow 999,999,999
        InputFilter[] fa = new InputFilter[1];
        fa[0] = new InputFilter.LengthFilter(9);
        answer.setFilters(fa);

        Integer i = getIntegerAnswerValue();

        if (i != null) {
            answer.setText(String.format(Locale.US, "%d", i));
        }
    }

    private Integer getIntegerAnswerValue() {
        IAnswerData dataHolder = getFormEntryPrompt().getAnswerValue();
        Integer d = null;
        if (dataHolder != null) {
            Object dataValue = dataHolder.getValue();
            if (dataValue != null) {
                if (dataValue instanceof Double) {
                    d = ((Double) dataValue).intValue();
                } else {
                    d = (Integer) dataValue;
                }
            }
        }
        return d;
    }

    @Override
    protected void fireActivity(Intent i) throws ActivityNotFoundException {
        i.putExtra("value", getIntegerAnswerValue());
        Collect.getInstance().getActivityLogger().logInstanceAction(this, "launchIntent",
                i.getAction(), getFormEntryPrompt().getIndex());

        ((Activity) getContext()).startActivityForResult(i,
                RequestCodes.EX_INT_CAPTURE);
    }


    @Override
    public IAnswerData getAnswer() {
        String s = answer.getText().toString();
        if (s.equals("")) {
            return null;
        } else {
            try {
                return new IntegerData(Integer.parseInt(s));
            } catch (Exception numberFormatException) {
                return null;
            }
        }
    }


    /**
     * Allows answer to be set externally in @link FormEntryActivity.
     */
    @Override
    public void setBinaryData(Object answer) {
        IntegerData integerData = ExternalAppsUtils.asIntegerData(answer);
        this.answer.setText(integerData == null ? null : integerData.getValue().toString());
        cancelWaitingForData();
    }

}
