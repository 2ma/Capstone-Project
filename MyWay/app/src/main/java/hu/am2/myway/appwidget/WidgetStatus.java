package hu.am2.myway.appwidget;

public class WidgetStatus {

    //in km
    private final float distance;
    //in ms
    private final long time;
    private final int state;

    public WidgetStatus(float distance, long time, int state) {
        this.distance = distance;
        this.time = time;
        this.state = state;
    }

    public float getDistance() {
        return distance;
    }

    public long getTime() {
        return time;
    }

    public int getState() {
        return state;
    }
}
