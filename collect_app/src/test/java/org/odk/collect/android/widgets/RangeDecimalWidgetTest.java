package org.odk.collect.android.widgets;

import android.support.annotation.NonNull;

import org.javarosa.core.model.data.DecimalData;
import org.odk.collect.android.widgets.base.RangeWidgetTest;
import org.odk.collect.onic.widgets.RangeDecimalWidget;
import org.robolectric.RuntimeEnvironment;

/**
 * @author James Knight
 */

public class RangeDecimalWidgetTest extends RangeWidgetTest<RangeDecimalWidget, DecimalData> {

    public RangeDecimalWidgetTest() {
        super();
    }

    @NonNull
    @Override
    public RangeDecimalWidget createWidget() {
        return new RangeDecimalWidget(RuntimeEnvironment.application, formEntryPrompt);
    }

    @NonNull
    @Override
    public DecimalData getNextAnswer() {
        return new DecimalData(random.nextDouble());
    }
}
