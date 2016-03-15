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
