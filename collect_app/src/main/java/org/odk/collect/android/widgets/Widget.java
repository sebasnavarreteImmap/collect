package org.odk.collect.android.widgets;

import org.javarosa.core.model.data.IAnswerData;

/**
 * @author James Knight
 */
public interface Widget {

    IAnswerData getAnswer();

    void clearAnswer();
}
