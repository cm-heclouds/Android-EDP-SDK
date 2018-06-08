/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by chenglei on 2015/12/29.
 */
public class EncryptMsg extends EdpMsg {

    public EncryptMsg() {
        super(Common.MsgType.ENCRYPTREQ);
    }

    public byte[] packMsg(BigInteger modulus, BigInteger publicExponent, int algorithm) {
        ByteBuffer buffer = ByteBuffer.allocate(133).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(publicExponent.intValue());
        byte[] array = modulus.toByteArray();
        if (array[0] == 0) {
            // 如果是正数，直接丢掉符号位
            buffer.put(array, 1, array.length - 1);
        } else {
            buffer.put(array);
        }
        buffer.put((byte) algorithm);
        byte[] edpPkg = packPkg(buffer.array());
        return edpPkg;
    }

}
