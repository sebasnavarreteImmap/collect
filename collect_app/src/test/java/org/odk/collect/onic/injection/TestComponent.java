package org.odk.collect.onic.injection;

import android.app.Application;

import org.odk.collect.onic.http.CollectServerClientTest;
import org.odk.collect.onic.injection.config.AppComponent;
import org.odk.collect.onic.injection.config.scopes.PerApplication;
import org.odk.collect.onic.sms.SmsSenderJobTest;
import org.odk.collect.onic.sms.SmsServiceTest;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@PerApplication
@Component(modules = {
        AndroidSupportInjectionModule.class,
        TestModule.class,
        ActivityBuilder.class
})
public interface TestComponent extends AppComponent {
    @Component.Builder
    interface Builder {

        @BindsInstance
        TestComponent.Builder application(Application application);

        TestComponent build();
    }

    void inject(SmsSenderJobTest smsSenderJobTest);

    void inject(SmsServiceTest smsServiceTest);

    void inject(CollectServerClientTest collectServerClientTest);
}
