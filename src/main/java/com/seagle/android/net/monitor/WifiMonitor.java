package com.seagle.android.net.monitor;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Bundle;


import java.util.List;

/**
 * <h1>WIFI连接功能接口</h1>
 * <p>这个类主要实现了WIFI的控制，包括：
 * <ul>
 * <li>WIFI开启/关闭相关功能</li>
 * <li>WIFI扫描相关的功能</li>
 * <li>WIFI连接相关的功能</li>
 * </ul>
 * <p>WIFI连接时涉及到了WIFI安全算法。通过扫描得到WIFI热点的安全算法内容字符串示例：
 * <ul>
 * <li>[WPA/WPA2-PSK-CCMP]：WPA网络</li>
 * <li>[WPA/WPA2-EAP-CCMP]：EAP网络</li>
 * <li>[WEP]：WEP网络</li>
 * <li>OPEN网络：OPEN网路</li>
 * </ul>
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/6/1
 */
public interface WifiMonitor {
    /**
     * 连接网络时：extraData数据参数
     */
    String DATA_KEY_PASSWORD = "password";
    String DATA_KEY_EAP_METHOD = "eap_method";
    String DATA_KEY_PHASE_METHOD = "phase_method";
    String DATA_KEY_IDENTITY = "identity";
    String DATA_KEY_ANONYMOUS = "anonymous_identity";
    int WIFI_PEAP_PHASE2_NONE = 0;
    int WIFI_PEAP_PHASE2_MSCHAPV2 = 1;
    int WIFI_PEAP_PHASE2_GTC = 2;

    /**
     * WIFI网络安全类型
     */
    enum SecurityType {

        /**
         * 未加密。没有密码
         */
        SECURITY_TYPE_NONE,

        /**
         * WEP加密，有密码
         */
        SECURITY_TYPE_WEP,

        /**
         * WAP/WAP2-PSK加密，有密码
         */
        SECURITY_TYPE_PSK,

        /**
         * WAP/WAP2-EAP加密，有登陆用户名和密码
         */
        SECURITY_TYPE_EAP
    }

    /**
     * WIFI是否已经打开
     *
     * @return true-打开
     */
    boolean isWifiEnabled();

    /**
     * 打开WIFI
     *
     * @return true - 打开
     */
    boolean enableWifi();

    /**
     * 关闭WIFI
     *
     * @return true-关闭
     */
    boolean disableWifi();

    /**
     * WIFI是否连接
     *
     * @return true-连接
     */
    boolean isWifiConnected();

    /**
     * 扫描wifi。
     */
    boolean startScanWifiAccessPoint();

    /**
     * 获取WIFI扫描结果
     *
     * @return WIFI扫描结果
     */
    List<ScanResult> getScanResultList();

    /**
     * 获取连接的WIFI信息
     *
     * @return WIFI信息
     */
    WifiInfo getConnectWifiInfo();

    /**
     * 解析WIFI加密类型
     * <p>字符串格式：[WPA-PSK-CCMP][WPA2-PSK-CCMP][ESS],
     *
     * @param capabilities 安全类型字符串
     * @return 安全类型
     */
    public SecurityType parseCapability(String capabilities);

    /**
     * 连接WIFI热点。
     * 不同的安全类型，需要配置不同的extraConfig参数。
     * 通过这种方式添加的网络一般是隐藏的网络。
     * <ul>
     * <li>SECURITY_TYPE_NONE:空</li>
     * <li>SECURITY_TYPE_WEP：需要配置密码</li>
     * <li>SECURITY_TYPE_PSK：需要配置密码</li>
     * <li>SECURITY_TYPE_EAP：需要配置账号和密码</li>
     * </ul>
     *
     * @param ssid      热点名称。
     * @param type      安全类型。
     * @param extraData 安全类型相关的附带配置参数
     * @return 连接成功or失败
     */
    boolean connectWifiAccessPoint(String ssid, SecurityType type, Bundle extraData);

    /**
     * 连接WIFI热点。
     * 在连接WEP，WAP/WAP2-PSK网络时，基本不用配置参数。
     * 但是在连接EAP网络时需要配置账号和密码
     *
     * @param result    wifi扫描结果
     * @param extraData 附带配置参数
     * @return true or false
     */
    boolean connectWifiAccessPoint(ScanResult result, Bundle extraData);

    /**
     * 端口当前WIFI热点
     *
     * @return true-断开
     */
    boolean disconnect();
}
