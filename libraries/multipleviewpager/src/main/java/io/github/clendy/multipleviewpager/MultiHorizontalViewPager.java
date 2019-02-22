/*
 * Copyright (C) 2016 Clendy <yc330483161@163.com|yc330483161@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.clendy.multipleviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;

import java.lang.reflect.Field;


/**
 * ViewPager that can control scrolling to next pager wither focus on ViewPager
 *
 * @author Clendy
 */
public class MultiHorizontalViewPager extends ViewPager {

    private static final String TAG = MultiHorizontalViewPager.class.getSimpleName();

    private static final int DEFAULT_DURATION = 800;

    private View mFocusView = null;

    /**
     * this field to control ViewPager scrolling to next pager wither focus on ViewPager,
     * if false, ViewPager cannot scrolling to next pager when focus on Viewpager,
     * otherwise ViewPager can random switch to left or right pager.
     */
    private boolean mScrollFocusPager = false;

    private int mDuration = DEFAULT_DURATION;

    public MultiHorizontalViewPager(Context context) {
        this(context, null);
    }

    public MultiHorizontalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiViewPager);
        try {
            mScrollFocusPager = typedArray.getBoolean(
                    R.styleable.MultiViewPager_scrollFocusOnPager, false);
            mDuration = typedArray.getInteger(
                    R.styleable.MultiViewPager_scrollDuration, DEFAULT_DURATION);
        } finally {
            typedArray.recycle();
        }

        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(false);
        initViewPagerScroll();
    }

    private void initViewPagerScroll() {
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            MultiScroller scroller = new MultiScroller(getContext());
            scroller.setScrollDuration(mDuration);
            mField.set(this, scroller);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        mFocusView = focused;
        super.requestChildFocus(child, focused);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean executeKeyEvent(KeyEvent event) {
        return super.executeKeyEvent(event);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        return super.focusSearch(focused, direction);
    }

    @Override
    public boolean arrowScroll(int direction) {
        if (mFocusView != null && !mScrollFocusPager) {
            if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
                View nextFocus = FocusFinder.getInstance()
                        .findNextFocus(this, mFocusView, direction);
                if (nextFocus == null) {
                    return false;
                }
            }
        }

        return super.arrowScroll(direction);
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        mDuration = duration;
        initViewPagerScroll();
    }

}
