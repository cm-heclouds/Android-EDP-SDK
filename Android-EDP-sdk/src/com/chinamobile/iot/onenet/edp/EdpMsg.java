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
import android.util.Base64;
import android.util.Log;

import com.chinamobile.iot.onenet.edp.toolbox.AESUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * function:	EDP 消息基类
 * author:		yonghua
 * date:		2015/01/18
 * version:		0.0.1
 */

public class EdpMsg {

    /**
     * 消息类型
     */
    private byte type;

    /**
     * 加密算法
     */
    private int algorithm;

    /**
     * 密钥
     */
    private String secretKey;

    public byte getMsgType() {
        return type;
    }

    public EdpMsg(byte msgType) {
        type = msgType;
    }

    /**
     * 设置密钥
     * @param secretKey
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    /**
     * 指定加密算法
     * @param algorithm 见{@link com.chinamobile.iot.onenet.edp.Common.Algorithm}
     */
    public void setAlgorithm(int algorithm) {
        this.algorithm = algorithm;
    }

    public int getAlgorithm() {
        return this.algorithm;
    }

    public void unpackMsg(byte[] msgData)
            throws IOException {
    }

    public byte[] packPkg(byte[] _msgData) {
        // 判断是否需要加密
        if (!TextUtils.isEmpty(secretKey)) {
            switch (algorithm) {

                // AES加密，加密模式ECB，填充方式ISO10126padding
                case Common.Algorithm.ALGORITHM_AES:
                    try {
                        _msgData = AESUtils.encrypt(_msgData, secretKey.getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }

        int len = _msgData.length;
        byte[] pkgDataLength = packLength(len);
        if (pkgDataLength == null) {
            System.err.println("[packPkg] packet data length exception. "
                    + "data_len=" + len);
            return null;
        }
        int pkgDataLengthSize = pkgDataLength.length;
        int pkgLength = 1 + pkgDataLengthSize + len;
        ByteBuffer packet = ByteBuffer.allocate(pkgLength);

        packet.put(this.type);
        packet.put(pkgDataLength);
        packet.put(_msgData);

        return packet.array();
    }

    //消息长度转换为edp长度格式
    public byte[] packLength(int size) {
        int twoByteMin = 128;
        int threeByteMin = 16384;
        int fourByteMin = 2097152;
        int maxSize = 268435455;
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
        int cnt = 0;
        if (size > maxSize) {
            return null;
        } else if (size >= fourByteMin) {
            cnt = 4;
        } else if (size >= threeByteMin) {
            cnt = 3;
        } else if (size >= twoByteMin) {
            cnt = 2;
        } else {
            cnt = 1;
        }

        byte bSize = 0;
        for (int i = 0; i < cnt; i++) {
            if (i == (cnt - 1)) {
                bSize = (byte) size;
            } else {
                bSize = (byte) ((size & 0x7F) | 0x80);
                size = size >> 7;
            }
            sizeBuffer.put(bSize);
        }

        int bufferSize = sizeBuffer.position();
        byte[] sizeArray = new byte[bufferSize];
        sizeBuffer.flip();
        sizeBuffer.get(sizeArray);
        return sizeArray;
    }

    //检测设备地址长度是否合法，大于等于5，小于10
    public boolean checkAddressLen(int _len) {
        if (_len < 5 || _len > 10) {
            return false;
        } else {
            return true;
        }
    }

}

