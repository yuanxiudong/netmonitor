package com.seagle.android.net.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_NETWORK_INFO;
import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_NETWORK_STATE;
import static com.seagle.android.net.monitor.NetworkMonitor.EXTRA_PRE_NETWORK_INFO;

/**
 * WiFi monitor.
 * Created by seagle on 2018/4/23.
 *
 * @author : yuanxiudong66@sina.com
 * @since : 2018-4-23
 */
public class WiFiNetworkMonitor extends NetStateMachine {

    private static final String TAG = "WiFiNetworkMonitor";

    /**
     * WiFi network connect state changed.
     * Get WiFi connect state by broadcast intent#getBooleanExtra({@link NetworkMonitor#EXTRA_NETWORK_STATE},false).<br/>
     * If current WiFi network is connected,app can get current network info by intent#getParcelableExtra({@link NetworkMonitor#EXTRA_NETWORK_STATE}),
     * and get wifi info by intent#getParcelableExtra({@link #EXTRA_WIFI_INFO}).<br/>
     * if current WiFi network is disconnected and previous state is connected,
     * app can also get the previous network info by intent#getParcelableExtra({@link NetworkMonitor#EXTRA_PRE_NETWORK_INFO})
     * and will return null if previous state is disconnected.<br/>
     *
     * @see NetworkMonitor#EXTRA_NETWORK_STATE
     * @see NetworkMonitor#EXTRA_NETWORK_INFO
     * @see NetworkMonitor#EXTRA_PRE_NETWORK_INFO
     * @see #EXTRA_WIFI_INFO
     */
    public static final String ACTION_WIFI_STATE_CHANGED = "com.seagle.android.net.monitor.ACTION_WIFI_STATE_CONNECTED";

    /**
     * WiFi info extras key.
     */
    public static final String EXTRA_WIFI_INFO = "wifiInfo";

    /**
     * Connect WiFi success.
     */
    public static final int CONNECT_SUCCESS = 0;

    /**
     * Connect WiFi error code: other err.
     */
    public static final int ERR_CONNECT_FAILED = -1;

    /**
     * Connect WiFi error code: timeout.
     */
    public static final int ERR_CONNECT_TIMEOUT = -2;

    /**
     * Connect WiFi error code: password wrong.
     */
    public static final int ERR_PASSWORD_WRONG = -3;

    /**
     * Capabilities:WEP.
     * For WEP or OPEN access point.
     */
    private static final String SECURITY_WEP = "WEP";

    /**
     * Capabilities:PSK.
     * For WAP-PSK or WAP2-PSK access point.
     */
    private static final String SECURITY_PSK = "PSK";

    private volatile WifiInfo mWifiInfo;
    private WifiManager mWifiManager;

    WiFiNetworkMonitor(Context context) {
        super(context);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Return current WiFi info.
     * Return null if WiFi not connected.
     *
     * @return WifiInfo
     */
    public WifiInfo getWiFiInfo() {
        return mWifiInfo;
    }

    /**
     * Enable WiFi.
     *
     * @return operation result
     */
    public boolean enableWiFi() {
        return mWifiManager.setWifiEnabled(true);
    }

    /**
     * Disable WiFi.
     *
     * @return operation result
     */
    public boolean disableWiFi() {
        return mWifiManager.setWifiEnabled(false);
    }

    /**
     * Return WiFi enable state.
     *
     * @return WiFi enable state
     */
    public boolean isWiFiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * Connect WiFi.
     * Support sync and async call method: <br>
     * If callback is null,will block the call thread and return result by synchronous.<br>
     * If callback not null,this method return -1 immediately and the connection result return by callback.
     * <p>
     * Attention:not support connect new EAP network.
     *
     * @param result   WiFi ScanResult
     * @param password WiFi password if needed
     * @param callback Connect result callback
     * @return Connect result
     */
    public int connectWiFi(ScanResult result, String password, final WiFiConnectCallback callback) {
        final WiFiConnector connector = new WiFiConnector(result, password, mWifiManager);
        final ConnectWiFiTask task = new ConnectWiFiTask(connector);
        if (callback == null) {
            return task.call();
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int result = task.call();
                    if (result == CONNECT_SUCCESS) {
                        callback.onConnectSuccess();
                    } else {
                        String message = "Connect WiFi " + connector.getSSID() + " failed as unknown err!";
                        if (ERR_CONNECT_TIMEOUT == result) {
                            message = "Connect WiFi " + connector.getSSID() + " timeout!";
                        } else if (ERR_PASSWORD_WRONG == result) {
                            message = "Connect WiFi " + connector.getSSID() + " failed as password wrong!";
                        }
                        callback.onConnectFailed(result, message);
                    }
                }
            });
            thread.start();
            return -1;
        }
    }

    /**
     * Connect WiFi.
     * Support sync and async call method: <br>
     * If callback is null,will block the call thread and return result by synchronous.<br>
     * If callback not null,this method return -1 immediately and the connection result return by callback.
     * <p>
     * Attention:not support connect new EAP network.
     *
     * @param ssid         WiFi SSID
     * @param capabilities WiFi capabilities
     * @param password     WiFi password if needed
     * @param callback     Connect result callback
     * @return Connect result
     */
    public int connectWiFi(String ssid, String capabilities, String password, final WiFiConnectCallback callback) {
        final WiFiConnector connector = new WiFiConnector(ssid, capabilities, password, mWifiManager);
        final ConnectWiFiTask task = new ConnectWiFiTask(connector);
        if (callback == null) {
            return task.call();
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int result = task.call();
                    if (result == 0) {
                        callback.onConnectSuccess();
                    } else {
                        String message = "Connect WiFi " + connector.getSSID() + " failed as unknown err!";
                        if (ERR_CONNECT_TIMEOUT == result) {
                            message = "Connect WiFi " + connector.getSSID() + " timeout!";
                        } else if (ERR_PASSWORD_WRONG == result) {
                            message = "Connect WiFi " + connector.getSSID() + " failed as password wrong!";
                        }
                        callback.onConnectFailed(result, message);
                    }
                }
            });
            thread.start();
            return -1;
        }
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    @Override
    protected void notifyNetworkState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            mNetworkInfo = networkInfo;
            mWifiInfo = mWifiManager.getConnectionInfo();
            Intent broadCastIntent = new Intent(ACTION_WIFI_STATE_CHANGED);
            broadCastIntent.putExtra(EXTRA_NETWORK_STATE, true);
            broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mNetworkInfo);
            broadCastIntent.putExtra(EXTRA_WIFI_INFO, mWifiInfo);
            mContext.sendStickyBroadcast(broadCastIntent);
            Log.i(TAG, "WiFi network connected: " + mNetworkInfo);
        } else {
            Log.i(TAG, "WiFi network disconnected: " + mNetworkInfo);
            Intent broadCastIntent = new Intent(ACTION_WIFI_STATE_CHANGED);
            broadCastIntent.putExtra(EXTRA_NETWORK_STATE, false);
            if (mNetworkInfo != null) {
                broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mNetworkInfo);
                broadCastIntent.putExtra(EXTRA_WIFI_INFO, mWifiInfo);
            }
            mContext.sendStickyBroadcast(broadCastIntent);
            mNetworkInfo = null;
            mNetwork = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected NetworkRequest getNetRequest() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        return builder.build();
    }

    @Override
    void stop() {
        if (mContext != null) {
            Intent broadCastIntent = new Intent(ACTION_WIFI_STATE_CHANGED);
            mContext.removeStickyBroadcast(broadCastIntent);
        }
        super.stop();
    }

    private class ConnectWiFiTask extends BroadcastReceiver implements Callable<Integer> {
        private final WiFiConnector mWiFiConnector;
        private volatile int mNetID = -2;
        private volatile boolean mConnecting;
        private final CountDownLatch mLatch = new CountDownLatch(1);
        private volatile int mResultCode;

        public ConnectWiFiTask(WiFiConnector wiFiConnector) {
            mWiFiConnector = wiFiConnector;
        }

        @Override
        public Integer call() {
            try {
                if ((mNetID = mWiFiConnector.connect()) > 0) {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                    mContext.registerReceiver(this, filter);
                    mConnecting = true;
                    if (!mLatch.await(30, TimeUnit.SECONDS)) {
                        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                        String ssid = WiFiConnector.convertToQuotedString(mWiFiConnector.getSSID());
                        if (wifiInfo != null && ssid.equalsIgnoreCase(wifiInfo.getSSID())) {
                            mResultCode = CONNECT_SUCCESS;
                        } else {
                            mResultCode = ERR_CONNECT_TIMEOUT;
                        }
                    }
                }
            } catch (Exception ex) {
                mResultCode = ERR_CONNECT_FAILED;
            } finally {
                mNetID = -2;
                mConnecting = false;
                mContext.unregisterReceiver(this);
            }
            return mResultCode;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mNetID > 0 && mConnecting) {
                String action = intent.getAction();
                if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equalsIgnoreCase(action) && mNetID != -2) {
                    SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                    Log.i(TAG, "State Changed: " + state);
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    String ssid = WiFiConnector.convertToQuotedString(mWiFiConnector.getSSID());
                    if (wifiInfo != null && ssid.equalsIgnoreCase(wifiInfo.getSSID())) {
                        SupplicantState wifiState = wifiInfo.getSupplicantState();
                        if (SupplicantState.COMPLETED == state && SupplicantState.COMPLETED == wifiState) {
                            mResultCode = CONNECT_SUCCESS;
                            mLatch.countDown();
                        } else if (SupplicantState.DISCONNECTED == state && SupplicantState.DISCONNECTED == wifiState) {
                            int resultCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                            if (resultCode == 1) {
                                mResultCode = ERR_PASSWORD_WRONG;
                            } else {
                                mResultCode = ERR_CONNECT_FAILED;
                            }
                            mLatch.countDown();
                            mWifiManager.removeNetwork(mNetID);
                        }
                    }
                }
            }
        }
    }

    /**
     * WiFi connect callback.
     */
    public interface WiFiConnectCallback {
        /**
         * Connect success.
         */
        void onConnectSuccess();

        /**
         * Connect failed.
         *
         * @param errorCode    errorCode
         * @param errorMessage errorMessage
         * @see #ERR_CONNECT_FAILED
         * @see #ERR_CONNECT_TIMEOUT
         * @see #ERR_PASSWORD_WRONG
         */
        void onConnectFailed(int errorCode, String errorMessage);
    }
}
