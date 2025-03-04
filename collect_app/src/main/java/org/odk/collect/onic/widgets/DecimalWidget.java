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

package org.odk.collect.onic.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.widget.EditText;

import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A widget that restricts values to floating point numbers.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@SuppressLint("ViewConstructor")
public class DecimalWidget extends StringWidget {

    public DecimalWidget(Context context, FormEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt, readOnlyOverride, true);

        // formatting
        EditText answerText = getAnswerTextField();

        answerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getAnswerFontSize());
        answerText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // needed to make long readonly text scroll
        answerText.setHorizontallyScrolling(false);
        answerText.setSingleLine(false);

        // only numbers are allowed
        answerText.setKeyListener(new DigitsKeyListener(true, true));

        // only 15 characters allowed
        InputFilter[] fa = new InputFilter[1];
        fa[0] = new InputFilter.LengthFilter(15);
        answerText.setFilters(fa);

        Double d = getDoubleAnswerValue();

        if (d != null) {
            // truncate to 15 digits max in US locale
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            nf.setMaximumFractionDigits(15);
            nf.setMaximumIntegerDigits(15);
            nf.setGroupingUsed(false);

            String formattedValue = nf.format(d);
            answerText.setText(formattedValue);

            Selection.setSelection(answerText.getText(), answerText.getText().length());
        }

        // disable if read only
        if (prompt.isReadOnly()) {
            setBackground(null);
            setFocusable(false);
            setClickable(false);
        }

        setupChangeListener();
    }

    private Double getDoubleAnswerValue() {
        IAnswerData dataHolder = getFormEntryPrompt().getAnswerValue();
        Double d = null;
        if (dataHolder != null) {
            Object dataValue = dataHolder.getValue();
            if (dataValue != null) {
                if (dataValue instanceof Integer) {
                    d = (double) (Integer) dataValue;
                } else {
                    d = (Double) dataValue;
                }
            }
        }
        return d;
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();
        String s = getAnswerTextField().getText().toString();
        if (s.isEmpty()) {
            return null;

        } else {
            try {
                return new DecimalData(Double.parseDouble(s));

            } catch (Exception numberFormatException) {
                return null;
            }
        }
    }

}
