package hu.am2.myway.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import hu.am2.myway.Constants;
import hu.am2.myway.R;
import hu.am2.myway.location.LocationService;
import hu.am2.myway.location.model.WayStatus;

public class MyWayWidget extends AppWidgetProvider {

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
        } else if (status.getState() == WayStatus.STATE_STOP) {
            defaultWidgetState(context, views);
        } else {
            views.setTextViewText(R.id.distanceText, String.valueOf(status.getDistance()));
            views.setTextViewText(R.id.timeText, String.valueOf(status.getTime()));

            views.setImageViewResource(R.id.playPauseBtn, status.getState() == WayStatus.STATE_RECORDING ? R.drawable.ic_pause_png : R.drawable
                .ic_record_png);

            Intent intentStartPause = new Intent(context, LocationService.class);
            intentStartPause.setAction(Constants.ACTION_START_PAUSE_RECORDING);
            PendingIntent pendingStartPause = PendingIntent.getService(context, 0, intentStartPause, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.startPauseBtn, pendingStartPause);

            Intent intentStop = new Intent(context, LocationService.class);
            intentStop.setAction(Constants.ACTION_STOP_RECORDING);
            PendingIntent pendingStop = PendingIntent.getService(context, 1, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.startPauseBtn, pendingStop);

        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void defaultWidgetState(Context context, RemoteViews views) {
        views.setImageViewResource(R.id.playPauseBtn, R.drawable.ic_record_png);
        views.setTextViewText(R.id.distanceText, context.getString(R.string.distance_default));
        views.setTextViewText(R.id.timeText, context.getString(R.string.time_default));

        Intent intent = new Intent(context, LocationService.class);
        intent.setAction(Constants.ACTION_START_PAUSE_RECORDING);
        PendingIntent pendingStart = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.startPauseBtn, pendingStart);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAppWidget(context, appWidgetManager, appWidgetIds, null);
    }
}

