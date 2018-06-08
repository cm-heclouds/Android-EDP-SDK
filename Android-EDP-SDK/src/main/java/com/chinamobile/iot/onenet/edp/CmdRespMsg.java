/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by chenglei on 2015/12/28.
 */
public class CmdRespMsg extends EdpMsg {

    public CmdRespMsg() {
        super(Common.MsgType.CMDRESP);
    }

    public byte[] packMsg(String cmdid, byte[] data) {
        short cmdidLen = (short) cmdid.length();
        int dataLen = cmdidLen + data.length + 6;
        ByteBuffer buffer = ByteBuffer.allocate(dataLen).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(cmdidLen);
        buffer.put(cmdid.getBytes());
        buffer.putInt(data.length);
        buffer.put(data);

        byte[] edpPkg = packPkg(buffer.array());
        return edpPkg;
    }
}
