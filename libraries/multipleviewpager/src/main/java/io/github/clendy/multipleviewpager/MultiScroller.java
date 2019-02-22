package io.github.clendy.multipleviewpager;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

class MultiScroller extends Scroller {

    private int mScrollDuration = 800;

    public MultiScroller(Context context) {
        super(context);
    }

    public MultiScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public MultiScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy) {
        super.startScroll(startX, startY, dx, dy, mScrollDuration);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, mScrollDuration);
    }

    public int getScrollDuration() {
        return mScrollDuration;
    }

    public void setScrollDuration(int scrollDuration) {
        mScrollDuration = scrollDuration;
    }
}
