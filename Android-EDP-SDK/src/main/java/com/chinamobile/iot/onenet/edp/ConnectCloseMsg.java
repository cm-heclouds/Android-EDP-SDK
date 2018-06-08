/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp;

import android.text.TextUtils;

import com.chinamobile.iot.onenet.edp.toolbox.AESUtils;

import java.io.IOException;

public class ConnectCloseMsg extends EdpMsg {

    private byte errorCode;

    public ConnectCloseMsg() {
        super(Common.MsgType.CONNCLOSE);
    }

    @Override
    public void unpackMsg(byte[] msgData) throws IOException {

        if (!TextUtils.isEmpty(getSecretKey())) {
            switch (getAlgorithm()) {

                // AES加密，加密模式ECB，填充方式ISO10126padding
                case Common.Algorithm.ALGORITHM_AES:
                    try {
                        msgData = AESUtils.decrypt(msgData, getSecretKey().getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }

        int dataLen = msgData.length;

        // 剩余消息长度为 1
        if (dataLen < 1) {
            throw new IOException("packet size too short. size:" + dataLen);
        }

        this.errorCode = msgData[0];

    }

    public byte getErrorCode() {
        return this.errorCode;
    }

}
