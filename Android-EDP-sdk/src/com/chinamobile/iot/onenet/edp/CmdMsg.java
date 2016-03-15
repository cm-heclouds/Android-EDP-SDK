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

package com.chinamobile.iot.onenet.edp;

import android.text.TextUtils;

import com.chinamobile.iot.onenet.edp.toolbox.AESUtils;

import java.io.IOException;

/**
 * Created by chenglei on 2015/12/25.
 */
public class CmdMsg extends EdpMsg {

    private String cmdId;
    private byte[] data;

    public CmdMsg() {
        super(Common.MsgType.CMDREQ);
    }

    public byte[] packMsg() {
        return null;
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
        // 命令请求报文最小长度为8
        if (dataLen < 8) {
            throw new IOException("packet size too short. size:" + dataLen);
        }

        // 计算cmdid长度
        int cmdIdLen = Common.twoByteToLen(msgData[0], msgData[1]);
        // 计算命令消息体长度
        int respDataLen = Common.fourByteToLen(msgData[cmdIdLen + 2], msgData[cmdIdLen + 3], msgData[cmdIdLen + 4], msgData[cmdIdLen + 5]);

        int dataRemain = dataLen - cmdIdLen - 6;

        cmdId = new String(msgData, 2, cmdIdLen);
        if (respDataLen > 0 && dataRemain > 0) {
            respDataLen = Math.min(dataRemain, respDataLen);
            data = new byte[respDataLen];
            System.arraycopy(msgData, cmdIdLen + 6, data, 0, respDataLen);
        }
    }

    public String getCmdId() {
        return cmdId;
    }

    public byte[] getData() {
        return data;
    }
}
