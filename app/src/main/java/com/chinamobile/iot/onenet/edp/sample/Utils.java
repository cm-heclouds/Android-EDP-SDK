/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by chenglei on 2016/2/2.
 */
public class Utils {

    public static boolean isNetworkAvaliable(Context context) {
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        }
        return false;
    }

    public static boolean checkNetwork(final Context context) {
        if (null == context) {
            return false;
        }
        if (isNetworkAvaliable(context)) {
            return true;
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, context.getResources().getString(R.string.network_unavailable_hint), Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
    }

    /**
     * 检查EditText内容是否为空，为空则Toast提示，内容为errorText
     */
    public static boolean checkInputNonEmpty(EditText edit, @NonNull String errorText) {
        String input = edit.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            edit.requestFocus();
            Toast.makeText(edit.getContext(), errorText, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
