package com.seagle.android.net.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <h1>网络监听管理器</h1>
 * <p>主要目的是监听网络变化并提供网络改变接口</P>
 * 通过监听广播的方式能够准备的监听到WIFI切换，WIFI与移动网络之间的切换。但是：
 * 1.WIFI网络的优先级高于移动网络，所以如果WIFI打开，移动网络会关闭，切换移动网络无效。
 * 2.WIFI之间进行切换，会触发网络断开。然后才触发WIFI连接上。
 * 3.移动网络之间的切换操作通知不可信，有时候切3G到4G（或4G到3G）的切换可以触发，有时候却不行。而且响应速度很慢。
 *
 * @author : xiudong.yuan@midea.com.cn
 * @date : 2016/5/18
 */
@SuppressWarnings("unused")
public class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";

    /**
     * 未知网络类型
     */
    public static final int NETWORK_TYPE_NONE = -1;

    /**
     * 网络类型。
     * Monitor只监控移动网络，WIFI网络，有线网络三种类型。其它网络视为未知网络。
     */
    public enum NetType {
        TYPE_NONE, TYPE_MOBILE, TYPE_WIFI, TYPE_WIRE, TYPE_UNKNOWN
    }

    /**
     * Android 上下文
     */
    private Context mContext;

    /**
     * 网络状态事件监听器
     */
    private final Set<NetworkEventListener> mNetworkEventListenerSet;

    /**
     * WIFI状态事件监听器
     */
    private final Set<WifiNetworkEventListener> mWifiNetworkEventListenerSet;

    /**
     * 移动网络状态监听器
     */
    private final Set<MobileNetworkEventListener> mMobileNetworkEventListenerSet;

    /**
     * 有线网络状态监听器
     */
    private final Set<WireNetworkEventListener> mWireNetworkEventListenerSet;

    /**
     * 网络状态变化监听
     */
    private final ConnectionChangeReceiver mConnectionChangeReceiver;

    /**
     * 已启动监控
     */
    private volatile boolean mStarted;

    /**
     * 当前连接的网络
     */
    private volatile NetworkInfo mNetworkInfo;

    /**
     * 网路连接管理服务
     */
    private ConnectivityManager mConnectivityManager;

    /**
     * WIFI网络连接服务管理
     */
    private WifiManager mWifiManager;

    /**
     * WIFI管理器
     */
    private WifiMonitor mWifiMonitor;

    /**
     * 单例
     */
    private static NetworkMonitor sInstance;

    public synchronized static NetworkMonitor getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkMonitor();
        }
        return sInstance;
    }

    private NetworkMonitor() {
        mNetworkEventListenerSet = new CopyOnWriteArraySet<>();
        mWifiNetworkEventListenerSet = new CopyOnWriteArraySet<>();
        mMobileNetworkEventListenerSet = new CopyOnWriteArraySet<>();
        mWireNetworkEventListenerSet = new CopyOnWriteArraySet<>();
        mConnectionChangeReceiver = new ConnectionChangeReceiver();
    }

    public boolean registerNetworkEventListener(NetworkEventListener listener) {
        return mNetworkEventListenerSet.add(listener);
    }

    public boolean unRegisterNetworkEventListener(NetworkEventListener listener) {
        return mNetworkEventListenerSet.remove(listener);
    }

    public boolean registerWifiNetworkEventListener(WifiNetworkEventListener listener) {
        return mWifiNetworkEventListenerSet.add(listener);
    }

    public boolean unRegisterWifiNetworkEventListener(WifiNetworkEventListener listener) {
        return mWifiNetworkEventListenerSet.remove(listener);
    }

    public boolean registerMobileNetworkEventListener(MobileNetworkEventListener listener) {
        return mMobileNetworkEventListenerSet.add(listener);
    }

    public boolean unRegisterMobileNetworkEventListener(MobileNetworkEventListener listener) {
        return mMobileNetworkEventListenerSet.remove(listener);
    }

    public boolean registerWireNetworkEventListener(WireNetworkEventListener listener) {
        return mWireNetworkEventListenerSet.add(listener);
    }

    public boolean unRegisterWireNetworkEventListener(WireNetworkEventListener listener) {
        return mWireNetworkEventListenerSet.remove(listener);
    }

    /**
     * 启动网络检测服务
     *
     * @param context Android上下文
     * @return true-成功
     */
    public synchronized boolean startMonitoring(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context should not be null!");
        }
        if (!mStarted) {
            mStarted = true;
            mContext = context.getApplicationContext();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mConnectionChangeReceiver, filter);
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            mWifiMonitor = new WifiMonitorImpl(mContext);
        }
        return true;
    }

    /**
     * 停止监控服务。
     *
     * @return true-停止成功
     */
    public synchronized boolean stopMonitoring() {
        if (mStarted) {
            mStarted = false;
            mContext.unregisterReceiver(mConnectionChangeReceiver);
            mNetworkInfo = null;
            mWifiMonitor = null;
        }
        return true;
    }

    /**
     * 获取当前连接的网络。
     *
     * @return NetworkInfo
     */
    public synchronized NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    /**
     * 获取WIFI管理器.
     *
     * @return WifiMonitor
     */
    public synchronized WifiMonitor getWifiMonitor() {
        return mWifiMonitor;
    }

    /**
     * 获取当前连接网络的类型。
     *
     * @return NetType
     */
    public synchronized NetType getConnectedNetType() {
        if (mNetworkInfo == null) {
            return NetType.TYPE_NONE;
        }
        return parseNetType(mNetworkInfo);
    }


    /**
     * 解析网络类型
     *
     * @param networkInfo 网络信息
     * @return 网络类型
     */
    private NetType parseNetType(NetworkInfo networkInfo) {
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_MOBILE:
                return NetType.TYPE_MOBILE;
            case ConnectivityManager.TYPE_WIFI:
                return NetType.TYPE_WIFI;
            case ConnectivityManager.TYPE_ETHERNET:
                return NetType.TYPE_WIRE;
            case NETWORK_TYPE_NONE:
                return NetType.TYPE_NONE;
            default:
                return NetType.TYPE_UNKNOWN;
        }
    }

    private void notifyNetworkChanged(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            Log.i(TAG, "网络连接");
            for (NetworkEventListener listener : mNetworkEventListenerSet) {
                listener.onNetworkConnected(networkInfo);
            }
        } else {
            Log.i(TAG, "网络连接");
            for (NetworkEventListener listener : mNetworkEventListenerSet) {
                listener.onNetworkDisconnected(networkInfo);
            }
        }
    }

    private void notifyWifiState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            Log.i(TAG, "WIF连接" + networkInfo);
            for (WifiNetworkEventListener listener : mWifiNetworkEventListenerSet) {
                listener.onWifiConnected(mWifiManager.getConnectionInfo());
            }
        } else {
            Log.i(TAG, "WIF断开" + networkInfo);
            for (WifiNetworkEventListener listener : mWifiNetworkEventListenerSet) {
                listener.onWifiDisconnected(mWifiManager.getConnectionInfo());
            }
        }
    }

    private void notifyMobileState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            Log.i(TAG, "移动网络连接" + networkInfo);
            for (MobileNetworkEventListener listener : mMobileNetworkEventListenerSet) {
                listener.onNetworkConnected(networkInfo);
            }
        } else {
            Log.i(TAG, "移动网络断开" + networkInfo);
            for (MobileNetworkEventListener listener : mMobileNetworkEventListenerSet) {
                listener.onNetworkDisconnected(networkInfo);
            }
        }
    }

    private void notifyWireState(boolean connected, NetworkInfo networkInfo) {
        if (connected) {
            Log.i(TAG, "有线网络连接" + networkInfo);
            for (WireNetworkEventListener listener : mWireNetworkEventListenerSet) {
                listener.onNetworkConnected(networkInfo);
            }
        } else {
            Log.i(TAG, "有线网络连接" + networkInfo);
            for (WireNetworkEventListener listener : mWireNetworkEventListenerSet) {
                listener.onNetworkDisconnected(networkInfo);
            }
        }
    }


    /**
     * 监听网络状态
     */
    class ConnectionChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.endsWith(intent.getAction())) {
                NetworkInfo network = mConnectivityManager.getActiveNetworkInfo();
                if (network == null) {
                    if (mNetworkInfo != null) {
                        NetType netType = parseNetType(mNetworkInfo);
                        switch (netType) {
                            case TYPE_WIFI:
                                notifyWifiState(false, mNetworkInfo);
                                break;
                            case TYPE_MOBILE:
                                notifyMobileState(false, mNetworkInfo);
                                break;
                            case TYPE_WIRE:
                                notifyWireState(false, mNetworkInfo);
                                break;
                            default:
                                break;
                        }
                    }
                    mNetworkInfo = null;
                } else if (parseNetType(network) == NetType.TYPE_UNKNOWN) {
                    Log.i(TAG, "其它网络网络");
                } else {
                    synchronized (this) {
                        if (NetworkInfo.State.CONNECTED == network.getState()) {
                            if (mNetworkInfo == null) {
                                mNetworkInfo = network;
                                notifyNetworkChanged(true, network);
                                NetType netType = parseNetType(network);
                                switch (netType) {
                                    case TYPE_WIFI:
                                        notifyWifiState(true, network);
                                        break;
                                    case TYPE_MOBILE:
                                        notifyMobileState(true, network);
                                        break;
                                    case TYPE_WIRE:
                                        notifyWireState(true, network);
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                if (mNetworkInfo.getType() != network.getType() || mNetworkInfo.getSubtype() != network.getSubtype()) {
                                    NetworkInfo preNetwork = mNetworkInfo;
                                    mNetworkInfo = network;
                                    for (NetworkEventListener listener : mNetworkEventListenerSet) {
                                        listener.onNetworkChanged(preNetwork, network);
                                    }
                                    NetType preNetType = parseNetType(preNetwork);
                                    switch (preNetType) {
                                        case TYPE_WIFI:
                                            notifyWifiState(false, network);
                                            break;
                                        case TYPE_MOBILE:
                                            notifyMobileState(false, network);
                                            break;
                                        case TYPE_WIRE:
                                            notifyWireState(false, network);
                                            break;
                                        default:
                                            break;
                                    }

                                    NetType netType = parseNetType(mNetworkInfo);
                                    switch (netType) {
                                        case TYPE_WIFI:
                                            notifyWifiState(true, network);
                                            break;
                                        case TYPE_MOBILE:
                                            notifyMobileState(true, network);
                                            break;
                                        case TYPE_WIRE:
                                            notifyWireState(true, network);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                mNetworkInfo = network;
                            }

                        } else if (NetworkInfo.State.DISCONNECTED == network.getState()) {
                            if (mNetworkInfo != null) {
                                notifyNetworkChanged(false, mNetworkInfo);
                                mNetworkInfo = null;
                                NetType netType = parseNetType(network);
                                switch (netType) {
                                    case TYPE_WIFI: {
                                        notifyWifiState(false, network);
                                        break;
                                    }
                                    case TYPE_MOBILE: {
                                        notifyMobileState(false, network);
                                        break;
                                    }
                                    case TYPE_WIRE: {
                                        notifyWireState(false, network);
                                        break;
                                    }
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
