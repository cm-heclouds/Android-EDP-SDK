/*
 * Copyright (C) 2015. China Mobile IOT. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
