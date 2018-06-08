/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.toolbox;

import android.util.Log;

public final class Logger {

    private static boolean sEnable = true;

    private Logger() {

    }

    public static void setEnable(boolean enable) {
        sEnable = enable;
    }

    public static void v(String tag, String log) {
        if (sEnable) {
            Log.v(tag, log);
        }
    }

    public static void d(String tag, String log) {
        if (sEnable) {
            Log.d(tag, log);
        }
    }

    public static void i(String tag, String log) {
        if (sEnable) {
            Log.i(tag, log);
        }
    }

    public static void w(String tag, String log) {
        if (sEnable) {
            Log.w(tag, log);
        }
    }

    public static void e(String tag, String log) {
        if (sEnable) {
            Log.e(tag, log);
        }
    }

    public static void w(String tag, Throwable tr) {
        if (sEnable) {
            Log.w(tag, tr);
        }
    }

    public static void v(String tag, String log, Throwable tr) {
        if (sEnable) {
            Log.v(tag, log, tr);
        }
    }

    public static void d(String tag, String log, Throwable tr) {
        if (sEnable) {
            Log.d(tag, log, tr);
        }
    }

    public static void i(String tag, String log, Throwable tr) {
        if (sEnable) {
            Log.i(tag, log, tr);
        }
    }

    public static void w(String tag, String log, Throwable tr) {
        if (sEnable) {
            Log.w(tag, log, tr);
        }
    }

    public static void e(String tag, String log, Throwable tr) {
        if (sEnable) {
            Log.e(tag, log, tr);
        }
    }

}
