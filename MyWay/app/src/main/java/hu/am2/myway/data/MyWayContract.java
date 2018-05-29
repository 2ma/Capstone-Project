package hu.am2.myway.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import hu.am2.myway.BuildConfig;

public final class MyWayContract {

    static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final class TrackPoint implements BaseColumns {

        public static final String PATH_TRACK_POINT = "trackPoint";
        public static final String TABLE_NAME = "track_point";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACK_POINT).build();

        public static final String COLUMN_LAT = "lat";
        public static final String COLUMN_LONG = "long";
        public static final String COLUMN_ACCURACY = "acc";
        public static final String COLUMN_ALTITUDE = "alt";
        public static final String COLUMN_SPEED = "speed";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_TRACK_ID = "track_id";


        public static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_TRACK_POINT;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_TRACK_POINT;

        public static Uri buildTrackPointUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }

    public static final class Track implements BaseColumns {

        public static final String PATH_TRACK = "track";
        public static final String TABLE_NAME = "track";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACK).build();

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_TOTAL_DISTANCE = "total_dist";
        public static final String COLUMN_AVG_ALTITUDE = "avg_alt";
        public static final String COLUMN_MAX_ALT = "max_alt";
        public static final String COLUMN_MIN_ALT = "min_alt";
        public static final String COLUMN_AVG_SPEED = "avg_speed";
        public static final String COLUMN_TOP_SPEED = "avg_speed";
        public static final String COLUMN_AVG_MOVING_SPEED = "avg_moving_speed";
        public static final String COLUMN_TOTAL_TIME = "total_time";
        public static final String COLUMN_MOVING_TIME = "moving_time";
        public static final String COLUMN_START_TIME = "start_time";
        public static final String COLUMN_STOP_TIME = "stop_time";


        public static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_TRACK;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_TRACK;

        public static Uri buildTrackUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }
}
