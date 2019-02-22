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

package io.github.clendy.recyclertablayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Property;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;


/**
 * A subclass extends RecyclerView as a navigation bar for Android TV application,
 * like TabLayout, need to be used with ViewPager
 *
 * @author Clendy
 */
public class RecyclerTabLayout extends RecyclerView implements
        ViewTreeObserver.OnGlobalFocusChangeListener, ViewPager.OnPageChangeListener {

    private ViewPager mViewPager;

    /**
     * Adapter position that will be selected after certain layout pass.
     */
    private int mPendingSelectionInt = NO_POSITION;

    /**
     * Focus helper.
     */
    private final FocusArchivist mFocusArchivist = new FocusArchivist();

    private static final Property<Drawable, Rect> BOUNDS_PROP = Property.of(
            Drawable.class, Rect.class, "bounds");

    private LinearLayoutManager mLinearLayoutManager;
    private FragmentActivity mActivity;

    @IntDef({SELECTED, FOCUSED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Selector {
    }

    private static final int SELECTED = 0;
    private static final int FOCUSED = 1;
    private static final int SELECTOR_COUNT = 2;

    private final AdapterDataObserver mDataObserver = new LocalAdapterDataObserver();

    private boolean mRememberLastFocus = true;

    private boolean mSmoothScrolling = false;

    private final Rect mSelectorSourceRect = new Rect();
    private final Rect mSelectorDestRect = new Rect();
    private final Interpolator mTransitionInterpolator = new LinearInterpolator();
    private final Animator[] mSelectorAnimators = new Animator[SELECTOR_COUNT];
    private final Drawable[] mSelectorDrawables = new Drawable[SELECTOR_COUNT];
    private AnimatorSet mSelectorAnimator;
    private int mSelectorVelocity = 0;

    private final SelectAnimatorListener mReusableSelectListener = new SelectAnimatorListener();


    private class LocalAdapterDataObserver extends AdapterDataObserver {

        @Override
        public void onChanged() {
            if (!getAdapter().hasStableIds()) {
                mPendingSelectionInt = getSelectedItemPosition();
                if (mPendingSelectionInt == NO_POSITION) {
                    mPendingSelectionInt = 0;
                }
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (!getAdapter().hasStableIds()) {
                int selectedPos = getSelectedItemPosition();
                if (selectedPos >= positionStart && selectedPos < positionStart + itemCount) {
                    mPendingSelectionInt = getSelectedItemPosition();
                }
                if (mPendingSelectionInt == NO_POSITION) {
                    mPendingSelectionInt = 0;
                }
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            int selectedPos = getSelectedItemPosition();
            if (selectedPos >= fromPosition && selectedPos < fromPosition + itemCount) {
                setSelection(selectedPos - fromPosition + toPosition);
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            int selectedPos = getSelectedItemPosition();
            if (selectedPos >= positionStart && selectedPos < positionStart + itemCount) {
                setSelection(selectedPos + itemCount);
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            int selectedPos = getSelectedItemPosition();
            if (selectedPos >= positionStart && selectedPos < positionStart + itemCount) {
                setSelection(positionStart);
            }
        }
    }

    private final class SelectAnimatorListener extends AnimatorListenerAdapter {

        View mToSelect;
        View mToDeselect;

        @Override
        public void onAnimationStart(Animator animation) {
            if (mToDeselect != null) {
                childSetSelected(mToDeselect, false);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mToSelect != null) {
                childSetSelected(mToSelect, true);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            onAnimationEnd(animation);
        }
    }

    /**
     * Callback for {@link Drawable} selectors. View must keep this reference in order for
     * {@link java.lang.ref.WeakReference} in selectors to survive.
     *
     * @see Drawable#setCallback(Drawable.Callback)
     */
    private final Drawable.Callback mSelectorCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            invalidate(who.getBounds());
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            getHandler().postAtTime(what, who, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            getHandler().removeCallbacks(what, who);
        }
    };


    private OnItemClickListener mOnItemClickListener;
    private final List<OnItemSelectedListener> mOnItemSelectedListenerList = new ArrayList<>();
    private OnViewPagerScrollStateListener mOnViewPagerScrollStateListener;
    private OnKeyInterceptListener mOnKeyInterceptListener;

    /**
     * Interface definition for a callback to be invoked when an item in this
     * RecyclerTabGroup has been clicked.
     */
    public interface OnItemClickListener {
        void onItemClick(RecyclerTabLayout parent, View view, int position, long id);
    }

    /**
     * Interface definition for a callback to be invoked when
     * an item in this view has been selected.
     */
    public interface OnItemSelectedListener {
        void onItemSelected(RecyclerTabLayout parent, View view, int position, long id);

        void onItemFocused(RecyclerTabLayout parent, View view, int position, long id);
    }

    /**
     * Interface definition for a callback to be invoked when ViewPager scrolling.
     */
    public interface OnViewPagerScrollStateListener {

        void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

        void onPageSelected(int selectedPos);

        void onPageScrollStateChanged(int state);
    }

    /**
     * Interface definition for a callback to be invoked when intercept key event.
     */
    public interface OnKeyInterceptListener {

        boolean onInterceptKeyEvent(KeyEvent event);
    }

    public RecyclerTabLayout(Context context) {
        this(context, null);
    }

    public RecyclerTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs, defStyle);
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RecyclerTabLayout,
                    defStyle, 0);
            try {
                if (ta.hasValue(R.styleable.RecyclerTabLayout_focusedBackground)) {
                    setFocusedBackground(
                            ta.getDrawable(R.styleable.RecyclerTabLayout_focusedBackground));
                }

                if (ta.hasValue(R.styleable.RecyclerTabLayout_selectedBackground)) {
                    setSelectedBackground(
                            ta.getDrawable(R.styleable.RecyclerTabLayout_selectedBackground));
                }

                if (ta.hasValue(R.styleable.RecyclerTabLayout_selectorVelocity)) {
                    setSelectorVelocity(
                            ta.getInt(R.styleable.RecyclerTabLayout_selectorVelocity, 0));
                }

                if (ta.hasValue(R.styleable.RecyclerTabLayout_orientation)) {
                    setOrientation(ta.getInt(R.styleable.RecyclerTabLayout_orientation, 0));
                }

                setSmoothScrolling(ta.getBoolean(R.styleable.RecyclerTabLayout_smoothScrolling,
                        false));
            } finally {
                ta.recycle();
            }

        }

        setHasFixedSize(true);
        setFocusable(true);
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        setWillNotDraw(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);

        addOnItemSelectedListener(mOnItemSelectedListener);
    }

    public LinearLayoutManager getLinearLayoutManager() {
        return mLinearLayoutManager;
    }

    /**
     * Sets the orientation of the layout. {@link android.support.v7.widget.LinearLayoutManager}
     * will do its best to keep scroll position.
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    private void setOrientation(int orientation) {
        mLinearLayoutManager = new NLayoutManager(getContext(), mSelectorDrawables);
        mLinearLayoutManager.setReverseLayout(false);
        if (orientation == 0) {
            mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        } else {
            mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        }
        setLayoutManager(mLinearLayoutManager);
    }

    /**
     * Sets selectors velocity. Zero or less velocity means that transition will be instant.
     *
     * @param velocity velocity's values
     */
    private void setSelectorVelocity(int velocity) {
        mSelectorVelocity = velocity;
    }

    /**
     * Gets selectors velocity.
     *
     * @return selectors velocity
     */
    public int getSelectorVelocity() {
        return mSelectorVelocity;
    }

    /**
     * Sets smooth scrolling flag. If set to true, container will smoothly scroll to selected child
     * if it is outside of the viewport (by viewport one means some 'camera' rectangle, not
     * necessarily all screen).
     *
     * @param smoothScrolling if true, enable smooth scrolling
     */
    private void setSmoothScrolling(boolean smoothScrolling) {
        mSmoothScrolling = smoothScrolling;
    }

    /**
     * Gets smooth scrolling flag.
     *
     * @return true if smooth scrolling is enabled
     * @see #setSmoothScrolling
     */
    public boolean getSmoothScrolling() {
        return mSmoothScrolling;
    }

    /**
     * Sets background selector which will be drawn behind the child which has focus.
     *
     * @param drawable selector drawable
     */
    private void setFocusedBackground(Drawable drawable) {
        setSelector(FOCUSED, drawable);
    }

    /**
     * Sets background selector which will be drawn behind the child which has focus.
     *
     * @param resId selector drawable's resource ID
     */
    public void setFocusedBackground(@DrawableRes int resId) {
        setFocusedBackground(getDrawableResource(resId));
    }

    /**
     * Gets background selector which will be drawn behind the child which has focus.
     *
     * @return focused background selector
     */
    public Drawable getFocusedBackground() {
        return getSelector(FOCUSED);
    }

    /**
     * Sets background selector which will be be drawn behind the child which has been selected
     * but lost focus.
     *
     * @param drawable selector drawable
     */
    private void setSelectedBackground(Drawable drawable) {
        setSelector(SELECTED, drawable);
    }

    /**
     * Sets background selector which will be be drawn behind the child which has been selected
     * but lost focus.
     *
     * @param resId selector drawable's resource ID
     */
    public void setSelectedBackground(@DrawableRes int resId) {
        setSelectedBackground(getDrawableResource(resId));
    }

    /**
     * Gets foreground selector which will be drawn atop of the child.
     *
     * @return selected background selector
     */
    public Drawable getSelectedBackground() {
        return getSelector(SELECTED);
    }

    private void setSelector(@Selector int index, Drawable drawable) {
        enforceSelectorIndexBounds(index);

        mSelectorDrawables[index] = drawable;
        mSelectorAnimators[index] = createSelectorAnimator(drawable);
        setSelectorCallback(drawable);
    }

    private Drawable getSelector(int index) {
        enforceSelectorIndexBounds(index);

        return mSelectorDrawables[index];
    }

    /**
     * Ensures that passed number is valid selector index.
     *
     * @param index selector index
     * @throws IndexOutOfBoundsException if index is not valid selector index
     */
    private void enforceSelectorIndexBounds(int index) {
        if (index < 0 || index >= SELECTOR_COUNT) {
            throw new IndexOutOfBoundsException("Passed index is not in valid range which is"
                    + " [0; " + SELECTOR_COUNT + ").");
        }
    }

    /**
     * Register a callback to be invoked when an item in this RecyclerView has
     * been clicked.
     *
     * @param listener the callback that will be invoked
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    /**
     * Register a callback to be invoked when an item in this RecyclerView has
     * been selected.
     *
     * @param listener the callback that will run
     */
    public void addOnItemSelectedListener(OnItemSelectedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("The OnItemSelectedListener is null.");
        }
        synchronized (mOnItemSelectedListenerList) {
            if (mOnItemSelectedListenerList.contains(listener)) {
                throw new IllegalStateException(
                        "OnItemSelectedListener " + listener + " is already added.");
            }
            mOnItemSelectedListenerList.add(listener);
        }
    }

    public void setOnViewPagerScrollStateListener(OnViewPagerScrollStateListener listener) {
        mOnViewPagerScrollStateListener = listener;
    }

    public OnKeyInterceptListener getOnKeyInterceptListener() {
        return mOnKeyInterceptListener;
    }

    /**
     * Register a callback to be invoked when intercept RecyclerView's key event.
     *
     * @param listener the callback that will be invoked
     */
    public void setOnKeyInterceptListener(OnKeyInterceptListener listener) {
        mOnKeyInterceptListener = listener;
    }

    public int getSelectedItemPosition() {
        View focusedChild = getFocusedChild();
        return getChildAdapterPosition(focusedChild);
    }

    /**
     * Set adapter position for item to select if RecycleView currently has focus or schedule
     * selection on next focus obtainment.
     *
     * @param adapterPosition adapter position of item to be selected
     */
    public void setSelection(int adapterPosition) {
        mPendingSelectionInt = adapterPosition;
        scrollToPosition(adapterPosition);
    }

    public boolean isRememberLastFocus() {
        return mRememberLastFocus;
    }

    public void setRememberLastFocus(boolean rememberLastFocus) {
        mRememberLastFocus = rememberLastFocus;
    }

    /**
     * ensure focus wither on leftmost item.
     *
     * @return if true, focus on leftmost item.
     */
    public boolean isFocusOnLeftmost() {
        if (mLinearLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            if (mPendingSelectionInt == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * ensure focus wither on rightmost item.
     *
     * @return if true, focus on rightmost item.
     */
    public boolean isFocusOnRightmost() {
        if (mLinearLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            if (mPendingSelectionInt == mLinearLayoutManager.getChildCount() - 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * ensure focus wither on topmost item.
     *
     * @return if true, focus on topmost item.
     */
    public boolean isFocusOnTopmost() {
        if (mLinearLayoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
            if (mPendingSelectionInt == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * ensure focus wither on bottommost item.
     *
     * @return if true, focus on bottommost item.
     */
    public boolean isFocusOnBottommost() {
        if (mLinearLayoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
            if (mPendingSelectionInt == mLinearLayoutManager.getChildCount() - 1) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setDescendantFocusability(enabled ? FOCUS_BEFORE_DESCENDANTS : FOCUS_BLOCK_DESCENDANTS);
        setFocusable(enabled);
    }

    @Override
    public void setAdapter(Adapter newAdapter) {
        Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mDataObserver);
        }

        super.setAdapter(newAdapter);

        if (newAdapter != null) {
            newAdapter.registerAdapterDataObserver(mDataObserver);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mPendingSelectionInt != NO_POSITION) {
            setSelectionOnLayout(mPendingSelectionInt);
        }
    }

    private void setSelectionOnLayout(int position) {
        RecyclerView.ViewHolder holder = findViewHolderForAdapterPosition(position);

        if (holder != null) {
            if (hasFocus()) {
                holder.itemView.requestFocus();
            } else {
                mFocusArchivist.archiveFocus(this, holder.itemView);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewTreeObserver obs = getViewTreeObserver();
        obs.addOnGlobalFocusChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ViewTreeObserver obs = getViewTreeObserver();
        obs.removeOnGlobalFocusChangeListener(this);
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        enforceSelectorsVisibility(isInTouchMode(), hasFocus());
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (mLinearLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
                View nextFocus = FocusFinder.getInstance().findNextFocus(this, focused, direction);
                if (nextFocus != null) {
                    return nextFocus;
                }
                return null;
            }
        } else {
            if (direction == View.FOCUS_UP || direction == View.FOCUS_DOWN) {
                View nextFocus = FocusFinder.getInstance().findNextFocus(this, focused, direction);
                if (nextFocus != null) {
                    return nextFocus;
                }
                return null;
            }
        }
        return super.focusSearch(focused, direction);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
//        Log.d("RecyclerTabLayout", "mPendingSelectionInt:" + mPendingSelectionInt);
        if (gainFocus) {
            boolean favorNaturalFocus = !mRememberLastFocus && previouslyFocusedRect != null;
            View lastFocusedView = mFocusArchivist.getLastFocus(this);
            if (favorNaturalFocus || lastFocusedView == null) {
                requestNaturalFocus(direction, previouslyFocusedRect);
            } else {
                if (mPendingSelectionInt == -1) {
                    lastFocusedView.requestFocus();
                } else if (mLinearLayoutManager.getPosition(lastFocusedView)
                        == mPendingSelectionInt) {
                    lastFocusedView.requestFocus();
                } else {
                    mLinearLayoutManager.findViewByPosition(mPendingSelectionInt).requestFocus();
                }
            }
        }
    }

    /**
     * Request natural focus.
     *
     * @param direction             direction in which focus is changing
     * @param previouslyFocusedRect previously focus rectangle
     */
    private void requestNaturalFocus(int direction, Rect previouslyFocusedRect) {
        FocusFinder ff = FocusFinder.getInstance();
        previouslyFocusedRect = previouslyFocusedRect == null
                ? new Rect(0, 0, 0, 0) : previouslyFocusedRect;
        View toFocus = ff.findNextFocusFromRect(this, previouslyFocusedRect, direction);
        toFocus = toFocus == null ? getChildAt(0) : toFocus;
        if (toFocus != null) {
            toFocus.requestFocus();
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);

        requestChildFocusInner(child, focused);
        fireOnItemFocusedEvent(child);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        if (state == SCROLL_STATE_IDLE) {
            View focusedChild = getFocusedChild();
            requestChildFocusInner(focusedChild, focusedChild);
        }
    }

    private void requestChildFocusInner(View child, View focused) {
        Drawable refSelector = null;
        for (Drawable selector : mSelectorDrawables) {
            if (selector != null) {
                refSelector = selector;
                break;
            }
        }

        int scrollState = getScrollState();

        if (refSelector != null && scrollState == SCROLL_STATE_IDLE) {
            mSelectorSourceRect.set(refSelector.getBounds());

            // Focused cannot be null
            focused.getHitRect(mSelectorDestRect);

            mReusableSelectListener.mToSelect = child;
            mReusableSelectListener.mToDeselect = mFocusArchivist.getLastFocus(this);

            animateSelectorChange(mReusableSelectListener);

            mFocusArchivist.archiveFocus(this, child);
        }
    }

    private void requestChildFocusInner() {
        Drawable refSelector = null;
        for (Drawable selector : mSelectorDrawables) {
            if (selector != null) {
                refSelector = selector;
                break;
            }
        }

        int scrollState = getScrollState();

        if (refSelector != null && scrollState == SCROLL_STATE_IDLE) {
            mSelectorSourceRect.set(refSelector.getBounds());

            View view = mLinearLayoutManager.findViewByPosition(mPendingSelectionInt);
            view.getHitRect(mSelectorDestRect);

            mReusableSelectListener.mToSelect = view;
            mReusableSelectListener.mToDeselect = mFocusArchivist.getLastFocus(this);

            animateSelectorChange(mReusableSelectListener);

            mFocusArchivist.archiveFocus(this, view);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {

        drawSelectorIfVisible(FOCUSED, canvas);
        drawSelectorIfVisible(SELECTED, canvas);

        super.onDraw(canvas);
    }

    private void drawSelectorIfVisible(@Selector int index, Canvas canvas) {
        enforceSelectorIndexBounds(index);

        Drawable selector = mSelectorDrawables[index];
        if (selector != null && selector.isVisible()) {
            selector.draw(canvas);
        }
    }

    /**
     * Animates selector when changes happen.
     *
     * @param listener AnimatorListener
     */
    private void animateSelectorChange(Animator.AnimatorListener listener) {
        if (mSelectorAnimator != null) {
            mSelectorAnimator.cancel();
        }

        mSelectorAnimator = new AnimatorSet();

        for (int i = 0; i < SELECTOR_COUNT; i++) {
            mSelectorAnimator.playTogether(mSelectorAnimators[i]);
        }

        mSelectorAnimator.setInterpolator(mTransitionInterpolator);
        mSelectorAnimator.addListener(listener);

        int duration = 0;
        if (mSelectorVelocity > 0) {
            int dx = mSelectorDestRect.centerX() - mSelectorSourceRect.centerX();
            int dy = mSelectorDestRect.centerY() - mSelectorSourceRect.centerY();
            duration = computeTravelDuration(dx, dy, mSelectorVelocity);
        }

        mSelectorAnimator.setDuration(duration);
        mSelectorAnimator.start();
    }

    private Animator createSelectorAnimator(Drawable selector) {
        return ObjectAnimator.ofObject(
                selector, BOUNDS_PROP, new RectEvaluator(),
                mSelectorSourceRect, mSelectorDestRect);
    }

    private int computeTravelDuration(int dx, int dy, int velocity) {
        return (int) (Math.sqrt(dx * dx + dy * dy) / velocity * 1000);
    }

    private void enforceSelectorsVisibility(boolean isInTouchMode, boolean hasFocus) {
        boolean visible = !isInTouchMode && hasFocus;
        for (int i = 0; i < mSelectorDrawables.length; i++) {
            if (i == FOCUSED) {
                mSelectorDrawables[i].setVisible(visible, false);
            }
            if (i == SELECTED) {
                mSelectorDrawables[i].setVisible(!visible, false);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private Drawable getDrawableResource(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getResources().getDrawable(resId, getContext().getTheme());
        } else {
            return getResources().getDrawable(resId);
        }
    }

    private void setSelectorCallback(Drawable selector) {
        if (selector != null) {
            selector.setCallback(mSelectorCallback);
        }
    }

    private void childSetSelected(View child, boolean selected) {
        child.setSelected(selected);

        if (selected) {
            fireOnItemSelectedEvent(child);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyInterceptListener != null && mOnKeyInterceptListener.onInterceptKeyEvent(event)) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return true;
            }
        }

        boolean consumed = super.dispatchKeyEvent(event);

        View focusedChild = getFocusedChild();

        if (focusedChild != null
                && event.getAction() == KeyEvent.ACTION_DOWN
                && isClickEvent(event)
                && event.getRepeatCount() == 0) {
            fireOnItemClickEvent(focusedChild);
        }

        return consumed;
    }

    private boolean isClickEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (mOnItemClickListener != null) {
            child.setClickable(true);
        }
    }

    @Override
    public void getFocusedRect(Rect r) {
        getDrawingRect(r);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (hasFocus()) {
            super.addFocusables(views, direction, focusableMode);
        } else if (isFocusable()) {
            views.add(this);
        }
    }

    private void fireOnItemClickEvent(View child) {
        if (mOnItemClickListener != null) {
            int position = getChildAdapterPosition(child);
            long id = getChildItemId(child);
            mOnItemClickListener.onItemClick(this, child, position, id);
        }
    }

    private void fireOnItemFocusedEvent(View child) {
        if (mOnItemSelectedListenerList != null && mOnItemSelectedListenerList.size() > 0) {
            int position = getChildAdapterPosition(child);
            long id = getChildItemId(child);
            for (OnItemSelectedListener listener : mOnItemSelectedListenerList) {
                listener.onItemFocused(this, child, position, id);
            }
        }
    }

    private void fireOnItemSelectedEvent(View child) {
        if (mOnItemSelectedListenerList != null && mOnItemSelectedListenerList.size() > 0) {
            int position = getChildAdapterPosition(child);
            long id = getChildItemId(child);
            for (OnItemSelectedListener listener : mOnItemSelectedListenerList) {
                listener.onItemSelected(this, child, position, id);
            }
        }
    }

    private OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(RecyclerTabLayout parent, View view, int position, long id) {
            setCurrentPager(position);
        }

        @Override
        public void onItemFocused(RecyclerTabLayout parent, View view, int position, long id) {

        }
    };

    public void setActivity(FragmentActivity activity) {
        mActivity = activity;
    }

    private void setCurrentPager(int index) {
        if (getContext() instanceof FragmentActivity) {
            mActivity = ((FragmentActivity) getContext());
        }
        if (mActivity != null && !mActivity.isFinishing()) {
            if (ensureCurrentPager(index)) {
                mViewPager.setCurrentItem(index, true);
            }
        }
    }

    private boolean ensureCurrentPager(int index) {
        return index != -1 && ensureViewPager()
                && mViewPager.getAdapter() != null
                && index < mViewPager.getAdapter().getCount()
                && index != mViewPager.getCurrentItem();
    }

    private boolean ensureViewPager() {
        return mViewPager != null;
    }

    @SuppressWarnings("deprecation")
    public void setViewPager(ViewPager viewPager) {
        if (viewPager == null) {
            return;
        }

        if (viewPager.equals(mViewPager)) return;
        if (mViewPager != null)
            mViewPager.setOnPageChangeListener(null);
        if (viewPager.getAdapter() == null)
            throw new IllegalStateException("ViewPager does not provide adapter instance.");

        mViewPager = viewPager;
        mViewPager.removeOnPageChangeListener(this);
        mViewPager.addOnPageChangeListener(this);

        postInvalidate();
    }

    public void setViewPager(final ViewPager viewPager, int index) {
        setViewPager(viewPager);

        setSelection(index);
        mViewPager.setCurrentItem(index, true);
        postInvalidate();
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mOnViewPagerScrollStateListener != null) {
            mOnViewPagerScrollStateListener.onPageScrolled(position, positionOffset,
                    positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        setSelection(position);
        if (getFocusedChild() == null) {
            requestChildFocusInner();
        }
        if (mOnViewPagerScrollStateListener != null) {
            mOnViewPagerScrollStateListener.onPageSelected(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (mOnViewPagerScrollStateListener != null) {
            mOnViewPagerScrollStateListener.onPageScrollStateChanged(state);
        }
    }
}
