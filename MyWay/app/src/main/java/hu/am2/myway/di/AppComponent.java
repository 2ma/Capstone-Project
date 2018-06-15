package hu.am2.myway.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import hu.am2.myway.App;

@Singleton
@Component(modules = {AndroidInjectionModule.class, ActivityModule.class, AppModule.class, ServiceModule.class,
    ViewModelModule.class, BroadcastModule.class})
public interface AppComponent {

    @Component.Builder
    interface Builder {
        AppComponent build();

        @BindsInstance
        Builder context(Application application);
    }

    void inject(App app);
}
