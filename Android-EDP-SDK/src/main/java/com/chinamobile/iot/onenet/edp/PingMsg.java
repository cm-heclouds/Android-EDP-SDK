/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp;

public class PingMsg extends EdpMsg {
    private byte[] pingMsg;

    public PingMsg() {
        super(Common.MsgType.PINGREQ);
        byte[] msg = {Common.MsgType.PINGREQ, 0x00};
        pingMsg = msg;
    }

    public byte[] packMsg() {
        return this.pingMsg;
    }
}
