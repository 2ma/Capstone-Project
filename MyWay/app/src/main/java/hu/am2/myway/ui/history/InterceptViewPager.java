package hu.am2.myway.ui.history;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class InterceptViewPager extends ViewPager {

    private View[] pageOne;
    private View[] pageTwo;
    private final Rect hitRect = new Rect();

    public InterceptViewPager(@NonNull Context context) {
        super(context);
    }

    public InterceptViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInterceptViews(View[] pageOne, View[] pageTwo) {
        this.pageOne = pageOne;
        this.pageTwo = pageTwo;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (MotionEvent.ACTION_DOWN == ev.getAction()) {
            int pos = getCurrentItem();
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            if (pos == 0) {
                for (View v : pageOne) {
                    v.getHitRect(hitRect);
                    if (hitRect.contains(x, y)) {
                        return super.onTouchEvent(ev);
                    }
                }
                return false;
            } else {
                for (View v : pageTwo) {
                    v.getHitRect(hitRect);
                    if (hitRect.contains(x, y)) {
                        return super.onTouchEvent(ev);
                    }
                }
                return false;
            }
        }
        return super.onTouchEvent(ev);
    }
}
