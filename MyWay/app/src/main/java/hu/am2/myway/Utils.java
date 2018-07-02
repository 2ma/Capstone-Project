package hu.am2.myway;


import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hu.am2.myway.location.model.WayPoint;

public class Utils {

    public static String getTimeFromMilliseconds(long ms) {
        final int H = (int) (ms / DateUtils.HOUR_IN_MILLIS);
        int remains = (int) (ms % DateUtils.HOUR_IN_MILLIS);

        final int m = (int) (remains / DateUtils.MINUTE_IN_MILLIS);
        remains = (int) (remains % DateUtils.MINUTE_IN_MILLIS);

        final int s = (int) (remains / DateUtils.SECOND_IN_MILLIS);

        StringBuilder sb = new StringBuilder();

        sb.append(H);
        sb.append(":");
        if (m < 10) {
            sb.append("0");
        }
        sb.append(m);
        sb.append(":");
        if (s < 10) {
            sb.append("0");
        }
        sb.append(s);

        return sb.toString();
    }

    public static String epochToStringDate(long ms) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.getDefault());
        Date d = new Date(ms);
        return simpleDateFormat.format(d);
    }

    public static SpannableString getSmallSpannable(String string, int start) {
        SpannableString spannableString = new SpannableString(string);
        spannableString.setSpan(new RelativeSizeSpan(0.5f), start, string.length(), 0);
        return spannableString;
    }

    public static List<List<LatLng>> getSegmentsFromWayPoints(List<WayPoint> wayPoints) {
        List<List<LatLng>> segments = new ArrayList<>();
        int size = wayPoints.size();
        if (wayPoints.size() > 0) {
            List<LatLng> waySegment = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                WayPoint w = wayPoints.get(i);
                if (w.getLatitude() == Constants.WAY_END_COORDINATE) {
                    if (waySegment.size() > 0) {
                        segments.add(waySegment);
                        waySegment = new ArrayList<>();
                    }
                } else {
                    waySegment.add(new LatLng(w.getLatitude(), w.getLongitude()));
                }
            }
        }
        return segments;
    }
}
