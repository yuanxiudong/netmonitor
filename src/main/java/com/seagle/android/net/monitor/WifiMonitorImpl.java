package com.seagle.android.net.monitor;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * <h1>WIFI连接功能接口实现</h1>
 * <p>无线网络基础只是普及：<br>
 * WPA/WPA2属于无线安全协议（802.11协议范畴），PSK和EAP属于实现方式。PSK用于普通的个人网络，<br>
 * EAP用于企业级网路，一般需要输入用户名和密码进行验证。CCMP/TKIP/（AES属于CCMP）等属于加密方式。<br>
 * keyMgmt：WPA/WPA2-PSK,WEP以及OPEN。<br>
 * Group_Cipher：CCMP/TKIP
 * <p>坑爹的问题：<br>
 * 6.0系统需要两个权限ACCESS_COARSE_LOCATION和ACCESS_FINE_LOCATION才能够扫描到设备。<br>
 * 而且必须打开手机的定位，必须打开手机的定位，打开手机的定位。<br>
 * Google哪位攻城狮能够告诉我为什么不，用户就连个网络还要开定位。特么的逗。。。。
 * 另外6.0的WIFI切换也有权限问题：只能修改自己添加的网络，不能修改由系统或者其它APP添加的网络。
 * 意味着：6.0的连接网络基本无效，需要手动连接。
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/6/1
 */
@SuppressWarnings("unused")
public final class WifiMonitorImpl implements WifiMonitor {
    public static final String TAG = "WifiMonitor";

    /**
     * Android上下文
     */
    private final Context mContext;

    /**
     * Wifi管理器
     */
    private final WifiManager mWifiManager;

    /**
     * WifiManager类的影藏方法public void connect(WifiConfiguration config, ActionListener listener)
     */
    private Method mConnectMethod;

    /**
     * Connect方法的参数之一
     */
    private Class mActionListenerClass;

    WifiMonitorImpl(Context context) {
        mContext = context.getApplicationContext();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiStateReceiver, filter);
        initRefMethod();
    }

    /**
     * 初始化系统影藏的方法
     */
    private void initRefMethod() {
        Class<WifiManager> wifiManagerClass = WifiManager.class;
        Class[] classes = WifiManager.class.getClasses();
        for (Class cla : classes) {
            if (cla.getSimpleName().equals("ActionListener")) {
                mActionListenerClass = cla;
                break;
            }
        }
        try {
            mConnectMethod = wifiManagerClass.getMethod("connect", WifiConfiguration.class, mActionListenerClass);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    @Override
    public boolean enableWifi() {
        return mWifiManager.setWifiEnabled(true);
    }

    @Override
    public boolean disableWifi() {
        return mWifiManager.setWifiEnabled(false);
    }

    @Override
    public boolean isWifiConnected() {
        return (isWifiEnabled() && mWifiManager.getConnectionInfo() != null);
    }

    @Override
    public boolean startScanWifiAccessPoint() {
        return mWifiManager.isWifiEnabled() && mWifiManager.startScan();
    }

    @Override
    public List<ScanResult> getScanResultList() {
        return mWifiManager.getScanResults();
    }

    @Override
    public WifiInfo getConnectWifiInfo() {
        if (isWifiEnabled()) {
            return mWifiManager.getConnectionInfo();
        }
        return null;
    }

    @Override
    public boolean connectWifiAccessPoint(final String ssid, final SecurityType type, final Bundle extraData) {
        if (mWifiManager.isWifiEnabled()) {


            //连接扫描到的网络
            List<ScanResult> scanResultList = mWifiManager.getScanResults();
            for (ScanResult scanResult : scanResultList) {
                SecurityType resultSecurityType = parseCapability(scanResult.capabilities);
                if (scanResult.SSID.equals(ssid) && resultSecurityType == type) {
                    return connectWifiAccessPoint(scanResult, extraData);
                }
            }
            //连接已配置网络
            List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration cfg : wifiConfigurationList) {
                String cfgSSID = String.format("\"%s\"", ssid);
                if (cfg.SSID.equals(cfgSSID)) {
                    return connectWifiAccessPoint(ssid, type, cfg, null, extraData);
                }
            }
            //连接影藏网络
            return connectWifiAccessPoint(ssid, type, null, null, extraData);
        }
        return false;
    }


    @Override
    public boolean connectWifiAccessPoint(ScanResult result, Bundle extraData) {
        if (mWifiManager.isWifiEnabled()) {
            //连接已配置网络
            List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration cfg : wifiConfigurationList) {
                String cfgSSID = String.format("\"%s\"", result.SSID);
                if (cfg.SSID.equals(cfgSSID)) {
                    return connectWifiAccessPoint(result.SSID, parseCapability(result.capabilities), cfg, result, extraData);
                }
            }
            //连接扫描到的网络
            return connectWifiAccessPoint(result.SSID, parseCapability(result.capabilities), null, result, extraData);
        }
        return false;
    }

    /**
     * 连接WIFI热点
     *
     * @param ssid      热点名称
     * @param type      类型
     * @param existCfg  已存在的配置
     * @param result    扫描的结果
     * @param extraData 附带数据
     * @return true-成功
     */
    private boolean connectWifiAccessPoint(final String ssid, final SecurityType type, WifiConfiguration existCfg,
                                           ScanResult result, Bundle extraData) {
        WifiConfiguration config = new WifiConfiguration();
        if (existCfg == null && result == null) {
            config.SSID = ssid;
            config.hiddenSSID = true;
        } else if (result != null && existCfg == null) {
            config.SSID = result.SSID;
            config.BSSID = result.BSSID;
        } else {
            config.SSID = existCfg.SSID;
            config.networkId = existCfg.networkId;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && existCfg != null) {
            if (mWifiManager.enableNetwork(config.networkId, true)) {
                mWifiManager.reconnect();
                return true;
            }
        } else if (buildConfig(config, type, existCfg, extraData)) {
            mWifiManager.disconnect();
            if (mConnectMethod == null) {
                return connectWifiNormal(config, existCfg == null);
            } else {
                return connectWifiReflect(config);
            }
        }
        return false;
    }

    /**
     * 通过正常的API进行网络连接
     *
     * @param configuration 配置
     * @param add           是否是新增的网络。
     * @return true-成功
     */
    private boolean connectWifiNormal(WifiConfiguration configuration, boolean add) {
        if (add) {
            if (mWifiManager.addNetwork(configuration) == -1) {
                Log.i(TAG, "add network failed");
                return false;
            }
        } else {
            if (mWifiManager.updateNetwork(configuration) == -1) {
                Log.i(TAG, "updateNetwork failed");
                return false;
            }
        }
        mWifiManager.saveConfiguration();
        if (mWifiManager.enableNetwork(configuration.networkId, true)) {
            mWifiManager.reconnect();
            Log.i(TAG, "enableNetwork failed");
            return true;
        }
        return false;
    }

    /**
     * 通过反射调用系统的API进行网络连接。
     *
     * @param configuration 配置
     * @return true-成功
     */
    private boolean connectWifiReflect(WifiConfiguration configuration) {
        InvocationHandler invokeHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("onSuccess")) {
                    Log.i(TAG, "Connect success");
                } else if (method.getName().equals("onFailure")) {
                    Log.i(TAG, "Connect failure" + args[0]);
                }
                return null;
            }
        };

        try {
            Object listener = Proxy.newProxyInstance(mActionListenerClass.getClassLoader(), new Class[]{mActionListenerClass}, invokeHandler);
            mConnectMethod.invoke(mWifiManager, configuration, listener);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 解析WIFI加密类型
     * <p>字符串格式：[WPA-PSK-CCMP][WPA2-PSK-CCMP][ESS],
     *
     * @param capabilities 安全类型字符串
     * @return 安全类型
     */
    @Override
    public SecurityType parseCapability(String capabilities) {
        if (!TextUtils.isEmpty(capabilities)) {
            String ignoreCapabilities = capabilities.toUpperCase();
            if (ignoreCapabilities.contains("WPA") || ignoreCapabilities.contains("WPA2")) {
                if (ignoreCapabilities.contains("EAP")) {
                    return SecurityType.SECURITY_TYPE_EAP;
                } else {
                    return SecurityType.SECURITY_TYPE_PSK;
                }
            } else if (ignoreCapabilities.contains("WEP")) {
                return SecurityType.SECURITY_TYPE_WEP;
            } else {
                return SecurityType.SECURITY_TYPE_NONE;
            }
        }
        return null;
    }


    @Override
    public boolean disconnect() {
        return false;
    }

    /**
     * 构建一个完整的WifiConfiguration。
     *
     * @param config    WifiConfiguration
     * @param type      安全类型
     * @param existCfg  已存在的配置
     * @param extraData 与安全类型相关的密码数据等
     * @return true配置成功
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean buildConfig(WifiConfiguration config, SecurityType type, WifiConfiguration existCfg, Bundle extraData) {
        switch (type) {
            case SECURITY_TYPE_NONE: {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            }
            case SECURITY_TYPE_WEP: {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                String password = extraData.getString(DATA_KEY_PASSWORD);
                if (!TextUtils.isEmpty(password)) {
                    int length = password.length();
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;
            }
            case SECURITY_TYPE_PSK: {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                String password = extraData.getString(DATA_KEY_PASSWORD);
                if (!TextUtils.isEmpty(password)) {
                    config.preSharedKey = password;
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;
            }
            case SECURITY_TYPE_EAP: {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                if (existCfg == null) {
                    config.enterpriseConfig = new WifiEnterpriseConfig();
                    int eapMethod = extraData.getInt(DATA_KEY_EAP_METHOD, 0);
                    int phase2Method = extraData.getInt(DATA_KEY_PHASE_METHOD, 0);
                    config.enterpriseConfig.setEapMethod(eapMethod);
                    switch (eapMethod) {
                        case WifiEnterpriseConfig.Eap.PEAP:
                            switch (phase2Method) {
                                case WIFI_PEAP_PHASE2_NONE:
                                    config.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.NONE);
                                    break;
                                case WIFI_PEAP_PHASE2_MSCHAPV2:
                                    config.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
                                    break;
                                case WIFI_PEAP_PHASE2_GTC:
                                    config.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.GTC);
                                    break;
                                default:
                                    Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                    break;
                            }
                            break;
                        default:
                            config.enterpriseConfig.setPhase2Method(phase2Method);
                            break;
                    }
                } else {
                    config.enterpriseConfig = existCfg.enterpriseConfig;
                }
                //设置用户名
                if (config.enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.SIM || config.enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.AKA
                        || config.enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.AKA_PRIME) {
                    config.enterpriseConfig.setIdentity("");
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else if (config.enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.PWD) {
                    String identity = extraData.getString(DATA_KEY_IDENTITY, "");
                    config.enterpriseConfig.setIdentity(identity);
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    String identity = extraData.getString(DATA_KEY_IDENTITY, "");
                    String anonymousIdentity = extraData.getString(DATA_KEY_ANONYMOUS, "");
                    config.enterpriseConfig.setIdentity(identity);
                    config.enterpriseConfig.setAnonymousIdentity(anonymousIdentity);
                }

                //设置密码
                String password = extraData.getString(DATA_KEY_PASSWORD, "");
                if (!TextUtils.isEmpty(password)) {
                    config.enterpriseConfig.setPassword(password);
                }
                break;
            }
            default:
                return false;
        }
        return true;
    }

    /**
     * 接送WIFI状态变化，再WIFI开启时启动Wifi扫描
     */
    BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        mWifiManager.startScan();
                        break;
                    default:
                        break;
                }
            }
        }
    };
}
