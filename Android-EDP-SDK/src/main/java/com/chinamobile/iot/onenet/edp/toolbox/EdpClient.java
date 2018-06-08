/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.toolbox;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;

import com.chinamobile.iot.onenet.edp.CmdRespMsg;
import com.chinamobile.iot.onenet.edp.Common;
import com.chinamobile.iot.onenet.edp.ConnectMsg;
import com.chinamobile.iot.onenet.edp.EdpKit;
import com.chinamobile.iot.onenet.edp.EdpMsg;
import com.chinamobile.iot.onenet.edp.EncryptMsg;
import com.chinamobile.iot.onenet.edp.EncryptRespMsg;
import com.chinamobile.iot.onenet.edp.PingMsg;
import com.chinamobile.iot.onenet.edp.PushDataMsg;
import com.chinamobile.iot.onenet.edp.SaveDataMsg;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 该类封装了EDP协议中规定的所有操作。EDP客户端使用该类发送请求，并通过{@link #setListener(Listener)}注册
 * {@link Listener}接收响应。其中接收到连接响应后，会自动启动{@link EdpService}定时发送心跳，默认为3min，
 * 可以通过{@link #setPingInterval(long)}来自定义心跳发送的周期。
 * <p>
 *
 * 调用{@link #connect()}方法后，会自动启动发送和接收两个子线程，接收到响应后，会post到主线程中，所以在
 * {@link Listener}中的回调方法都在主线程中执行。
 * <p>
 *
 * 如果使用加密通信，则在调用{@link #connect()}方法后调用{@link #requestEncrypt(int)}，然后在加密响应回调中
 * 再调用{@link #sendConnectReq()}。
 * <p>
 *
 * 如果使用明文通信，则在{@link #connect()}之后直接调用{@link #sendConnectReq()}发送连接请求。
 * <p>
 *
 * Created by chenglei on 2015/12/24.
 */
public class EdpClient {

    /**
     * 默认超时时间为4min，超过该时间没有发送心跳请求则断开连接
     */
    public static final long DEFAULT_TIMEOUT_MILLI_SECONDS = 4 * 60 * 1000;

    /**
     * 默认自动发送心跳的间隔
     */
    private static final long DEFAULT_PING_INTERVAL = 3 * 60 * 1000;

    /**
     * 连接认证类型1：设备ID + 鉴权信息(apikey)
     */
    public static final int CONNECT_TYPE_1 = 1;

    /**
     * 连接认证类型2：项目ID + 鉴权信息(auth info)
     */
    public static final int CONNECT_TYPE_2 = 2;

    /**
     * 服务器主机名
     */
    private String mHost = "jjfaedp.hedevice.com";

    /**
     * 端口
     */
    private int mPort = 876;

    /**
     * 备用端口
     */
    private int mPort2 = 29876;

    /**
     * 心跳周期
     */
    private long mPingIntervalMilli = DEFAULT_PING_INTERVAL;

    private Socket mEdpSocket;

    private boolean mConnected;

    /**
     * 请求消息队列
     */
    private LinkedBlockingQueue<Message> mMessageQueue = new LinkedBlockingQueue<Message>();

    /**
     * 认证类型
     */
    private int mConnectType;

    private String mDeviceId;
    private String mApikey;
    private String mProjectId;
    private String mAuthInfo;

    private Listener mListener;

    /**
     * 心跳广播的Action
     */
    public static final String ACTION_HEARTBEAT = "onenet.edp.intent.ACTION_HEARTBEAT";

    private static EdpClient sInstance;
    private Context mApplicationContext;

    private Handler mHandler = new Handler();

    private int mEncryptAlgorithm;
    private EdpKit mEdpKit = new EdpKit();

    private String mSectetKey;

    /**
     * 初始化 EdpClient
     * @param context   上下文
     * @param type      连接类型
     * @param id        设备或项目id
     * @param authinfo  鉴权信息(apikey或authinfo)
     */
    public static void initialize(Context context, int type, String id, String authinfo) {
        sInstance = new EdpClient(context, type, id, authinfo);
    }

    public static EdpClient getInstance() {
        if (null == sInstance) {
            throw new RuntimeException("You must call EdpClient.initialize() first");
        }
        return sInstance;
    }

    private EdpClient(Context context, int type, String id, String authinfo) {
        mApplicationContext = context.getApplicationContext();
        mConnectType = type;
        if (CONNECT_TYPE_1 == mConnectType) {
            mDeviceId = id;
            mApikey = authinfo;
        } else if (CONNECT_TYPE_2 == mConnectType) {
            mProjectId = id;
            mAuthInfo = authinfo;
        }
    }

    /**
     * 设置心跳请求周期，默认3min
     *
     * @param milliseconds 心跳周期，毫秒
     */
    public void setPingInterval(long milliseconds) {
        if (milliseconds >= DEFAULT_TIMEOUT_MILLI_SECONDS) {
            throw new IllegalArgumentException("ping interval can not be longer than 4 min");
        }
        mPingIntervalMilli = milliseconds;
    }

    private class ConnectThreat extends Thread {
        @Override
        public void run() {

            try {
                mEdpSocket = new Socket(mHost, mPort);

                Thread.sleep(1000);

                new RecvMessageThread().start();
                new SendMessageThread().start();

            } catch (final IOException e) {
                e.printStackTrace();
                if (mListener != null && mConnected) {
                    mHandler.post(new FailedEvent(e));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private class SendMessageThread extends Thread {

        private Exception mException;

        @Override
        public void run() {

            OutputStream outputStream = null;
            try {
                while (true) {
                    if (null == mEdpSocket) {
                        break;
                    }
                    outputStream = mEdpSocket.getOutputStream();
                    mConnected = true;
                    Message msg = mMessageQueue.take();
                    if (msg.getType() == Message.BYTES) {
                        outputStream.write(msg.getPacket());
                    } else if (msg.getType() == Message.FILE) {
                        sendFile(msg.getFilePath(), outputStream);
                    } else if (msg.getType() == Message.DISCONNECT) {
                        if (mListener != null) {
                            mHandler.post(new DisconnectEvent());
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                mException = e;
            } catch (InterruptedException e) {
                mException = e;
            } finally {
                if (mException != null && mListener != null && mConnected) {
                    mHandler.post(new FailedEvent(mException));
                }
                mConnected = false;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void sendFile(String filePath, OutputStream outputStream) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        byte[] buffer = new byte[1024];
        int size;
        while ((size = fis.read(buffer)) > 0) {
            outputStream.write(buffer, 0, size);
        }
        outputStream.flush();
        fis.close();
    }

    private class RecvMessageThread extends Thread {

        @Override
        public void run() {

            InputStream inputStream = null;
            try {
                while (true) {
                    if (null == mEdpSocket) {
                        break;
                    }
                    inputStream = mEdpSocket.getInputStream();
                    mConnected = true;
                    byte[] recvPacket = readRecvPacket(inputStream);
                    if (null == recvPacket) {
                        break;
                    }
                    List<EdpMsg> msgList = mEdpKit.unpack(recvPacket, mEncryptAlgorithm, mSectetKey);
                    if (mListener != null && msgList != null) {
                        for (EdpMsg msg : msgList) {
                            if (msg != null) {
                                byte type = msg.getMsgType();
                                if (Common.MsgType.CONNRESP == type) {
                                    EdpService.start(mApplicationContext.getApplicationContext());
                                } else if (Common.MsgType.ENCRYPTRESP == type) {
                                    EncryptRespMsg encryptRespMsg = (EncryptRespMsg) msg;
                                    try {
                                        mSectetKey = rsaDecrypt(encryptRespMsg.getEncryptSecretKey());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        mHandler.post(new ReceiveEvent(msgList));
                    }
                }
            } catch (IOException e) {
                if (mListener != null && mConnected) {
                    mHandler.post(new FailedEvent(e));
                }
            } finally {
                Message msg = new Message();
                msg.setType(Message.DISCONNECT);
                enqueueMsg(msg);
                mConnected = false;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private byte[] readRecvPacket(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int size;
        byte[] recvPacket = null;
        int pos = 0;
        while ((size = inputStream.read(buffer)) > 0) {
            if (null == recvPacket) {
                recvPacket = new byte[size];
                System.arraycopy(buffer, 0, recvPacket, pos, size);
            } else {
                byte[] temp = new byte[pos + size];
                System.arraycopy(recvPacket, 0, temp, 0, pos);
                System.arraycopy(buffer, 0, temp, pos, size);
                recvPacket = temp;
            }
            if (size < 1024) {
                break;
            }
            pos += size;
        }
        if (size <= 0) {
            recvPacket = null;
        }
        return recvPacket;
    }

    /**
     * 建立TCP连接
     */
    public void connect() {
        if (mConnected) {
            return;
        }

        new ConnectThreat().start();
    }

    /**
     * 建立TCP连接，指定主机名和端口
     *
     * @param host 主机名
     * @param port 端口
     */
    public void connect(String host, int port) {
        if (TextUtils.isEmpty(host)) {
            throw new IllegalArgumentException("host can not be empty");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("port " + port + " is illegal");
        }
        mHost = host;
        mPort = port;
        connect();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        mConnected = false;
        if (mEdpSocket != null) {
            try {
                mEdpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mEdpSocket = null;
            }
        }
        EdpService.stop(mApplicationContext);
        Message msg = new Message();
        msg.setType(Message.DISCONNECT);
        enqueueMsg(msg);
        mMessageQueue.clear();
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    /**
     * 发送连接请求
     */
    public void sendConnectReq() {
        ConnectMsg connectMsg = new ConnectMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            connectMsg.setAlgorithm(mEncryptAlgorithm);
            connectMsg.setSecretKey(mSectetKey);
        }
        Message msg = new Message();
        msg.setType(Message.BYTES);
        byte[] packet = null;
        switch (mConnectType) {
            case CONNECT_TYPE_1:
                packet = connectMsg.packMsg(mDeviceId, mApikey);
                break;

            case CONNECT_TYPE_2:
                packet = connectMsg.packMsg("0", mProjectId, mAuthInfo);
                break;

            default:
                throw new IllegalArgumentException("Unknown connect type: " + mConnectType);
        }

        msg.setPacket(packet);
        // 发送连接请求
        enqueueMsg(msg);
    }

    /**
     * 心跳请求
     */
    public void sendHeartbeat() {
        PingMsg pingMsg = new PingMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            pingMsg.setAlgorithm(mEncryptAlgorithm);
            pingMsg.setSecretKey(mSectetKey);
        }
        Message msg = new Message();
        msg.setType(Message.BYTES);
        msg.setPacket(pingMsg.packMsg());
        enqueueMsg(msg);
    }

    /**
     * 转发（透传）数据
     *
     * @param deviceId 设备ID
     * @param data     数据
     */
    public void pushData(long deviceId, byte[] data) {
        Message msg = new Message();
        PushDataMsg pushDataMsg = new PushDataMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            pushDataMsg.setAlgorithm(mEncryptAlgorithm);
            pushDataMsg.setSecretKey(mSectetKey);
        }
        try {
            msg.setType(Message.BYTES);
            msg.setPacket(pushDataMsg.packMsg(deviceId, data));
            enqueueMsg(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 转发（透传）数据
     *
     * @param deviceId 设备ID
     * @param data     数据
     */
    public void pushData(long deviceId, String data) {
        Message msg = new Message();
        PushDataMsg pushDataMsg = new PushDataMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            pushDataMsg.setAlgorithm(mEncryptAlgorithm);
            pushDataMsg.setSecretKey(mSectetKey);
        }
        try {
            msg.setType(Message.BYTES);
            msg.setPacket(pushDataMsg.packMsg(deviceId, data));
            enqueueMsg(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 存储（转发）数据
     *
     * @param desDeviceId 目标设备ID
     * @param dataType    数据类型
     * @param tokenStr    token
     * @param data        data
     */
    public void saveData(String desDeviceId, int dataType, String tokenStr, byte[] data) {
        Message msg = new Message();
        SaveDataMsg saveDataMsg = new SaveDataMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            saveDataMsg.setAlgorithm(mEncryptAlgorithm);
            saveDataMsg.setSecretKey(mSectetKey);
        }
        msg.setType(Message.BYTES);
        boolean flag = saveDataMsg.packMsg(desDeviceId, dataType, tokenStr, data);
        if (flag) {
            msg.setPacket(saveDataMsg.commit());
            enqueueMsg(msg);
        }
    }

    /**
     * 发送命令响应
     *
     * @param cmdid cmdid
     * @param data  data
     */
    public void sendCmdResp(String cmdid, byte[] data) {
        Message msg = new Message();
        CmdRespMsg cmdRespMsg = new CmdRespMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            cmdRespMsg.setAlgorithm(mEncryptAlgorithm);
            cmdRespMsg.setSecretKey(mSectetKey);
        }
        msg.setType(Message.BYTES);
        msg.setPacket(cmdRespMsg.packMsg(cmdid, data));
        enqueueMsg(msg);
    }

    /**
     * 发送加密请求
     *
     * @param algorithm 加密类型
     */
    public void requestEncrypt(int algorithm) {
        if (algorithm > Common.Algorithm.NO_ALGORITHM) {
            mEncryptAlgorithm = algorithm;
            try {
                generateEncryptInfo();
                EncryptMsg encryptMsg = new EncryptMsg();
                Message msg = new Message();
                msg.setType(Message.BYTES);
                msg.setPacket(encryptMsg.packMsg(mModulus, mPublicExponent, algorithm));
                enqueueMsg(msg);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 消息队列中的消息
     */
    class Message {

        /**
         * 消息体为文字的类型
         */
        public static final int BYTES = 1;

        /**
         * 消息体为文件
         */
        public static final int FILE = 2;

        /**
         * 断开连接
         */
        public static final int DISCONNECT = 3;

        /**
         * 类型
         */
        private int type;

        /**
         * 完整的EDP消息，文件类型的消息此项无效
         */
        private byte[] packet;

        /**
         * 文件路径
         */
        private String filePath;

        public int getType() {
            return type;
        }

        public Message setType(int type) {
            this.type = type;
            return this;
        }

        public byte[] getPacket() {
            return packet;
        }

        public Message setPacket(byte[] packet) {
            this.packet = packet;
            return this;
        }

        public String getFilePath() {
            return filePath;
        }

        public Message setFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
    }

    public void setupAlarm(Context context) {
        Intent intent = new Intent(ACTION_HEARTBEAT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        setupAlarm(context, mPingIntervalMilli, pendingIntent);
    }

    public void setupAlarm(Context context, long timeIntervalMillis, PendingIntent pendingIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
        long triggerAtMillis = SystemClock.elapsedRealtime() + timeIntervalMillis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(ACTION_HEARTBEAT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        cancelAlarm(context, pendingIntent);
    }

    public void cancelAlarm(Context context, PendingIntent pendingIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    /**
     * 加入消息到发送队列
     * @param msg
     */
    private void enqueueMsg(Message msg) {
        mMessageQueue.offer(msg);
    }

    /**
     * 设备是否在线
     *
     * @return 设备是否在线
     */
    public boolean isConnected() {
        return mConnected;
    }

    private class ReceiveEvent implements Runnable {

        private List<EdpMsg> mMsgList;

        public ReceiveEvent(List<EdpMsg> msgList) {
            mMsgList = msgList;
        }

        @Override
        public void run() {
            if (mListener != null) {
                mListener.onReceive(mMsgList);
            }
        }
    }

    private class FailedEvent implements Runnable {

        private Exception mException;

        public FailedEvent(Exception e) {
            mException = e;
        }

        @Override
        public void run() {
            if (mListener != null) {
                mListener.onFailed(mException);
            }
        }
    }

    private class DisconnectEvent implements Runnable {

        @Override
        public void run() {
            if (mListener != null) {
                mListener.onDisconnect();
            }
        }
    }

    private BigInteger mModulus;
    private BigInteger mPublicExponent;
    private BigInteger mPrivateExponent;
    private RSAPublicKey mPublicKey;
    private RSAPrivateKey mPrivateKey;

    private void generateEncryptInfo() throws NoSuchAlgorithmException {
        HashMap<String, Object> map = RSAUtils.getKeys();
        //生成公钥和私钥
        mPublicKey = (RSAPublicKey) map.get("public");
        mPrivateKey = (RSAPrivateKey) map.get("private");

        //模
        mModulus = mPublicKey.getModulus();
        //公钥指数
        mPublicExponent = mPublicKey.getPublicExponent();
        //私钥指数
        mPrivateExponent = mPrivateKey.getPrivateExponent();
    }

    /**
     * RSA加密
     * @param plainText 明文
     * @return 加密后内容
     * @throws Exception Exception
     */
    public String rsaEncrypt(String plainText) throws Exception {
        if (mPublicKey != null) {
            return RSAUtils.encryptByPublicKey(plainText, mPublicKey);
        } else {
            return plainText;
        }
    }

    /**
     * RSA解密
     * @param cipherText 密文
     * @return 解密后内容
     * @throws Exception Exception
     */
    public String rsaDecrypt(String cipherText) throws Exception {
        if (mPrivateKey != null) {
            return RSAUtils.decryptByPrivateKey(cipherText, mPrivateKey);
        } else {
            return cipherText;
        }
    }

}
