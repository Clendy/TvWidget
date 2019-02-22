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

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

/**
 * @author Clendy 2016/12/22 022 10:21
 */
public class RecyclerTabHelper {

    private RecyclerTabHelper() {

    }

    public static RecyclerTabHelper newInstance() {
        return new RecyclerTabHelper();
    }

    public void changeTabTitleColor(Context context, LinearLayoutManager manager, @IdRes int idRes,
                                    @ColorRes int selectColor, @ColorRes int defaultColor, int pos) {
        if (context == null || manager == null || manager.getItemCount() <= 0 || idRes == -1) {
            return;
        }
        for (int i = 0; i < manager.getItemCount(); i++) {
            View view = manager.findViewByPosition(i);
            if (view.findViewById(idRes) instanceof TextView) {
                TextView textView = (TextView) view.findViewById(idRes);
                if (pos == i) {
                    textView.setTextColor(context.getResources().getColor(selectColor));
                } else {
                    textView.setTextColor(context.getResources().getColor(defaultColor));
                }
            }
        }
    }
}
