package hu.am2.myway.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class MyWayProvider extends ContentProvider {

    private static final int TRACK_POINT = 100;
    private static final int TRACK_POINT_ID = 101;
    private static final int TRACK = 200;
    private static final int TRACK_ID = 201;

    private static final UriMatcher matcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(MyWayContract.AUTHORITY, MyWayContract.TrackPoint.PATH_TRACK_POINT, TRACK_POINT);
        uriMatcher.addURI(MyWayContract.AUTHORITY, MyWayContract.TrackPoint.PATH_TRACK_POINT + "/*", TRACK_POINT_ID);
        uriMatcher.addURI(MyWayContract.AUTHORITY, MyWayContract.Track.PATH_TRACK, TRACK);
        uriMatcher.addURI(MyWayContract.AUTHORITY, MyWayContract.Track.PATH_TRACK + "/*", TRACK_ID);

        return uriMatcher;
    }

    private MyWayDatabase myWayDatabase;

    @Override
    public boolean onCreate() {
        myWayDatabase = new MyWayDatabase(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable
        String sortOrder) {

        Cursor cursor;

        final SQLiteDatabase db = myWayDatabase.getReadableDatabase();

        final int match = matcher.match(uri);

        switch (match) {
            case TRACK_POINT: {
                cursor = db.query(MyWayContract.TrackPoint.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            case TRACK_POINT_ID: {
                selection = MyWayContract.TrackPoint._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(MyWayContract.TrackPoint.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            case TRACK: {
                cursor = db.query(MyWayContract.Track.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            case TRACK_ID: {
                selection = MyWayContract.Track._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(MyWayContract.Track.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        final int match = matcher.match(uri);

        switch (match) {
            case TRACK_POINT: {
                return MyWayContract.Track.CONTENT_DIR_TYPE;
            }
            case TRACK_POINT_ID: {
                return MyWayContract.Track.CONTENT_ITEM_TYPE;
            }
            case TRACK: {
                return MyWayContract.Track.CONTENT_DIR_TYPE;
            }
            case TRACK_ID: {
                return MyWayContract.Track.CONTENT_ITEM_TYPE;
            }
            default:
                throw new UnsupportedOperationException("Unknown type of uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase db = myWayDatabase.getWritableDatabase();

        final int match = matcher.match(uri);

        long id;

        Uri newUri;

        switch (match) {
            case TRACK_POINT: {
                id = db.insert(MyWayContract.TrackPoint.TABLE_NAME, null, values);
                if (id > 0) {
                    newUri = MyWayContract.TrackPoint.buildTrackPointUri(id);
                } else
                    throw new SQLException("Failed to insert row: " + uri);
                break;
            }
            case TRACK: {
                id = db.insert(MyWayContract.Track.TABLE_NAME, null, values);
                if (id > 0) {
                    newUri = MyWayContract.Track.buildTrackUri(id);
                } else
                    throw new SQLException("Failed to insert row: " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Can't insert, unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return newUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        final SQLiteDatabase db = myWayDatabase.getWritableDatabase();

        final int match = matcher.match(uri);

        int rowsDeleted;

        switch (match) {
            case TRACK_POINT: {
                rowsDeleted = db.delete(MyWayContract.TrackPoint.TABLE_NAME, null, null);
                break;
            }
            case TRACK_POINT_ID: {
                String where = MyWayContract.TrackPoint._ID + "=?";
                String[] whereArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(MyWayContract.TrackPoint.TABLE_NAME, where, whereArgs);
                break;
            }
            case TRACK: {
                rowsDeleted = db.delete(MyWayContract.Track.TABLE_NAME, null, null);
                break;
            }
            case TRACK_ID: {
                String where = MyWayContract.Track._ID + "=?";
                String[] whereArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(MyWayContract.Track.TABLE_NAME, where, whereArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Can't delete, unknown type of uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {

        final SQLiteDatabase db = myWayDatabase.getWritableDatabase();

        final int match = matcher.match(uri);

        int rowsUpdated;

        switch (match) {
            case TRACK_POINT_ID: {
                String where = MyWayContract.TrackPoint._ID + "=?";
                String[] whereArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = db.update(MyWayContract.TrackPoint.TABLE_NAME, values, where, whereArgs);
                break;
            }
            case TRACK_ID: {
                String where = MyWayContract.Track._ID + "=?";
                String[] whereArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = db.update(MyWayContract.Track.TABLE_NAME, values, where, whereArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Can't update, unknown type of uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }
}
