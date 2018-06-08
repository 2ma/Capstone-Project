package hu.am2.myway.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import hu.am2.myway.ui.history.HistoryListFragment;
import hu.am2.myway.ui.history.HistoryMapFragment;

@Module
public abstract class FragmentModule {

    @ContributesAndroidInjector
    abstract HistoryListFragment provideHistoryListFragment();

    @ContributesAndroidInjector
    abstract HistoryMapFragment provideMapFragment();
}
