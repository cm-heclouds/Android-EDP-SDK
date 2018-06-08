/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.app.Application;

/**
 * Created by chenglei on 2016/2/2.
 */
public class App extends Application {

    public static final String LOG_TAG = "EdpSample";

    public static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }
}
