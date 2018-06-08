package hu.am2.myway.di;

import android.app.Application;
import android.arch.persistence.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import hu.am2.myway.data.dao.WayDao;
import hu.am2.myway.data.database.MyWayDatabase;

@Module
public class AppModule {

    @Provides
    @Singleton
    public MyWayDatabase providesMyWayDatabase(Application application) {
        return Room.databaseBuilder(application, MyWayDatabase.class, "my_way.db").build();
    }

    @Provides
    @Singleton
    public WayDao providesWayDao(MyWayDatabase database) {
        return database.wayDao();
    }
}
