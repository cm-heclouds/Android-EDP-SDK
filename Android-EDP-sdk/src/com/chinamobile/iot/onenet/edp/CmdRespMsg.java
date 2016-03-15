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
