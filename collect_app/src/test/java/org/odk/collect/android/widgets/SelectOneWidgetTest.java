package org.odk.collect.android.widgets;

import android.support.annotation.NonNull;

import org.odk.collect.android.widgets.base.GeneralSelectOneWidgetTest;
import org.odk.collect.onic.widgets.SelectOneWidget;
import org.robolectric.RuntimeEnvironment;

/**
 * @author James Knight
 */

public class SelectOneWidgetTest extends GeneralSelectOneWidgetTest<SelectOneWidget> {

    @NonNull
    @Override
    public SelectOneWidget createWidget() {
        return new SelectOneWidget(RuntimeEnvironment.application, formEntryPrompt);
    }
}
