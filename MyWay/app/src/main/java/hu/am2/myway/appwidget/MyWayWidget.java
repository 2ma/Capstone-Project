package hu.am2.myway.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import hu.am2.myway.AppExecutors;
import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.Utils;
import hu.am2.myway.data.Repository;
import hu.am2.myway.location.LocationService;
import hu.am2.myway.location.WayRecorder;
import hu.am2.myway.location.model.Way;
import hu.am2.myway.ui.main.MainActivity;

import static hu.am2.myway.location.WayRecorder.STATE_RECORDING;
import static hu.am2.myway.location.WayRecorder.STATE_STOP;

public class MyWayWidget extends AppWidgetProvider {

    @Inject
    Repository repository;

    @Inject
    AppExecutors executors;

    @Override
    public void onReceive(Context context, Intent intent) {
        AndroidInjection.inject(this, context);
        super.onReceive(context, intent);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, WidgetStatus status) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, status);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, WidgetStatus status) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.my_way_widget);
        if (status == null) {
            defaultWidgetState(context, views);
        } else if (status.getState() == STATE_STOP) {
            defaultWidgetState(context, views);
        } else {
            views.setTextViewText(R.id.widgetDistanceText, context.getString(R.string.distance_unit, status.getDistance()));
            views.setTextViewText(R.id.widgetTimeText, Utils.getTimeFromMilliseconds(status.getTime()));

            views.setImageViewResource(R.id.widgetRecordPauseBtn, status.getState() == STATE_RECORDING ? R.drawable.ic_pause_png : R
                .drawable
                .ic_record_png);

            Intent intentStartPause = new Intent(context, LocationService.class);
            intentStartPause.setAction(Constants.ACTION_START_PAUSE_RECORDING);

            PendingIntent pendingStartPause;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                pendingStartPause = PendingIntent.getForegroundService(context, 0, intentStartPause, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingStartPause = PendingIntent.getService(context, 0, intentStartPause, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            views.setOnClickPendingIntent(R.id.widgetRecordPauseBtn, pendingStartPause);

            Intent intentStop = new Intent(context, LocationService.class);
            intentStop.setAction(Constants.ACTION_STOP_RECORDING);
            PendingIntent pendingStop;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                pendingStop = PendingIntent.getForegroundService(context, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingStop = PendingIntent.getService(context, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            views.setOnClickPendingIntent(R.id.widgetStopBtn, pendingStop);

        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void defaultWidgetState(Context context, RemoteViews views) {
        views.setImageViewResource(R.id.widgetRecordPauseBtn, R.drawable.ic_record_png);
        views.setTextViewText(R.id.widgetDistanceText, context.getString(R.string.distance_default));
        views.setTextViewText(R.id.widgetTimeText, context.getString(R.string.time_default));
        Intent intentStart = new Intent(context, LocationService.class);
        intentStart.setAction(Constants.ACTION_START_PAUSE_RECORDING);
        PendingIntent pendingStart;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingStart = PendingIntent.getForegroundService(context, 0, intentStart, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingStart = PendingIntent.getService(context, 0, intentStart, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Intent intentMain = new Intent(context, MainActivity.class);
        PendingIntent pendingMain = PendingIntent.getActivity(context, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget, pendingMain);
        views.setOnClickPendingIntent(R.id.widgetRecordPauseBtn, pendingStart);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long id = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
        if (id != -1) {
            executors.getDiskIO().execute(() -> {
                int state = sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, WayRecorder.STATE_STOP);
                Way way = repository.getWayForId(id);
                WidgetStatus widgetStatus = new WidgetStatus(way.getTotalDistance(), way.getTotalTime(), state);
                updateAppWidget(context, appWidgetManager, appWidgetIds, widgetStatus);
            });
        } else {
            updateAppWidget(context, appWidgetManager, appWidgetIds, null);
        }

    }
}

