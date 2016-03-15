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

/*
 * function:	onenet edp默认参数定义和返回码说明表
 * author:		yonghua
 * date:		2015/01/04
 * version:		0.0.1
 */

public class Common {
    //edp 消息类型
    public class MsgType {
        public static final byte CONNREQ = (byte) 0x10;         // 连接建立请求
        public static final byte CONNRESP = (byte) 0x20;        // 连接响应
        public static final byte PUSHDATA = (byte) 0x30;        // 发送数据
        public static final byte SAVEDATA = (byte) 0x80;        // 保存数据
        public static final byte SAVERESP = (byte) 0x90;        // 保存确认响应
        public static final byte CMDREQ = (byte) 0xA0;          // 命令请求
        public static final byte CMDRESP = (byte) 0xB0;         // 命令响应
        public static final byte PINGREQ = (byte) 0xC0;         // 心跳请求
        public static final byte PINGRESP = (byte) 0xD0;        // 心跳响应
        public static final byte ENCRYPTREQ = (byte) 0xE0;      // 加密请求
        public static final byte ENCRYPTRESP = (byte) 0xF0;     // 加密响应
    }

    //edp 返回码
    public class ConnResp {
        public static final byte ACCEPTED = 0;                        // 连接成功
        public static final byte REFUSED_PROTOCOL_INVALID = 1;        // 协议错误
        public static final byte REFUSED_BAD_DEVID = 2;               // 设备ID鉴权失败
        public static final byte REFUSED_SERVER_UNAVAILABLE = 3;      // 服务器失败
        public static final byte REFUSED_BAD_USERID_PASSWORD = 4;     // 用户ID鉴权失败
        public static final byte REFUSED_NOT_AUTHORIZED = 5;          // 未授权
        public static final byte REFUSED_INVALID_AUTHOR_CODE = 6;     // 无效的鉴权码
        public static final byte REFUSED_INVALID_ACTIVATE_CODE = 7;   // 无效的激活码
        public static final byte REFUSED_HAS_ACTIVATED = 8;           // 该设备已被激活
        public static final byte REFUSED_DUP_AUTHEN = 9;              // 重复发送连接请求包
    }

    //函数的异常返回码
    public class ErrorCode {

    }

    /**
     * 加密算法类型
     */
    public class Algorithm {

        /**
         * 不加密
         */
        public static final int NO_ALGORITHM = -1;

        /**
         * AES加密
         */
        public static final int ALGORITHM_AES = 1;

    }

    public static int twoByteToLen(byte highByte, byte lowByte) {
        int len = (((int) (highByte & 0xFF)) << 8) + (int) (lowByte & 0xFF);
        return len;
    }

    public static int fourByteToLen(byte firstByte, byte secondByte, byte thirdByte, byte fourthByte) {
        int len = firstByte & 0x7F;    //过滤掉异常负数
        len = len << 8;
        len += secondByte & 0xFF;
        len = len << 8;
        len += thirdByte & 0xFF;
        len = len << 8;
        len += fourthByte & 0xFF;
        return len;
    }
}
