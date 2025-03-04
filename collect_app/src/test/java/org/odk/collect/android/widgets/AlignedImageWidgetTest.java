package org.odk.collect.android.widgets;

import android.support.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.data.StringData;
import org.junit.Before;
import org.mockito.Mock;
import org.odk.collect.android.widgets.base.FileWidgetTest;
import org.odk.collect.onic.widgets.AlignedImageWidget;
import org.robolectric.RuntimeEnvironment;

import java.io.File;

import static org.mockito.Mockito.when;

/**
 * @author James Knight
 */
public class AlignedImageWidgetTest extends FileWidgetTest<AlignedImageWidget> {

    @Mock
    File file;

    @NonNull
    @Override
    public AlignedImageWidget createWidget() {
        return new AlignedImageWidget(RuntimeEnvironment.application, formEntryPrompt);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Override
    public Object createBinaryData(StringData answerData) {
        when(file.exists()).thenReturn(true);
        when(file.getName()).thenReturn(answerData.getDisplayText());

        return file;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        when(formEntryPrompt.getAppearanceHint()).thenReturn("0");
    }
}