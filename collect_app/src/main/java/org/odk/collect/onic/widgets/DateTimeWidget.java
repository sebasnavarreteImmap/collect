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
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.LocalDateTime;
import org.odk.collect.onic.widgets.interfaces.BinaryWidget;

/**
 * Displays a DatePicker widget. DateWidget handles leap years and does not allow dates that do not
 * exist.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("ViewConstructor")
public class DateTimeWidget extends QuestionWidget implements BinaryWidget {

    private AbstractDateWidget dateWidget;
    private TimeWidget timeWidget;

    public DateTimeWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        setGravity(Gravity.START);

        String appearance = prompt.getQuestion().getAppearanceAttr();
        if (appearance != null && appearance.contains("ethiopian")) {
            dateWidget = new EthiopianDateWidget(context, prompt);
        } else {
            dateWidget = new DateWidget(context, prompt);
        }
        timeWidget = new TimeWidget(context, prompt);

        dateWidget.getQuestionMediaLayout().getView_Text().setVisibility(GONE);
        dateWidget.getHelpTextView().setVisibility(GONE);

        timeWidget.getQuestionMediaLayout().getView_Text().setVisibility(GONE);
        timeWidget.getHelpTextView().setVisibility(GONE);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(dateWidget);
        if (!dateWidget.isDayHidden()) {
            linearLayout.addView(timeWidget);
        }
        addAnswerView(linearLayout);
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        if (dateWidget.isNullAnswer() && timeWidget.isNullAnswer()) {
            return null;
        } else {
            if (timeWidget.isNullAnswer()) {
                timeWidget.setTimeToCurrent();
                timeWidget.setTimeLabel();
            } else if (dateWidget.isNullAnswer()) {
                dateWidget.setDateToCurrent();
                dateWidget.setDateLabel();
            }

            int year = dateWidget.getDate().getYear();
            int month = dateWidget.getDate().getMonthOfYear();
            int day = dateWidget.getDate().getDayOfMonth();
            int hour = timeWidget.getHour();
            int minute = timeWidget.getMinute();

            LocalDateTime ldt = new LocalDateTime()
                    .withYear(year)
                    .withMonthOfYear(month)
                    .withDayOfMonth(day)
                    .withHourOfDay(hour)
                    .withMinuteOfHour(minute)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);

            return new DateTimeData(ldt.toDate());
        }
    }

    @Override
    public void clearAnswer() {
        dateWidget.clearAnswer();
        timeWidget.clearAnswer();
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        dateWidget.setOnLongClickListener(l);
        timeWidget.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        dateWidget.cancelLongPress();
        timeWidget.cancelLongPress();
    }

    @Override
    public void setBinaryData(Object answer) {
        dateWidget.setBinaryData(answer);
        cancelWaitingForData();
    }

    public AbstractDateWidget getDateWidget() {
        return dateWidget;
    }

    // Exposed for testing purposes to avoid reflection.
    public void setDateWidget(DateWidget dateWidget) {
        this.dateWidget = dateWidget;
    }

    // Exposed for testing purposes to avoid reflection.
    public void setTimeWidget(TimeWidget timeWidget) {
        this.timeWidget = timeWidget;
    }
}
