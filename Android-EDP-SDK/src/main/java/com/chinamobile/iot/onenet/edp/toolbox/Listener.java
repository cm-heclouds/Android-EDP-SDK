/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.toolbox;

import com.chinamobile.iot.onenet.edp.EdpMsg;

import java.util.List;

/**
 * Created by chenglei on 2015/12/24.
 */
public interface Listener {

    void onReceive(List<EdpMsg> msgList);

    void onFailed(Exception e);

    void onDisconnect();

}
