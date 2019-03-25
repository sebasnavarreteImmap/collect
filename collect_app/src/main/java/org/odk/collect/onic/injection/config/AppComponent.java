package org.odk.collect.onic.injection.config;

import android.app.Application;

import org.odk.collect.onic.activities.FormDownloadList;
import org.odk.collect.onic.activities.FormEntryActivity;
import org.odk.collect.onic.activities.InstanceUploaderList;
import org.odk.collect.onic.adapters.InstanceUploaderAdapter;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.fragments.DataManagerList;
import org.odk.collect.onic.http.CollectServerClient;
import org.odk.collect.onic.injection.ActivityBuilder;
import org.odk.collect.onic.injection.config.scopes.PerApplication;
import org.odk.collect.onic.logic.PropertyManager;
import org.odk.collect.onic.preferences.ServerPreferencesFragment;
import org.odk.collect.onic.tasks.InstanceServerUploaderTask;
import org.odk.collect.onic.tasks.sms.SmsSentBroadcastReceiver;
import org.odk.collect.onic.tasks.sms.SmsNotificationReceiver;
import org.odk.collect.onic.tasks.sms.SmsSender;
import org.odk.collect.onic.tasks.sms.SmsService;
import org.odk.collect.onic.utilities.AuthDialogUtility;
import org.odk.collect.onic.utilities.DownloadFormListUtils;
import org.odk.collect.onic.utilities.FormDownloader;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

/**
 * Primary module, bootstraps the injection system and
 * injects the main Collect instance here.
 * <p>
 * Shouldn't be modified unless absolutely necessary.
 */
@PerApplication
@Component(modules = {
        AndroidSupportInjectionModule.class,
        AppModule.class,
        ActivityBuilder.class
})
public interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder application(Application application);

        AppComponent build();
    }

    void inject(Collect collect);

    void inject(SmsService smsService);

    void inject(SmsSender smsSender);

    void inject(SmsSentBroadcastReceiver smsSentBroadcastReceiver);

    void inject(SmsNotificationReceiver smsNotificationReceiver);

    void inject(InstanceUploaderList instanceUploaderList);

    void inject(InstanceUploaderAdapter instanceUploaderAdapter);

    void inject(DataManagerList dataManagerList);

    void inject(PropertyManager propertyManager);

    void inject(FormEntryActivity formEntryActivity);

    void inject(InstanceServerUploaderTask uploader);

    void inject(CollectServerClient collectClient);

    void inject(ServerPreferencesFragment serverPreferencesFragment);

    void inject(FormDownloader formDownloader);

    void inject(DownloadFormListUtils downloadFormListUtils);

    void inject(AuthDialogUtility authDialogUtility);
  
    void inject(FormDownloadList formDownloadList);
}
