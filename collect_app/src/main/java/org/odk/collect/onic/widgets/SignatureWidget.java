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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images;
//import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.onic.R;
import org.odk.collect.onic.activities.DrawActivity;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.utilities.FileUtils;
import org.odk.collect.onic.utilities.MediaUtils;
import org.odk.collect.onic.utilities.ViewIds;
import org.odk.collect.onic.widgets.interfaces.BaseImageWidget;
import org.odk.collect.onic.utilities.ApplicationConstants;

import java.io.File;

import timber.log.Timber;

/**
 * Signature widget.
 *
 * @author BehrAtherton@gmail.com
 */
public class SignatureWidget extends QuestionWidget implements BaseImageWidget {

    private Button signButton;
    private String binaryName;

    @Nullable
    private ImageView imageView;

    private TextView errorTextView;

    public SignatureWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        errorTextView = new TextView(context);
        errorTextView.setId(ViewIds.generateViewId());
        errorTextView.setText(R.string.selected_invalid_image);

        signButton = getSimpleButton(getContext().getString(R.string.sign_button));
        signButton.setEnabled(!prompt.isReadOnly());
        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "signButton", "click",
                                getFormEntryPrompt().getIndex());
                launchSignatureActivity();
            }
        });

        // finish complex layout
        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(signButton);
        answerLayout.addView(errorTextView);

        // and hide the sign button if read-only
        if (prompt.isReadOnly()) {
            signButton.setVisibility(GONE);
        }
        errorTextView.setVisibility(GONE);

        // retrieve answer from data model and update ui
        binaryName = prompt.getAnswerText();

        // Only add the imageView if the user has signed
        if (binaryName != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;

            File f = new File(getInstanceFolder() + File.separator + binaryName);

            Bitmap bmp = null;
            if (f.exists()) {
                bmp = FileUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);
                if (bmp == null) {
                    errorTextView.setVisibility(VISIBLE);
                }
            }

            imageView = getAnswerImageView(bmp);
            answerLayout.addView(imageView);
        }
        addAnswerView(answerLayout);
    }

    @Override
    public void onImageClick() {
        Collect.getInstance().getActivityLogger().logInstanceAction(this, "viewImage",
                "click", getFormEntryPrompt().getIndex());
        launchSignatureActivity();
    }

    private void launchSignatureActivity() {
        errorTextView.setVisibility(GONE);
        Intent i = new Intent(getContext(), DrawActivity.class);
        i.putExtra(DrawActivity.OPTION, DrawActivity.OPTION_SIGNATURE);
        // copy...
        if (binaryName != null) {
            File f = new File(getInstanceFolder() + File.separator + binaryName);
            i.putExtra(DrawActivity.REF_IMAGE, Uri.fromFile(f));
        }
        i.putExtra(DrawActivity.EXTRA_OUTPUT,
                Uri.fromFile(new File(Collect.TMPFILE_PATH)));

        try {
            waitForData();
            ((Activity) getContext()).startActivityForResult(i,
                    ApplicationConstants.RequestCodes.SIGNATURE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(),
                    getContext().getString(R.string.activity_not_found, "signature capture"),
                    Toast.LENGTH_SHORT).show();
            cancelWaitingForData();
        }
    }

    @Override
    public void deleteFile() {
        // get the file path and delete the file
        String name = binaryName;
        // clean up variables
        binaryName = null;
        // delete from media provider
        int del = MediaUtils.deleteImageFileFromMediaProvider(
                getInstanceFolder() + File.separator + name);
        Timber.i("Deleted %d rows from media content provider", del);
    }

    @Override
    public void clearAnswer() {
        // remove the file
        deleteFile();
        if (imageView != null) {
            imageView.setImageBitmap(null);
        }

        errorTextView.setVisibility(GONE);

        // reset buttons
        signButton.setText(getContext().getString(R.string.sign_button));
    }

    @Override
    public IAnswerData getAnswer() {
        if (binaryName != null) {
            return new StringData(binaryName);
        } else {
            return null;
        }
    }

    @Override
    public void setBinaryData(Object answer) {
        // you are replacing an answer. delete the previous image using the
        // content provider.
        if (binaryName != null) {
            deleteFile();
        }

        File newImage = (File) answer;
        if (newImage.exists()) {
            // Add the new image to the Media content provider so that the
            // viewing is fast in Android 2.0+
            ContentValues values = new ContentValues(6);
            values.put(Images.Media.TITLE, newImage.getName());
            values.put(Images.Media.DISPLAY_NAME, newImage.getName());
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(Images.Media.MIME_TYPE, "image/jpeg");
            values.put(Images.Media.DATA, newImage.getAbsolutePath());

            Uri imageURI = getContext().getContentResolver().insert(
                    Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageURI != null) {
                Timber.i("Inserting image returned uri = %s", imageURI.toString());
            }

            binaryName = newImage.getName();
            Timber.i("Setting current answer to %s", newImage.getName());
        } else {
            Timber.e("NO IMAGE EXISTS at: %s", newImage.getAbsolutePath());
        }

        cancelWaitingForData();
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
        signButton.setOnLongClickListener(l);
        if (imageView != null) {
            imageView.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        signButton.cancelLongPress();
        if (imageView != null) {
            imageView.cancelLongPress();
        }
    }
}
