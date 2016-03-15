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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PushDataMsg extends EdpMsg {
    private String srcDeviceId;
    private int dataLen;
    private byte[] data;

    public PushDataMsg() {
        super(Common.MsgType.PUSHDATA);
    }

    /*
     * 解析转发消息
     * @see onenet.edp.EdpMsg#unpackMsg(byte[])
     * @param msgData message complete packet
     * @throws IOException if unpack fail.
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

        //数据中必包含 源地址长度和源地址，长度最小为 2 + 5
        if (dataLen < 7) {
            throw new IOException("packet size too short.size:" + dataLen);
        }

        int addressLen = Common.twoByteToLen(msgData[0], msgData[1]);
        int dataRemain = dataLen - 2;
        if (!checkAddressLen(addressLen) || addressLen > (dataLen - 2)) {
            throw new IOException("address size too long.");
        }

        srcDeviceId = new String(msgData, 2, addressLen);
        dataRemain = dataRemain - addressLen;
        if (dataRemain > 0) {
            data = new byte[dataRemain];
            System.arraycopy(msgData, addressLen + 2, data, 0, dataRemain);
        } else {
            throw new IOException("data is null");
        }
    }

    /*
     * 封包edp转发消息
     * @param desDeviceId destination device id
     * @param _data pushed data
     * @return push data packet
     * @throws IOException if pack fail.
     */
    public byte[] packMsg(long desDeviceId, byte[] _data) throws IOException {
        if (desDeviceId <= 0) {
            throw new IOException("desDeviceId invalid. desDeviceid=" + desDeviceId);
        }

        if (_data == null) {
            throw new IOException("send data is null.");
        }

        String desDeviceIdStr = "" + desDeviceId;
        short addressLen = (short) desDeviceIdStr.length();
        int dataLen = 2 + addressLen + _data.length;
        ByteBuffer buffer = ByteBuffer.allocate(dataLen).order(ByteOrder.BIG_ENDIAN);

        buffer.putShort(addressLen);
        buffer.put(desDeviceIdStr.getBytes());
        buffer.put(_data);

        byte[] edpPkg = packPkg(buffer.array());
        return edpPkg;
    }

    public byte[] packMsg(long desDeviceId, String data) throws IOException {
        return packMsg(desDeviceId, data.getBytes());
    }

    public String getSrcDeviceId() {
        return this.srcDeviceId;
    }

    public int getDataLen() {
        return this.dataLen;
    }

    public byte[] getData() {
        return this.data;
    }
}
