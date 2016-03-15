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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * edp 连接请求消息操作类，支持该消息的封装
 *
 * @author yonghua
 *         date:2015/08/07
 */
public class ConnectMsg extends EdpMsg {
    public ConnectMsg() {
        super(Common.MsgType.CONNREQ);
    }

    /**
     * 封装edp连接请求的消息报文
     *
     * @param deviceId       device id
     * @param userId         user id
     * @param authInfo       authentication information (e.g. master-key)
     * @param connectTimeout connect timeout
     * @return packet
     */
    public byte[] packMsg(String deviceId, String userId, String authInfo, short connectTimeout) {
        if (authInfo == null) {
            return null;
        }

        ByteBuffer data = ByteBuffer.allocate(256).order(ByteOrder.BIG_ENDIAN);

        //协议描述
        data.putShort((short) EdpKit.EDP_PROTOCOL.length());
        data.put(EdpKit.EDP_PROTOCOL.getBytes());

        //协议版本
        data.put((byte) 0x01);

        //连接标志
        if (!"0".equals(userId)) {
            data.put((byte) 0xC0);
        } else {
            data.put((byte) 0x40);
        }

        //保持连接时间，单位为秒
        data.putShort(connectTimeout);

        //设备ID
        if (!TextUtils.isEmpty(deviceId)) {
            short strLen = (short) deviceId.length();
            data.putShort(strLen);
            data.put(deviceId.getBytes());
        } else {
            data.putShort((short) 0);
        }

        //用户ID
        if (!TextUtils.isEmpty(userId) && !"0".equals(userId)) {
            short strLen = (short) userId.length();
            data.putShort(strLen);
            data.put(userId.getBytes());
        }

        //鉴权信息
        short infoLen = (byte) authInfo.length();
        data.putShort(infoLen);
        data.put(authInfo.getBytes());

        int packetSize = data.position();
        byte[] packet = new byte[packetSize];
        data.flip();
        data.get(packet);

        byte[] edpPkg = packPkg(packet);
        return edpPkg;
    }

    /**
     * 封装edp连接请求的消息报文
     *
     * @param deviceId device id
     * @param userId   user id. if don't set userId, set 0.
     * @param authInfo authentication information (e.g. master-key)
     * @return packet
     */
    public byte[] packMsg(String deviceId, String userId, String authInfo) {
        return packMsg(deviceId, userId, authInfo, (short) 300);    //默认时间设置为5分钟
    }

    /**
     * 封装edp连接请求的消息报文
     *
     * @param deviceId device id
     * @param authInfo authentication information (e.g. master-key)
     * @return packet
     */
    public byte[] packMsg(String deviceId, String authInfo) {
        return packMsg(deviceId, "0", authInfo, (short) 300);
    }
}
