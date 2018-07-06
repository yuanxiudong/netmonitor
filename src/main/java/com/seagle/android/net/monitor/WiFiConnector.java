package com.seagle.android.net.monitor;


import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * WiFi connector.
 * Created by seagle on 2018/4/23.
 *
 * @author yuanxiudong66@sina.com
 * @since 2018-4-23
 */
class WiFiConnector {
    private static final int SECURITY_NONE = 0;
    private static final int SECURITY_WEP = 1;
    private static final int SECURITY_PSK = 2;
    private static final int SECURITY_EAP = 3;

    private final WifiManager mWifiManager;
    private final ScanResult mScanResult;
    private final String mSSID;
    private final String mCapabilities;
    private final String mPassword;

    WiFiConnector(ScanResult scanResult, String password, WifiManager wifiManager) {
        mScanResult = scanResult;
        mSSID = scanResult.SSID;
        mCapabilities = scanResult.capabilities;
        mPassword = password;
        mWifiManager = wifiManager;
    }

    WiFiConnector(String SSID, String capabilities, String password, WifiManager wifiManager) {
        mScanResult = null;
        mSSID = SSID;
        mCapabilities = capabilities;
        mPassword = password;
        mWifiManager = wifiManager;
    }

    public String getSSID() {
        return mSSID;
    }

    public static String convertToQuotedString(String ssid) {
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid;
        }
        return "\"" + ssid + "\"";
    }

    private static int getSecurity(String capabilities) {
        if (capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public int connect() {
        WifiConfiguration wifiConfiguration = getExistedConfiguration(mSSID);
        if (wifiConfiguration == null) {
            wifiConfiguration = getConfig(null);
            int netID = mWifiManager.addNetwork(wifiConfiguration);
            if (netID > 0 && mWifiManager.enableNetwork(netID, true)) {
                mWifiManager.saveConfiguration();
                mWifiManager.reconnect();
                return netID;
            }
        } else {
            wifiConfiguration = getConfig(wifiConfiguration);
            mWifiManager.updateNetwork(wifiConfiguration);
            if (mWifiManager.enableNetwork(wifiConfiguration.networkId, true)) {
                mWifiManager.saveConfiguration();
                mWifiManager.reconnect();
                return wifiConfiguration.networkId;
            }
        }
        return -1;
    }

    private WifiConfiguration getExistedConfiguration(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        String ssid = convertToQuotedString(SSID);
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals(ssid)) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration getConfig(WifiConfiguration existConfig) {
        WifiConfiguration config = existConfig == null ? new WifiConfiguration() : existConfig;
        if (mScanResult == null) {
            config.SSID = convertToQuotedString(mSSID);
            config.hiddenSSID = true;
        } else {
            config.SSID = convertToQuotedString(mScanResult.SSID);
        }
        int security = getSecurity(mCapabilities);
        switch (security) {
            case SECURITY_NONE: {
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;
            }
            case SECURITY_WEP: {
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (mPassword.length() != 0) {
                    int length = mPassword.length();
                    String password = mPassword;
                    if ((length == 10 || length == 26 || length == 58)
                            && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;
            }
            case SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (mPassword.length() != 0) {
                    String password = mPassword;
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;
            default: {
                throw new RuntimeException("EAP network not support.");
            }
        }
        return config;
    }
}
