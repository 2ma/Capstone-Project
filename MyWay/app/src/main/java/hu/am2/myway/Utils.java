package hu.am2.myway;


import android.text.format.DateUtils;

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

    public static String getTimeForHistory(long totalTime) {
        final int H = (int) (totalTime / DateUtils.HOUR_IN_MILLIS);
        int remains = (int) (totalTime % DateUtils.HOUR_IN_MILLIS);

        final int m = (int) (remains / DateUtils.MINUTE_IN_MILLIS);

        StringBuilder sb = new StringBuilder();

        sb.append(H);
        sb.append(":");
        if (m < 10) {
            sb.append("0");
        }
        sb.append(m);

        return sb.toString();
    }
}
