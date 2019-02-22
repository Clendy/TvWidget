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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Property;
import android.view.View;

/**
 * @author Clendy
 */
class NLayoutManager extends LinearLayoutManager {

    private static final Property<Drawable, Rect> BOUNDS_PROP = Property.of(
            Drawable.class, Rect.class, "bounds");

    private Drawable[] mSelectorDrawables = new Drawable[2];
    private int mShowTime = 0;

    public NLayoutManager(Context context, Drawable[] selectorDrawables) {
        super(context);
        if (selectorDrawables != null && selectorDrawables.length >= 2) {
            mSelectorDrawables[0] = selectorDrawables[0];
            mSelectorDrawables[1] = selectorDrawables[1];
        }
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                          int widthSpec, int heightSpec) {
        super.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (getItemCount() > 0) {
            View view = findViewByPosition(0);
            if (view != null && mSelectorDrawables[0] != null && mShowTime++ <= 0) {
                Rect mSelectorSourceRect =
                        new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                ObjectAnimator.ofObject(
                        mSelectorDrawables[0], BOUNDS_PROP, new RectEvaluator(),
                        mSelectorSourceRect, mSelectorSourceRect)
                        .setDuration(10)
                        .start();
            }
        }
    }
}
