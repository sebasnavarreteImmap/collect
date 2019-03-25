package org.odk.collect.onic.http.mock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.odk.collect.onic.http.HttpCredentialsInterface;
import org.odk.collect.onic.http.HttpGetResult;

import java.net.URI;

public class MockHttpClientConnectionError extends MockHttpClientConnection {

    @Override
    @NonNull
    public HttpGetResult executeGetRequest(@NonNull URI uri, @Nullable String contentType, @Nullable HttpCredentialsInterface credentials) throws Exception {
        return null;
    }
}
