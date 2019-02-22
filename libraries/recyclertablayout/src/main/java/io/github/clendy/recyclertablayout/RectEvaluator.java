package io.github.clendy.recyclertablayout;

import android.animation.TypeEvaluator;
import android.graphics.Rect;
import android.support.annotation.NonNull;

/**
 * @author Clendy
 */
class RectEvaluator implements TypeEvaluator<Rect> {

    private final Rect mCacheRect = new Rect();

    @Override
    public Rect evaluate(float fraction, @NonNull Rect startValue, @NonNull Rect endValue) {
        int left = startValue.left + (int) ((endValue.left - startValue.left) * fraction);
        int top = startValue.top + (int) ((endValue.top - startValue.top) * fraction);
        int right = startValue.right + (int) ((endValue.right - startValue.right) * fraction);
        int bottom = startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction);

        mCacheRect.set(left, top, right, bottom);
        return mCacheRect;
    }
}
