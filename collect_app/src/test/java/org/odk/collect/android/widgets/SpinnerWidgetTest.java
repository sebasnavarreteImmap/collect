package org.odk.collect.android.widgets;

import android.support.annotation.NonNull;

import org.odk.collect.android.widgets.base.GeneralSelectOneWidgetTest;
import org.odk.collect.onic.widgets.SpinnerWidget;
import org.robolectric.RuntimeEnvironment;

/**
 * @author James Knight
 */

public class SpinnerWidgetTest extends GeneralSelectOneWidgetTest<SpinnerWidget> {
    @NonNull
    @Override
    public SpinnerWidget createWidget() {
        return new SpinnerWidget(RuntimeEnvironment.application, formEntryPrompt);
    }
}
