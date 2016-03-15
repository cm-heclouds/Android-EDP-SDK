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


public class ConnectRespMsg extends EdpMsg {
    private boolean hasLicenseCode;    //是否包含授权码
    private byte resCode;            //连接操作返回码
    private int licenseCodeLen;        //授权码长度
    private String licenseCode;        //授权码

    ConnectRespMsg() {
        super(Common.MsgType.CONNRESP);
        hasLicenseCode = false;
    }

    /*
     * unpack connect responce msg
     * check if has license code ,decode licence code.
     * @param msgData packet
     * @see onenet.edp.EdpMsg#unpackMsg(byte[])
     * @throws IOException if packet size is exception
     */
    @Override
    public void unpackMsg(byte[] msgData)
            throws IOException {
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
        //连接响应报文最小长度为2
        if (dataLen < 2) {
            throw new IOException("packet size too short.size:" + dataLen);
        }

        this.resCode = msgData[1];
        if (msgData[0] == 1) {
            this.hasLicenseCode = true;
            if (dataLen < 4) {
                throw new IOException("packet has license code but size too short.size:" + dataLen);
            }
            int licenseCodeLen = Common.twoByteToLen(msgData[2], msgData[3]);
            int dataRemain = dataLen - 4;
            if (dataRemain < licenseCodeLen) {
                throw new IOException("packet remain size shorter than license code size");
            }
            this.licenseCode = new String(msgData, 4, licenseCodeLen);
        } else {
            this.hasLicenseCode = false;
        }
    }

    public byte getResCode() {
        return this.resCode;
    }

    public boolean getHasLicenseCode() {
        return this.hasLicenseCode;
    }

    public int getLicenseCodeLen() {
        return this.licenseCodeLen;
    }

    public String getLicenseCode() {
        return this.licenseCode;
    }
}
