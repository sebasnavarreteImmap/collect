package org.odk.collect.android.widgets;

import android.support.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.data.StringData;
import org.junit.Before;
import org.mockito.Mock;
import org.odk.collect.android.widgets.base.FileWidgetTest;
import org.odk.collect.onic.widgets.ImageWebViewWidget;
import org.robolectric.RuntimeEnvironment;

import java.io.File;

import static org.mockito.Mockito.when;

/**
 * @author James Knight
 */
public class ImageWebViewWidgetTest extends FileWidgetTest<ImageWebViewWidget> {

    @Mock
    File file;

    private String fileName = null;

    public ImageWebViewWidgetTest() {
        super();
    }

    @NonNull
    @Override
    public ImageWebViewWidget createWidget() {
        return new ImageWebViewWidget(RuntimeEnvironment.application, formEntryPrompt);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(fileName);
    }

    @Override
    public Object createBinaryData(StringData answerData) {
        return file;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        fileName = RandomString.make();
    }

    @Override
    protected void prepareForSetAnswer() {
        when(file.exists()).thenReturn(true);
        when(file.getName()).thenReturn(fileName);
    }
}