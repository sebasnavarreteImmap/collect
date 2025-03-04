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

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class FormEntryPromptUtils {

    public static String getAnswerText(FormEntryPrompt fep, Context context) {
        IAnswerData data = fep.getAnswerValue();
        String text;
        if (data instanceof DateTimeData) {
            text = DateTimeUtils.getDateTimeLabel((Date) data.getValue(),
                    DateTimeUtils.getDatePickerDetails(fep.getQuestion().getAppearanceAttr()), true, context);
        } else if (data instanceof DateData) {
            text = DateTimeUtils.getDateTimeLabel((Date) data.getValue(),
                    DateTimeUtils.getDatePickerDetails(fep.getQuestion().getAppearanceAttr()), false, context);
        } else {
            text = fep.getAnswerText();
        }

        return text;
    }

        //agregado Jorge devuelve valor de la respuesta
    public static Serializable getAnswerValue(FormEntryPrompt fep, Context context) {
        IAnswerData data = fep.getAnswerValue();
        Serializable text = "";

        if(!fep.isReadOnly() && data  != null){
            if(data.getValue().getClass().getName() == "ArrayList"){
                text = (ArrayList) data.getValue();

            }else{
                text = (String) data.getValue();
            }

        }
        return text;

    }
}
