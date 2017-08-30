package org.odk.collect.android.utilities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sanjeev on 17/3/17.
 */

public class CustomTabHelper {
    private static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    private CustomTabsClient customTabsClient;
    private CustomTabsSession customTabsSession;

    /*
     * unbind 'serviceConnection' after the context in which it was run is destroyed to
     * prevent the leakage of service
     */
    private CustomTabsServiceConnection serviceConnection;

    public void bindCustomTabsService(final Context context, final Uri url) {
        if (customTabsClient != null) {
            return;
        }
        serviceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                CustomTabHelper.this.customTabsClient = customTabsClient;
                CustomTabHelper.this.customTabsClient.warmup(0L);
                customTabsSession = CustomTabHelper.this.customTabsClient.newSession(null);
                customTabsSession.mayLaunchUrl(getNonNullUri(url), null, null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                customTabsClient = null;
            }
        };
        CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME, serviceConnection);
    }

    /**
     * Code from https://developer.chrome.com/multidevice/android/customtabs
     * For more information see
     * http://stackoverflow.com/a/33281092/137744
     * https://medium.com/google-developers/best-practices-for-custom-tabs-5700e55143ee
     */

    private List<String> getPackageName(Context context) {
        // Get default VIEW intent handler that can view a web url.
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.test-url.com"));

        // Get all apps that can handle VIEW intents.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }
        return packagesSupportingCustomTabs;
    }

    // https://github.com/opendatakit/collect/issues/1221
    private Uri getNonNullUri(Uri url) {
        return url != null ? url : Uri.parse("");
    }

    public CustomTabsServiceConnection getServiceConnection() {
        return serviceConnection;
    }

    public void openUri(Context context, Uri uri) {
        if (getPackageName(context).size() != 0) {
            //open in chrome custom tab
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.intent.setPackage(getPackageName(context).get(0));
            customTabsIntent.launchUrl(context, uri);
        } else {
            //open in an external browser
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }
}
