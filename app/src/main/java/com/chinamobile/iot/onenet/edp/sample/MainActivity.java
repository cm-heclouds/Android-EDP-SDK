/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.chinamobile.iot.onenet.edp.CmdMsg;
import com.chinamobile.iot.onenet.edp.Common;
import com.chinamobile.iot.onenet.edp.ConnectCloseMsg;
import com.chinamobile.iot.onenet.edp.ConnectRespMsg;
import com.chinamobile.iot.onenet.edp.EdpMsg;
import com.chinamobile.iot.onenet.edp.EncryptRespMsg;
import com.chinamobile.iot.onenet.edp.PingRespMsg;
import com.chinamobile.iot.onenet.edp.PushDataMsg;
import com.chinamobile.iot.onenet.edp.SaveDataMsg;
import com.chinamobile.iot.onenet.edp.SaveRespMsg;
import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;
import com.chinamobile.iot.onenet.edp.toolbox.Listener;
import com.chinamobile.iot.onenet.edp.toolbox.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenglei on 2015/12/25.
 */
public class MainActivity extends AppCompatActivity {

    private static final String EXTRA_CONNECT_TYPE = "extra_connect_type";
    private static final String EXTRA_ENCRYPT_TYPE = "extra_encrypt_type";
    private static final String EXTRA_ID = "extra_id";
    private static final String EXTRA_AUTH_INFO = "extra_auth_info";

    private ArrayList<String> mLogList = new ArrayList<String>();

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private RequestLogFragment mRequestLogFragment;
    private GpsFragment mGpsFragment;
    private FileFragment mFileFragment;
    private FragmentManager mFragmentManager;

    private RadioGroup mLeftDrawer;

    public String mDeviceId;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    public static void actionMain(Context context, String id, String authInfo, int connectType, int encryptType) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_CONNECT_TYPE, connectType);
        intent.putExtra(EXTRA_ENCRYPT_TYPE, encryptType);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_AUTH_INFO, authInfo);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake_lock");
        mWakeLock.acquire();

        String id = getIntent().getStringExtra(EXTRA_ID);
        String authInfo = getIntent().getStringExtra(EXTRA_AUTH_INFO);
        int connetcType = getIntent().getIntExtra(EXTRA_CONNECT_TYPE, 1);
        int encryptType = getIntent().getIntExtra(EXTRA_ENCRYPT_TYPE, -1);
        if (EdpClient.CONNECT_TYPE_1 == connetcType) {
            mDeviceId = id;
        }

        initFragments();
        initViews();

        // 1、初始化SDK
        EdpClient.initialize(this, connetcType, id, authInfo);

        // 2、设置接收响应的回调
        EdpClient.getInstance().setListener(mEdpListener);

        // 3、设置自动发送心跳的周期（默认4min）
        EdpClient.getInstance().setPingInterval(3 * 60 * 1000);

        // 4、建立TCP连接
        EdpClient.getInstance().connect();

        if (Common.Algorithm.NO_ALGORITHM == encryptType) {
            // 5、如果使用明文通信，则建立连接后直接发送连接请求
            EdpClient.getInstance().sendConnectReq();
        } else if (Common.Algorithm.ALGORITHM_AES == encryptType) {
            // 6、如果使用加密通信，则先发送加密请求，然后在加密响应回调中发送连接请求
            EdpClient.getInstance().requestEncrypt(Common.Algorithm.ALGORITHM_AES);
        }

    }

    private void initFragments() {
        mRequestLogFragment = RequestLogFragment.newInstance(mLogList);
        mGpsFragment = GpsFragment.newInstance();
        mFileFragment = FileFragment.newInstance();
    }

    private void initViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("请求日志");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close);
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mLeftDrawer = (RadioGroup) findViewById(R.id.left_drawer);
        mLeftDrawer.setOnCheckedChangeListener(mOnCheckedChangeListener);

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.beginTransaction().replace(R.id.content_frame, mRequestLogFragment).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EdpClient.getInstance().isConnected()) {
            EdpClient.getInstance().disconnect();
        }
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private Listener mEdpListener = new Listener() {

        @Override
        public void onReceive(List<EdpMsg> msgList) {
            if (null == msgList) {
                return;
            }
            for (EdpMsg msg : msgList) {
                if (null == msg) {
                    continue;
                }
                switch (msg.getMsgType()) {

                    // 连接响应
                    case Common.MsgType.CONNRESP:

                        ConnectRespMsg connectRespMsg = (ConnectRespMsg) msg;
                        Logger.i(App.LOG_TAG, "连接响应码: " + connectRespMsg.getResCode());
                        if (connectRespMsg.getResCode() == Common.ConnResp.ACCEPTED) {
                            Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
                        }
                        mLogList.add("连接响应\n\n响应码 = " + connectRespMsg.getResCode());

                        break;

                    // 心跳响应
                    case Common.MsgType.PINGRESP:

                        PingRespMsg pingRespMsg = (PingRespMsg) msg;
                        Logger.i(App.LOG_TAG, "心跳响应");
                        Toast.makeText(getApplicationContext(), "心跳响应", Toast.LENGTH_SHORT).show();
                        mLogList.add("心跳响应");

                        break;

                    // 存储确认
                    case Common.MsgType.SAVERESP:

                        SaveRespMsg saveRespMsg = (SaveRespMsg) msg;
                        Logger.i(App.LOG_TAG, "存储确认: " + new String(saveRespMsg.getData()));
                        mLogList.add("存储确认: " + new String(saveRespMsg.getData()));

                        NotificationController.getInstance(MainActivity.this).notifyMessage("存储确认: " + new String(saveRespMsg.getData()));

                        break;

                    // 转发（透传）
                    case Common.MsgType.PUSHDATA:

                        PushDataMsg pushDataMsg = (PushDataMsg) msg;
                        Logger.i(App.LOG_TAG, "透传数据: " + new String(pushDataMsg.getData()));
                        mLogList.add("透传数据: " + new String(pushDataMsg.getData()));
                        NotificationController.getInstance(MainActivity.this).notifyMessage("透传数据: " + new String(pushDataMsg.getData()));
                        break;

                    // 存储（转发）
                    case Common.MsgType.SAVEDATA:

                        SaveDataMsg saveDataMsg = (SaveDataMsg) msg;
                        for (byte[] bytes : saveDataMsg.getDataList()) {
                            Logger.i(App.LOG_TAG, "存储数据: " + new String(bytes));
                            //mLogList.add("存储数据: " + new String(bytes));
                            NotificationController.getInstance(MainActivity.this).notifyMessage("存储数据: " + new String(bytes));
                        }
                        break;

                    // 命令请求
                    case Common.MsgType.CMDREQ:

                        CmdMsg cmdMsg = (CmdMsg) msg;
                        Logger.i(App.LOG_TAG, "cmdid: " + cmdMsg.getCmdId() + "\n命令请求内容: " + new String(cmdMsg.getData()));
                        Toast.makeText(getApplicationContext(), "cmdid: " + cmdMsg.getCmdId() + "\n命令请求内容: " + new String(cmdMsg.getData()), Toast.LENGTH_LONG).show();
                        mLogList.add("命令请求：\ncmdid: " + cmdMsg.getCmdId() + "\ndata: " + new String(cmdMsg.getData()));

                        NotificationController.getInstance(MainActivity.this).notifyMessage("命令请求：\ncmdid: " + cmdMsg.getCmdId() + "\ndata: " + new String(cmdMsg.getData()));

                        // 发送命令响应
                        EdpClient.getInstance().sendCmdResp(cmdMsg.getCmdId(), "发送命令成功".getBytes());
                        break;

                    // 加密响应
                    case Common.MsgType.ENCRYPTRESP:
                        Logger.i(App.LOG_TAG, "加密响应");
                        EncryptRespMsg encryptRespMsg = (EncryptRespMsg) msg;
                        String key = encryptRespMsg.getEncryptSecretKey();
                        Logger.i(App.LOG_TAG, "加密密钥 = " + key);
                        try {
                            String keyPlain = EdpClient.getInstance().rsaDecrypt(key);
                            Logger.i(App.LOG_TAG, "原始密钥 = " + keyPlain);
                            mLogList.add("加密响应\n\n加密密钥 = " + key + "\n\n原始密钥 = " + keyPlain);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        EdpClient.getInstance().sendConnectReq();
                        break;

                    case Common.MsgType.CONNCLOSE:
                        Logger.i(App.LOG_TAG, "连接关闭");
                        ConnectCloseMsg connectCloseMsg = (ConnectCloseMsg) msg;
                        byte errorCode = connectCloseMsg.getErrorCode();
                        mLogList.add("连接关闭, 错误码" + errorCode);
                        NotificationController.getInstance(MainActivity.this).notifyMessage("连接关闭, 错误码" + errorCode);
                        break;
                }
                mRequestLogFragment.updateList(mLogList);
            }
        }

        @Override
        public void onFailed(Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("网络发生异常，连接已断开，请重新连接")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SignInActivity.actionSignIn(MainActivity.this);
                            finish();
                        }
                    })
                    .show();
        }

        @Override
        public void onDisconnect() {
            Toast.makeText(getApplicationContext(), "连接断开", Toast.LENGTH_SHORT).show();
        }
    };

    private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.radio_log:
                    mFragmentManager.beginTransaction().replace(R.id.content_frame, mRequestLogFragment).commit();
                    getSupportActionBar().setTitle("请求日志");
                    break;

                case R.id.radio_gps:
                    mFragmentManager.beginTransaction().replace(R.id.content_frame, mGpsFragment).commit();
                    getSupportActionBar().setTitle("上传GPS数据");
                    break;

                case R.id.radio_file:
                    mFragmentManager.beginTransaction().replace(R.id.content_frame, mFileFragment).commit();
                    getSupportActionBar().setTitle("上传文件");
            }

            mDrawerLayout.closeDrawers();
        }
    };

    @Override
    public void onBackPressed() {
        if (mFileFragment != null && mFileFragment.onBackPressed()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("退出将断开连接，确认继续？")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EdpClient.getInstance().disconnect();
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reconnect:
                EdpClient.getInstance().disconnect();
                SignInActivity.actionSignIn(MainActivity.this);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int type = intent.getIntExtra("type", -1);
        switch (type) {
            case NotificationController.TYPE_CMD:
                mLeftDrawer.check(R.id.radio_log);
                break;
        }

    }
}
