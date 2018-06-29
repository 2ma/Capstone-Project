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
import hu.am2.myway.location.model.Way;
import hu.am2.myway.ui.main.MainActivity;
import hu.am2.myway.ui.map.MapActivity;

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

    public static void updateAppWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, WidgetStatus widgetStatus) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, widgetStatus);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, WidgetStatus widgetStatus) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.my_way_widget);

        if (widgetStatus != null) {
            if (widgetStatus.getState() == STATE_STOP) {
                defaultWidgetState(context, views, appWidgetManager, appWidgetId);
            } else {
                views.setTextViewText(R.id.widgetDistanceText, context.getString(R.string.distance_unit, widgetStatus.getDistance()));
                views.setTextViewText(R.id.widgetTimeText, Utils.getTimeFromMilliseconds(widgetStatus.getTime()));

                views.setImageViewResource(R.id.widgetRecordPauseBtn, widgetStatus.getState() == STATE_RECORDING ? R.drawable.ic_pause_png : R
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

                Intent intentMap = new Intent(context, MapActivity.class);
                PendingIntent pendingMap = PendingIntent.getActivity(context, 0, intentMap, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.widget, pendingMap);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        } else {
            defaultWidgetState(context, views, appWidgetManager, appWidgetId);
        }
    }

    private static void defaultWidgetState(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
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
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long wayId = sharedPreferences.getLong(Constants.PREF_WAY_ID, -1);
        if (wayId != -1) {
            executors.getDiskIO().execute(() -> {
                Way way = repository.getWayForId(wayId);
                updateAppWidgets(context, appWidgetManager, appWidgetIds, new WidgetStatus(way.getTotalDistance() / 1000, way.getTotalTime(),
                    sharedPreferences.getInt(Constants.PREF_RECORDING_STATE, STATE_STOP)));
            });
        } else {
            updateAppWidgets(context, appWidgetManager, appWidgetIds, null);
        }
    }
}

