package hu.am2.myway.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyWayDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "my_tracks.db";

    public MyWayDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + MyWayContract.TrackPoint.TABLE_NAME + " (" +
            MyWayContract.TrackPoint._ID + " INTEGER PRIMARY KEY NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_LAT + " INTEGER NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_LONG + " INTEGER NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_TIME + " INTEGER NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_ALTITUDE + " FLOAT NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_ACCURACY + " FLOAT NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_SPEED + " FLOAT NOT NULL, " +
            MyWayContract.TrackPoint.COLUMN_TRACK_ID + " INTEGER NOT NULL );"
        );

        db.execSQL("CREATE TABLE " + MyWayContract.Track.TABLE_NAME + " (" +
            MyWayContract.TrackPoint._ID + " INTEGER PRIMARY KEY NOT NULL, " +
            MyWayContract.Track.COLUMN_NAME + " TEXT, " +
            MyWayContract.Track.COLUMN_DESCRIPTION + " TEXT, " +
            MyWayContract.Track.COLUMN_TYPE + " INTEGER, " +
            MyWayContract.Track.COLUMN_TOTAL_DISTANCE + " FLOAT, " +
            MyWayContract.Track.COLUMN_AVG_SPEED + " FLOAT, " +
            MyWayContract.Track.COLUMN_TOP_SPEED + " FLOAT, " +
            MyWayContract.Track.COLUMN_AVG_MOVING_SPEED + " FLOAT, " +
            MyWayContract.Track.COLUMN_START_TIME + " INTEGER, " +
            MyWayContract.Track.COLUMN_STOP_TIME + " INTEGER, " +
            MyWayContract.Track.COLUMN_TOTAL_TIME + " INTEGER, " +
            MyWayContract.Track.COLUMN_MOVING_TIME + " INTEGER, " +
            MyWayContract.Track.COLUMN_AVG_ALTITUDE + " FLOAT, " +
            MyWayContract.Track.COLUMN_MAX_ALT + " FLOAT, " +
            MyWayContract.Track.COLUMN_MIN_ALT + " FLOAT );"
        );


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
