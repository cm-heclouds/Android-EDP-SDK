/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by chenglei on 2016/3/22.
 */
public class IconView extends TextView {

    private static Typeface sTypeface;

    public IconView(Context context) {
        this(context, null);
    }

    public IconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (null == sTypeface) {
            sTypeface = Typeface.createFromAsset(getContext().getAssets(), "iconfont.ttf");
        }
        setTypeface(sTypeface);
        setIncludeFontPadding(false);
    }
}
