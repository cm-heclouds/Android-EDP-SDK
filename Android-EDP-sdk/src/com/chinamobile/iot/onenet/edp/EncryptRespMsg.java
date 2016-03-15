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

import com.chinamobile.iot.onenet.edp.toolbox.RSAUtils;

import java.io.IOException;

/**
 * Created by chenglei on 2015/12/29.
 */
public class EncryptRespMsg extends EdpMsg {

    private String encryptSecretKey;

    public EncryptRespMsg() {
        super(Common.MsgType.ENCRYPTRESP);
    }

    @Override
    public void unpackMsg(byte[] msgData) throws IOException {
        int dataLen = msgData.length;
        if (dataLen < 2) {
            throw new IOException("packet size too short. size:" + dataLen);
        }
        int keyLen = Common.twoByteToLen(msgData[0], msgData[1]);
        if (dataLen >= keyLen + 2) {
            byte[] bytes = new byte[keyLen];
            System.arraycopy(msgData, 2, bytes, 0, keyLen);
            encryptSecretKey = RSAUtils.bcd2Str(bytes);
        }
    }

    public String getEncryptSecretKey() {
        return encryptSecretKey;
    }
}
