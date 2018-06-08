package hu.am2.myway.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import hu.am2.myway.ui.history.HistoryActivity;
import hu.am2.myway.ui.main.MainActivity;
import hu.am2.myway.ui.map.MapActivity;
import hu.am2.myway.ui.saveway.SaveWayActivity;

@Module
public abstract class ActivityModule {

    @ContributesAndroidInjector
    abstract MapActivity provideMapActivity();

    @ContributesAndroidInjector
    abstract HistoryActivity provideHistoryActivity();

    @ContributesAndroidInjector
    abstract MainActivity provideMainActivity();

    @ContributesAndroidInjector
    abstract SaveWayActivity provideSaveWayActivity();
}
