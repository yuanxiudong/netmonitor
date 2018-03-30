package com.seagle.android.net.monitor;

import android.net.NetworkInfo;

/**
 * <h1>网络事件</h1>
 * <p>网络事件包含wifi网络和移动网络。网络事件包括：连接，断开，切换</P>
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/5/18
 */
public interface NetworkEventListener {

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

    /**
     * 网络切换。
     * 事件触发前提：设备联网。
     * 事件触发条件；WIFI网络之间的切换或移动网络直接的切换或者WIFI与移动网络之间的切换
     *
     * @param preNetworkInfo 切换前网络信息
     * @param newNetworkInfo 当前网络信息
     */
    void onNetworkChanged(NetworkInfo preNetworkInfo, NetworkInfo newNetworkInfo);
}
