/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.toolbox;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * 该服务主要用于自动发送心跳请求，应在接收到连接响应后启动。
 *
 * Created by chenglei on 2015/12/25.
 */
public class EdpService extends Service {

    private HeartbeatReceiver mHeartbeatReceiver = new HeartbeatReceiver();
    private EdpClient mEdpClient;

    public static void start(Context context) {
        Intent intent = new Intent(context, EdpService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, EdpService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mEdpClient = EdpClient.getInstance();
        mHeartbeatReceiver.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent broadcastIntent = new Intent(EdpClient.ACTION_HEARTBEAT);
        sendBroadcast(broadcastIntent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mHeartbeatReceiver.unregister(this);
        super.onDestroy();
    }

    private class HeartbeatReceiver extends BroadcastReceiver {

        public void register(Context context) {
            IntentFilter filter = new IntentFilter(EdpClient.ACTION_HEARTBEAT);
            context.registerReceiver(this, filter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mEdpClient.sendHeartbeat();
            mEdpClient.setupAlarm(context);
        }
    }
}
