package org.odk.collect.onic.utilities;

import android.support.annotation.Nullable;

import timber.log.Timber;

public class ObjectUtils {
    @SuppressWarnings("unchecked")
    @Nullable
    public static  <T> T uncheckedCast(Object object) {
        try {
            return (T) object;

        } catch (ClassCastException e) {
            Timber.e("Object %s could not be cast.", object);
            return null;
        }
    }
}
