/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.support.v4.app.NotificationCompat;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Class that manages notifications.
 */
public class NotificationController {

    public static final int ID_DOWNLOADING = 0;
    public static final int ID_CMD = 1;

    private static NotificationController sInstance;
    private final Context mApplicationContext;
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final HashMap<Long, ContentObserver> mNotificationMap;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public static final int TYPE_CMD = 1;
    
    NotificationController(Context context) {
        mContext = context;
        mApplicationContext = context.getApplicationContext();
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationMap = new HashMap<Long, ContentObserver>();
    }

    /** Singleton access */
    public static synchronized NotificationController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NotificationController(context);
        }
        return sInstance;
    }
    
    public void notifyDownloadingProgress(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mApplicationContext);
        builder.setTicker("开始下载");
        builder.setContentTitle("正在下载");
        builder.setContentText("已下载" + progress + "%");
        builder.setProgress(100, progress, false);
        builder.setSmallIcon(R.drawable.ic_launcher);
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        builder.setContentIntent(PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        Notification n = builder.build();
        mNotificationManager.notify("download", ID_DOWNLOADING, n);
    }

    public void notifyMessage(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mApplicationContext);
        builder.setTicker("通知");
        builder.setContentTitle("EDP消息");
        builder.setContentText(message);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra("type", TYPE_CMD);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        builder.setContentIntent(PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        Notification n = builder.build();
        mNotificationManager.notify("download", ID_CMD, n);
    }
    
    public void cancelDownloadingProgress() {
        mNotificationManager.cancel("download", ID_DOWNLOADING);
    }

}
