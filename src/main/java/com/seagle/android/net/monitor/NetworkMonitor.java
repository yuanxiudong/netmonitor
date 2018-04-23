package com.seagle.android.net.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Android system network monitor.
 *
 * @author : yuanxiudong66@sina.com
 * @since : 2016/5/18
 */
public class NetworkMonitor {

    private static final String TAG = "SENetworkMonitor";
    /**
     * Network connected.
     */
    public static final String ACTION_NET_CONNECTED = "com.seagle.android.net.monitor.ACTION_NET_CONNECTED";
    /**
     * Network disconnected.
     */
    public static final String ACTION_NET_DISCONNECTED = "com.seagle.android.net.monitor.ACTION_NET_DISCONNECTED";
    /**
     * Network changed.
     */
    public static final String ACTION_NET_CHANGED = "com.seagle.android.net.monitor.ACTION_NET_CHANGED";
    /**
     * Network info extras.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    /**
     * The previous Network info extras.
     */
    public static final String EXTRA_PRE_NETWORK_INFO = "networkInfo";
    private SoftReference<Context> mContext;
    private volatile boolean mStarted;
    private volatile NetworkInfo mActiveNetworkInfo;
    private ConnectivityManager mConnectivityManager;
    private final ConnectionChangeReceiver mConnectionChangeReceiver;
    private WifiNetworkMonitor mWifiStateMachine;
    private MobileNetworkMonitor mMobileStateMachine;
    private EthernetNetworkMonitor mEthernetStateMachine;

    private static NetworkMonitor sInstance;

    public synchronized static NetworkMonitor getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkMonitor();
        }
        return sInstance;
    }

    private NetworkMonitor() {
        mConnectionChangeReceiver = new ConnectionChangeReceiver();

    }

    /**
     * Start monitor network state.
     *
     * @param context Android Context
     */
    public synchronized void startMonitoring(Context context) {
        if (!mStarted) {
            if (context == null) {
                throw new IllegalArgumentException("Context should not be null!");
            }

            mWifiStateMachine = new WifiNetworkMonitor(context);
            mMobileStateMachine = new MobileNetworkMonitor(context);
            mEthernetStateMachine = new EthernetNetworkMonitor(context);

            mStarted = true;
            mContext = new SoftReference<>(context.getApplicationContext());
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(mConnectionChangeReceiver, filter);
            mConnectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (mConnectivityManager == null) {
                throw new NullPointerException("Get system connectivity service failed!");
            }
            mActiveNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mActiveNetworkInfo == null || mActiveNetworkInfo.getState() != NetworkInfo.State.CONNECTED) {
                Intent broadCastIntent = new Intent(ACTION_NET_DISCONNECTED);
                broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mActiveNetworkInfo);
                context.sendStickyBroadcast(broadCastIntent);
            } else {
                Intent broadCastIntent = new Intent(ACTION_NET_CONNECTED);
                broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mActiveNetworkInfo);
                context.sendStickyBroadcast(broadCastIntent);
            }
            initNetwork();
            mWifiStateMachine.start();
            mMobileStateMachine.start();
            mEthernetStateMachine.start();
        }
    }

    private void initNetwork() {
        List<NetworkInfo> networkInfoList = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = mConnectivityManager.getAllNetworks();
            for (Network network : networks) {
                NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                if (networkInfo != null) {
                    networkInfoList.add(networkInfo);
                }
            }
        } else {
            NetworkInfo[] networkInfoArray = mConnectivityManager.getAllNetworkInfo();
            if (networkInfoArray != null && networkInfoArray.length > 0) {
                networkInfoList.addAll(Arrays.asList(networkInfoArray));
            }
        }
        for (NetworkInfo networkInfo : networkInfoList) {
            if (ConnectivityManager.TYPE_WIFI == networkInfo.getType()) {
                mWifiStateMachine.setNetworkInfo(networkInfo);
            } else if (ConnectivityManager.TYPE_MOBILE == networkInfo.getType()) {
                mMobileStateMachine.setNetworkInfo(networkInfo);
            } else if (ConnectivityManager.TYPE_ETHERNET == networkInfo.getType()) {
                mEthernetStateMachine.setNetworkInfo(networkInfo);
            }
        }
    }

    /**
     * Stop monitor network.
     */
    public synchronized void stopMonitoring() {
        if (mStarted) {
            mStarted = false;
            if (mContext != null) {
                Context context = mContext.get();
                if (context != null) {
                    context.unregisterReceiver(mConnectionChangeReceiver);
                }
            }
            mWifiStateMachine.stop();
            mMobileStateMachine.stop();
            mEthernetStateMachine.stop();
            mActiveNetworkInfo = null;
        }
    }

    /**
     * The system connection change broadcast receiver.
     */
    class ConnectionChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null) {
                if (mActiveNetworkInfo != null) {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mWifiStateMachine.notifyWifiState(false, null);
                        mMobileStateMachine.notifyWifiState(false, null);
                        mEthernetStateMachine.notifyWifiState(false, null);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_DISCONNECTED);
                    broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mActiveNetworkInfo);
                    context.sendBroadcast(broadCastIntent);
                    mActiveNetworkInfo = null;
                    Log.i(TAG, "Network disconnected!");
                }
            } else if (mActiveNetworkInfo == null) {
                Log.i(TAG, "Network connected: " + activeNetworkInfo);
                if (ConnectivityManager.TYPE_MOBILE == activeNetworkInfo.getType()) {
                    mActiveNetworkInfo = activeNetworkInfo;
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mMobileStateMachine.notifyWifiState(true, mActiveNetworkInfo);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_CONNECTED);
                    broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mActiveNetworkInfo);
                    context.sendStickyBroadcast(broadCastIntent);
                } else if (ConnectivityManager.TYPE_WIFI == activeNetworkInfo.getType()) {
                    mActiveNetworkInfo = activeNetworkInfo;
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mWifiStateMachine.notifyWifiState(true, mActiveNetworkInfo);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_CONNECTED);
                    broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mActiveNetworkInfo);
                    context.sendStickyBroadcast(broadCastIntent);
                } else if (ConnectivityManager.TYPE_ETHERNET == activeNetworkInfo.getType()) {
                    mActiveNetworkInfo = activeNetworkInfo;
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mEthernetStateMachine.notifyWifiState(true, mActiveNetworkInfo);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_CONNECTED);
                    broadCastIntent.putExtra(EXTRA_NETWORK_INFO, mActiveNetworkInfo);
                    context.sendStickyBroadcast(broadCastIntent);
                } else {
                    Log.i(TAG, "Other Network connected!");
                    mActiveNetworkInfo = null;
                }
            } else if (mActiveNetworkInfo.getType() != activeNetworkInfo.getType()) {
                if (ConnectivityManager.TYPE_MOBILE == activeNetworkInfo.getType()) {
                    Log.i(TAG, "Network change to mobile: " + activeNetworkInfo);
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mMobileStateMachine.notifyWifiState(true, activeNetworkInfo);
                        mWifiStateMachine.notifyWifiState(false, null);
                        mEthernetStateMachine.notifyWifiState(false, null);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_CHANGED);
                    broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mActiveNetworkInfo);
                    broadCastIntent.putExtra(EXTRA_NETWORK_INFO, activeNetworkInfo);
                    mActiveNetworkInfo = activeNetworkInfo;
                    context.sendStickyBroadcast(broadCastIntent);
                } else if (ConnectivityManager.TYPE_WIFI == activeNetworkInfo.getType()) {
                    Log.i(TAG, "Network change to wifi: " + activeNetworkInfo);
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mWifiStateMachine.notifyWifiState(true, activeNetworkInfo);
                        mMobileStateMachine.notifyWifiState(false, null);
                        mEthernetStateMachine.notifyWifiState(false, null);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_CHANGED);
                    broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mActiveNetworkInfo);
                    broadCastIntent.putExtra(EXTRA_NETWORK_INFO, activeNetworkInfo);
                    mActiveNetworkInfo = activeNetworkInfo;
                    context.sendStickyBroadcast(broadCastIntent);
                } else if (ConnectivityManager.TYPE_ETHERNET == activeNetworkInfo.getType()) {
                    Log.i(TAG, " Network change to ethernet: " + activeNetworkInfo);
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mEthernetStateMachine.notifyWifiState(true, activeNetworkInfo);
                        mWifiStateMachine.notifyWifiState(false, null);
                        mMobileStateMachine.notifyWifiState(false, null);
                    }
                    Intent broadCastIntent = new Intent(ACTION_NET_CHANGED);
                    broadCastIntent.putExtra(EXTRA_PRE_NETWORK_INFO, mActiveNetworkInfo);
                    broadCastIntent.putExtra(EXTRA_NETWORK_INFO, activeNetworkInfo);
                    mActiveNetworkInfo = activeNetworkInfo;
                    context.sendStickyBroadcast(broadCastIntent);
                } else {
                    Log.i(TAG, "Other Network connected!");
                    mActiveNetworkInfo = null;
                }

            }
        }
    }

    /**
     * Net state machine
     */
    static abstract class NetStateMachine {
        volatile NetworkInfo mNetworkInfo;
        private ConnectivityManager.NetworkCallback mNetCallback;
        ConnectivityManager mConnectivityManager;
        Context mContext;

        NetStateMachine(Context context) {
            mContext = context;
            mConnectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        void setNetworkInfo(NetworkInfo networkInfo) {
            mNetworkInfo = networkInfo;
        }

        void start() {
            if (mNetworkInfo == null || mNetworkInfo.getState() != NetworkInfo.State.CONNECTED) {
                notifyWifiState(false, null);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mNetCallback = new ConnectivityManager.NetworkCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onAvailable(Network network) {
                        if (network != null) {
                            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                            notifyWifiState(true, networkInfo);
                        }
                    }

                    @Override
                    public void onLost(Network network) {
                        notifyWifiState(false, null);
                    }
                };
                mConnectivityManager.registerNetworkCallback(getNetRequest(), mNetCallback);
            }
        }

        void stop() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mConnectivityManager.unregisterNetworkCallback(mNetCallback);
            }
        }

        protected abstract NetworkRequest getNetRequest();

        protected abstract void notifyWifiState(boolean connected, NetworkInfo networkInfo);
    }
}
