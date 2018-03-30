package com.seagle.android.net.monitor;

import android.net.NetworkInfo;

/**
 * <h1>一句话功能说明</h1>
 * <p>详细功能描述</P>
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/6/27
 */
public interface WireNetworkEventListener {
    /**
     * 网络已连接。
     * 事件触发前提：设备断网
     * 事件触发条件：WIFI网络或者移动网络连接成功。
     *
     * @param networkInfo 当前网络信息
     */
    void onNetworkConnected(NetworkInfo networkInfo);

    /**
     * 网络已断开。
     * 事件触发前提：设备联网。
     * 事件触发条件：WIFI网络和移动网络都断开。
     *
     * @param networkInfo 断开的网络信息
     */
    void onNetworkDisconnected(NetworkInfo networkInfo);
}
