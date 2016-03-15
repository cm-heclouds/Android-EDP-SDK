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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

/**
 * 存储（转发）数据的类封装
 * 支持了5种数据形式，并提供了相应的封装方法
 */
public class SaveDataMsg extends EdpMsg {
    //is or not has forward address
    private boolean hasAddress;
    //the source device id of forward this msg
    private String srcDeviceId;
    //data list
    private List<byte[]> dataList;
    //data type list
    private List<Byte> dataTypeList;
    //bin data token info map
    private Map<Integer, byte[]> binTokenMap;
    //default resolve data buffer size
    private final int DATA_BUFFER_SIZE = 1024;
    //resolve data buffer
    private ByteBuffer dataBuffer;
    //datapoint date formatter
    private static SimpleDateFormat DATAPOINT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    //default save data type 5 msg configuration
    private static String saveData5MsgSeparator = ",";
    private static String savaData5MsgNewLine = ";";

    public SaveDataMsg() {
        super(Common.MsgType.SAVEDATA);
        dataList = new ArrayList<byte[]>();
        dataTypeList = new ArrayList<Byte>();
        binTokenMap = new HashMap<Integer, byte[]>();
        dataBuffer = ByteBuffer.allocate(DATA_BUFFER_SIZE).order(ByteOrder.BIG_ENDIAN);
    }

    /**
     * 封装格式1的消息，若有多个数据点，可多次调用，统一封装
     *
     * @param msg    save data content. Json instance must be initialized.
     * @param token  token字符串，若有，只需要提供一次，若多次提供，以最后一次为准，若不提供，可置为null
     * @param dsId   datastream id
     * @param dpTime datapoint time
     * @param value  datapoint value
     */
    public static void packSaveData1Msg(JSONObject msg, String token, String dsId, Date dpTime, Object value) throws JSONException {
        if (token != null) {
            msg.put("token", token);
        }

        //封装好单个数据点
        JSONObject dp = new JSONObject();
        if (dpTime != null) {
            dp.put("at", DATAPOINT_DATE_FORMAT.format(dpTime));
        }
        dp.put("value", value);

        //检查数据流ID是否已经有数据点，并分情况处理
        //msg已经含有数据点的处理
        if (msg.has("datastreams")) {
            JSONArray datastreams = msg.getJSONArray("datastreams");
            int index = 0;
            for (; index < datastreams.length(); index++) {
                JSONObject datastream = datastreams.getJSONObject(index);
                String tempDsId = datastream.getString("id");
                if (dsId.compareTo(tempDsId) == 0) {
                    //datastream.append("datapoints", dp);
                    JSONArray datapoints = datastream.getJSONArray("datapoints");
                    datapoints.put(dp);
                    break;
                }
            }
            if (index == datastreams.length()) {
                JSONObject tempDatastream = new JSONObject();
                tempDatastream.put("id", dsId);
                JSONArray datapoints = new JSONArray();
                datapoints.put(dp);
                tempDatastream.put("datapoints", datapoints);
                datastreams.put(tempDatastream);
                //tempDatastream.append("datapoints", dp);
                //msg.append("datastreams", tempDatastream);
            }
        } else {    //不含有数据点的处理
            JSONObject tempDatastream = new JSONObject();
            tempDatastream.put("id", dsId);
            JSONArray datapoints = new JSONArray();
            datapoints.put(dp);
            tempDatastream.put("datapoints", datapoints);
            JSONArray datastreams = new JSONArray();
            datastreams.put(tempDatastream);
            msg.put("datastreams", datastreams);
            //tempDatastream.append("datapoints", dp);
            //msg.append("datastreams", tempDatastream);
        }
    }

    /**
     * 封装格式2的token信息。dsId为必填值，其他选项，若无，可置为null
     *
     * @param token    token information. Json instance must be initialized.
     * @param tokenStr token string
     * @param dsId     datastream id
     * @param dpTime   datapoint time
     * @param desc
     */
    public static void packSaveData2Token(JSONObject token, String tokenStr, String dsId,
                                          Date dpTime, String desc) throws JSONException {
        token.put("ds_id", dsId);
        if (tokenStr != null) {
            token.put("token", tokenStr);
        }
        if (dpTime != null) {
            token.put("at", DATAPOINT_DATE_FORMAT.format(dpTime));
        }
        if (desc != null) {
            token.put("desc", desc);
        }
    }

    /**
     * 封装格式3的消息，若有多个数据点，请多次调用,若某个数据流多次赋值，以最后一直值为准
     *
     * @param msg   save data content. Json instance must be initialized.
     * @param dsId  datastream id
     * @param value datapoint value
     */
    public static void packSaveData3Msg(JSONObject msg, String dsId, Object value) throws JSONException {
        msg.put(dsId, value);
    }

    /**
     * 封装格式4的消息，若有多个数据，请多次调用
     *
     * @param msg    save data content. Json instance must be initialized.
     * @param dsId   datastream id
     * @param dpTime datapoint time
     * @param value  datapoint value
     */
    public static void packSaveData4Msg(JSONObject msg, String dsId, Date dpTime,
                                        Object value) throws JSONException {
        //格式化时间
        String dpTimeStr = DATAPOINT_DATE_FORMAT.format(dpTime);

        if (msg.has(dsId)) {
            msg.getJSONObject(dsId).put(dpTimeStr, value);
        } else {
            JSONObject datastream = new JSONObject();
            datastream.put(dpTimeStr, value);
            msg.put(dsId, datastream);
        }
    }

    /**
     * 设置格式5消息的分隔符。可以不设，默认分隔符为","和";"
     *
     * @param separator field separator
     * @param newLine   line separator
     */
    public static void setSaveData5MsgSeparator(String separator, String newLine) {
        saveData5MsgSeparator = separator;
        savaData5MsgNewLine = newLine;
    }

    /**
     * 封装格式5的消息，若有多个消息，请多次调用
     *
     * @param msg    save data content.StringBuilder instance must be initialized.
     * @param value  datapoint value
     * @param dsId   datastream id
     * @param dpTime datapoint time
     */
    public static void packetSaveData5Msg(StringBuilder msg, Object value,
                                          String dsId, Date dpTime) {
        //判断消息是否为首条消息，首条消息需添加双分隔符，非首条消息，添加行分隔符
        if (msg.length() < 2) {
            msg.append(saveData5MsgSeparator).append(savaData5MsgNewLine);
        } else {
            msg.append(savaData5MsgNewLine);
        }

        msg.append(dsId).append(saveData5MsgSeparator);
        msg.append(DATAPOINT_DATE_FORMAT.format(dpTime)).append(saveData5MsgSeparator);
        msg.append(value);
    }

    /**
     * 封装格式5的消息，若有多个消息，请多次调用
     *
     * @param msg   save data content.StringBuilder instance must be initialized.
     * @param value datapoint value
     * @param dsId  datastream id
     */
    public static void packetSaveData5Msg(StringBuilder msg, Object value,
                                          String dsId) {
        //判断消息是否为首条消息，首条消息需添加双分隔符，非首条消息，添加行分隔符
        if (msg.length() < 2) {
            msg.append(saveData5MsgSeparator).append(savaData5MsgNewLine);
        } else {
            msg.append(savaData5MsgNewLine);
        }

        msg.append(dsId).append(saveData5MsgSeparator);
        msg.append(value);
    }

    /**
     * 封装格式5的消息，若有多个消息，请多次调用
     *
     * @param msg   save data content.StringBuilder instance must be initialized.
     * @param value datapoint value
     */
    public static void packetSaveData5Msg(StringBuilder msg, Object value) {
        //判断消息是否为首条消息，首条消息需添加双分隔符，非首条消息，添加行分隔符
        if (msg.length() < 2) {
            msg.append(saveData5MsgSeparator).append(savaData5MsgNewLine);
        } else {
            msg.append(savaData5MsgNewLine);
        }

        msg.append(value);
    }

    /**
     * unpack save data msg
     *
     * @param msgData
     * @throws IOException
     * @see com.chinamobile.iot.onenet.edp.EdpMsg#unpackMsg(byte[])
     */
    @Override
    public void unpackMsg(byte[] msgData) throws IOException {
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
        int dataRemain = dataLen - 1;
        int position = 1;
        if (msgData[0] == (byte) 0x80) {
            hasAddress = true;
        } else {
            hasAddress = false;
        }
        if (hasAddress) {
            if (dataLen < 8) {
                throw new IOException("save data length too short. dataLen=" + dataLen);
            }

            int addressLen = Common.twoByteToLen(msgData[1], msgData[2]);
            position += 2;
            if (!checkAddressLen(addressLen) || addressLen > (dataLen - 3)) {
                throw new IOException("address length exception. addressLen=" + addressLen);
            }
            srcDeviceId = new String(msgData, 3, addressLen);
            position += addressLen;
            dataRemain = dataRemain - 2 - addressLen;
        }

        int index = 0;
        while (dataRemain > 0) {
            if (dataRemain < 3) {
                throw new IOException("data remain length too short.");
            }
            byte dataType = msgData[position];
            position += 1;
            int datapointLen = Common.twoByteToLen(msgData[position], msgData[position + 1]);
            position += 2;
            dataRemain = dataRemain - 3;

            if (datapointLen > dataRemain) {
                throw new IOException("datapoint_length too long. datapointLen=" + datapointLen
                        + " dataRemain=" + dataRemain);
            }

            byte[] datapoint = new byte[datapointLen];
            System.arraycopy(msgData, position, datapoint, 0, datapointLen);
            position += datapointLen;
            dataRemain = dataRemain - datapointLen;

            //根据数据类型，分情况处理
            if (dataType < 1 || dataType > 5) {
                throw new IOException("not supported data_type:" + dataType);
            } else if (dataType != 2) {
                dataTypeList.add(dataType);
                dataList.add(datapoint);
            } else {    //特殊处理二进制数据
                if (dataRemain < 4) {
                    throw new IOException("[save_data] bin_data no enouth length bytes.");
                }
                int binDataLen = Common.fourByteToLen(msgData[position],
                        msgData[position + 1], msgData[position + 2], msgData[position + 3]);
                position += 4;
                dataRemain = dataRemain - 4;
                if (binDataLen > dataRemain) {
                    throw new IOException("[sava_data] bin_data too long.length="
                            + binDataLen + " dataRemain=" + dataRemain);
                }

                byte[] binData = new byte[binDataLen];
                System.arraycopy(msgData, position, binData, 0, binDataLen);
                dataTypeList.add(dataType);
                binTokenMap.put(index, datapoint);
                dataList.add(binData);

                position += binDataLen;
                dataRemain = dataRemain - binDataLen;
            }

            index++;
        }

    }

    /**
     * 封包存储消息包首个包，后续添加数据点使用addDatapoint()
     *
     * @param desDeviceId destination device id.if don't have desDeviceId, set 0.
     * @param dataType    datapoint type
     * @param tokenStr    token string.only type 2 data effect.
     * @param _data       data content
     * @return if pack fail, return false.
     */
    public boolean packMsg(String desDeviceId, int dataType,
                           String tokenStr, byte[] _data) {
        dataBuffer.clear();
        boolean hasDeviceId = true;
        //检查是否含有转发地址
        if (TextUtils.isEmpty(desDeviceId)) {
            hasDeviceId = false;
        }

        if (hasDeviceId) {
            dataBuffer.put((byte) 0x80);
            short idLen = (short) desDeviceId.length();
            dataBuffer.putShort(idLen);
            dataBuffer.put(desDeviceId.getBytes());
        } else {
            dataBuffer.put((byte) 0x00);
        }

        return addDatapoint(dataType, tokenStr, _data);
    }

    /**
     * 封包存储消息包首个包，后续添加数据点使用addDatapoint()
     *
     * @param dataType datapoint type
     * @param tokenStr token string.only type 2 data effect.
     * @param _data    data content
     * @return if pack fail, return false.
     */
    public boolean packMsg(int dataType, String tokenStr, byte[] _data) {
        return packMsg("0", dataType, tokenStr, _data);
    }

    /**
     * 添加存储edp消息数据点
     *
     * @param dataType datapoint type
     * @param tokenStr token string
     * @param _data    data content
     * @return if add fail,return false.
     */
    public boolean addDatapoint(int dataType, String tokenStr, byte[] _data) {
        if (dataType == 1 || (dataType > 2 && dataType <= 5)) {
            int needBufferSize = 1 + 2 + _data.length;
            //空间不足，扩容
            if (needBufferSize > dataBuffer.remaining()) {
                int newBufferSize = needBufferSize + dataBuffer.position();
                ByteBuffer newBuffer = ByteBuffer.allocate(newBufferSize).order(ByteOrder.BIG_ENDIAN);
                byte[] oldData = new byte[dataBuffer.position()];
                dataBuffer.flip();
                dataBuffer.get(oldData);
                newBuffer.put(oldData);
                dataBuffer = newBuffer;
            }
            dataBuffer.put((byte) dataType);
            short dataLen = (short) _data.length;
            dataBuffer.putShort(dataLen);
            dataBuffer.put(_data);
            return true;
        } else if (dataType == 2) {
            short tokenLen = 0;
            if (tokenStr == null) {
                tokenLen = 0;
            } else {
                tokenLen = (short) tokenStr.length();
            }
            int needBufferSize = 1 + 2 + tokenLen + 4 + _data.length;
            //空间不足，扩容
            if (needBufferSize > dataBuffer.remaining()) {
                int newBufferSize = needBufferSize + dataBuffer.position();
                ByteBuffer newBuffer = ByteBuffer.allocate(newBufferSize).order(ByteOrder.BIG_ENDIAN);
                byte[] oldData = new byte[dataBuffer.position()];
                dataBuffer.flip();
                dataBuffer.get(oldData);
                newBuffer.put(oldData);
                dataBuffer = newBuffer;
            }
            dataBuffer.put((byte) 0x02);
            dataBuffer.putShort(tokenLen);
            if (tokenLen > 0) {
                dataBuffer.put(tokenStr.getBytes());
            }
            int dataLen = _data.length;
            dataBuffer.putInt(dataLen);
            dataBuffer.put(_data);
            return true;
        } else {
            System.err.println("[save_data] packMsg dataType exception. dataType=" + dataType);
            return false;
        }
    }

    /**
     * 添加存储edp消息数据点
     *
     * @param dataType datapoint type
     * @param tokenStr token string.only type 2 data effect.
     * @param data     data content
     * @return if add fail,return false.
     */
    public boolean addDatapoint(int dataType, String tokenStr, String data) {
        return addDatapoint(dataType, tokenStr, data.getBytes());
    }

    /**
     * 添加多个数据点后，生成一个统一的edp 存储消息包
     *
     * @return edp packet
     */
    public byte[] commit() {
        int dataSize = dataBuffer.position();
        byte[] msgData = new byte[dataSize];
        dataBuffer.flip();
        dataBuffer.get(msgData);

        byte[] edpPkg = packPkg(msgData);
        return edpPkg;
    }

    public boolean getHasAddress() {
        return this.hasAddress;
    }

    public String getSrcDeviceId() {
        return this.srcDeviceId;
    }

    public List<Byte> getDataTypeList() {
        return this.dataTypeList;
    }

    public List<byte[]> getDataList() {
        return this.dataList;
    }
}
