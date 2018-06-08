package hu.am2.myway.appwidget;

public class WidgetStatus {

    private float distance;
    private long time;
    private int state;

    public WidgetStatus(float distance, long time, int state) {
        this.distance = distance;
        this.time = time;
        this.state = state;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
