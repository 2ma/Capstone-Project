package hu.am2.myway.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import hu.am2.myway.appwidget.MyWayWidget;

@Module
public abstract class BroadcastModule {

    @ContributesAndroidInjector
    abstract MyWayWidget provideMyWayWidget();
}
