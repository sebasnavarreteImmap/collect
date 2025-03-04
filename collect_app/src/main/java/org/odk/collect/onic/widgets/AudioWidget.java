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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore.Audio;
//import android.support.annotation.NonNull;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.utilities.FileUtil;
import org.odk.collect.onic.utilities.MediaUtil;
import org.odk.collect.onic.widgets.interfaces.FileWidget;
import org.odk.collect.onic.utilities.ApplicationConstants;

import java.io.File;

import timber.log.Timber;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the
 * form.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("ViewConstructor")
public class AudioWidget extends QuestionWidget implements FileWidget {

    @NonNull
    private FileUtil fileUtil;

    @NonNull
    private MediaUtil mediaUtil;

    private Button captureButton;
    private Button playButton;
    private Button chooseButton;

    private String binaryName;

    public AudioWidget(Context context, FormEntryPrompt prompt) {
        this(context, prompt, new FileUtil(), new MediaUtil());
    }

    AudioWidget(Context context, FormEntryPrompt prompt, @NonNull FileUtil fileUtil, @NonNull MediaUtil mediaUtil) {
        super(context, prompt);

        this.fileUtil = fileUtil;
        this.mediaUtil = mediaUtil;

        captureButton = getSimpleButton(getContext().getString(R.string.capture_audio), R.id.capture_audio);
        captureButton.setEnabled(!prompt.isReadOnly());
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "captureButton", "click",
                                getFormEntryPrompt().getIndex());
                Intent i = new Intent(
                        android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                i.putExtra(
                        android.provider.MediaStore.EXTRA_OUTPUT,
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                .toString());
                try {
                    waitForData();
                    ((Activity) getContext()).startActivityForResult(i,
                            ApplicationConstants.RequestCodes.AUDIO_CAPTURE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            getContext(),
                            getContext().getString(R.string.activity_not_found,
                                    "audio capture"), Toast.LENGTH_SHORT)
                            .show();
                    cancelWaitingForData();
                }

            }
        });

        chooseButton = getSimpleButton(getContext().getString(R.string.choose_sound), R.id.choose_sound);
        chooseButton.setEnabled(!prompt.isReadOnly());
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "chooseButton", "click",
                                getFormEntryPrompt().getIndex());
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("audio/*");
                try {
                    waitForData();
                    ((Activity) getContext()).startActivityForResult(i,
                            ApplicationConstants.RequestCodes.AUDIO_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            getContext(),
                            getContext().getString(R.string.activity_not_found,
                                    "choose audio"), Toast.LENGTH_SHORT).show();
                    cancelWaitingForData();
                }

            }
        });

        playButton = getSimpleButton(getContext().getString(R.string.play_audio), R.id.play_audio);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance()
                        .getActivityLogger()
                        .logInstanceAction(this, "playButton", "click",
                                getFormEntryPrompt().getIndex());
                Intent i = new Intent("android.intent.action.VIEW");
                File f = new File(getInstanceFolder() + File.separator
                        + binaryName);
                i.setDataAndType(Uri.fromFile(f), "audio/*");
                try {
                    getContext().startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            getContext(),
                            getContext().getString(R.string.activity_not_found,
                                    "play audio"), Toast.LENGTH_SHORT).show();
                }

            }
        });

        // retrieve answer from data model and update ui
        binaryName = prompt.getAnswerText();
        if (binaryName != null) {
            playButton.setEnabled(true);
        } else {
            playButton.setEnabled(false);
        }

        // finish complex layout
        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(captureButton);
        answerLayout.addView(chooseButton);
        answerLayout.addView(playButton);
        addAnswerView(answerLayout);

        // and hide the capture and choose button if read-only
        if (getFormEntryPrompt().isReadOnly()) {
            captureButton.setVisibility(View.GONE);
            chooseButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void deleteFile() {
        // get the file path and delete the file
        String name = binaryName;
        // clean up variables
        binaryName = null;
        // delete from media provider
        int del = mediaUtil.deleteAudioFileFromMediaProvider(
                getInstanceFolder() + File.separator + name);
        Timber.i("Deleted %d rows from media content provider", del);
    }

    @Override
    public void clearAnswer() {
        // remove the file
        deleteFile();


        // reset buttons
        playButton.setEnabled(false);
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
    public void setBinaryData(Object binaryuri) {
        if (binaryuri == null || !(binaryuri instanceof Uri)) {
            Timber.w("AudioWidget's setBinaryData must receive a Uri object.");
            return;
        }

        Uri uri = (Uri) binaryuri;

        // get the file path and create a copy in the instance folder
        String sourcePath = getSourcePathFromUri(uri);
        String destinationPath = getDestinationPathFromSourcePath(sourcePath);

        File source = fileUtil.getFileAtPath(sourcePath);
        File newAudio = fileUtil.getFileAtPath(destinationPath);

        fileUtil.copyFile(source, newAudio);

        if (newAudio.exists()) {
            // Add the copy to the content provier
            ContentValues values = new ContentValues(6);
            values.put(Audio.Media.TITLE, newAudio.getName());
            values.put(Audio.Media.DISPLAY_NAME, newAudio.getName());
            values.put(Audio.Media.DATE_ADDED, System.currentTimeMillis());
            values.put(Audio.Media.DATA, newAudio.getAbsolutePath());

            Uri audioURI = getContext().getContentResolver().insert(
                    Audio.Media.EXTERNAL_CONTENT_URI, values);

            if (audioURI != null) {
                Timber.i("Inserting AUDIO returned uri = %s", audioURI.toString());
            }

            // when replacing an answer. remove the current media.
            if (binaryName != null && !binaryName.equals(newAudio.getName())) {
                deleteFile();
            }

            binaryName = newAudio.getName();
            Timber.i("Setting current answer to %s", newAudio.getName());
        } else {
            Timber.e("Inserting Audio file FAILED");
        }

        cancelWaitingForData();
    }

    private String getSourcePathFromUri(@NonNull Uri uri) {
        return mediaUtil.getPathFromUri(getContext(), uri, Audio.Media.DATA);
    }

    private String getDestinationPathFromSourcePath(@NonNull String sourcePath) {
        String extension = sourcePath.substring(sourcePath.lastIndexOf('.'));
        return getInstanceFolder() + File.separator
                + fileUtil.getRandomFilename() + extension;
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        captureButton.setOnLongClickListener(l);
        chooseButton.setOnLongClickListener(l);
        playButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        captureButton.cancelLongPress();
        chooseButton.cancelLongPress();
        playButton.cancelLongPress();
    }
}
