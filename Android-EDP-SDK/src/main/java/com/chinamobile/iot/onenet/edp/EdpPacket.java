/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp;

/**
 * Created by chenglei on 2015/12/24.
 */
public class EdpPacket {

    public byte type;
    public int dataLength;
    public byte[] data;

    public EdpPacket() {
        type = 0;
        dataLength = 0;
    }

}
