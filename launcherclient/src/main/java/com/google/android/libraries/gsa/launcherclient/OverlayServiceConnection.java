package com.google.android.libraries.gsa.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.libraries.launcherclient.ILauncherOverlay;

import java.lang.ref.WeakReference;

class OverlayServiceConnection implements ServiceConnection {

    public static final String TAG = "LauncherClient.OverlayServiceConnection";

    private final Context mContext;

    private final int flags;

    private boolean isConnected;
    protected ILauncherOverlay mLauncherOverlay;

    private WeakReference<LauncherClient> weakReference;

    public OverlayServiceConnection(Context context) {
        this(context.getApplicationContext(), Context.BIND_AUTO_CREATE & Context.BIND_NOT_FOREGROUND);
    }

    OverlayServiceConnection(Context context, int flags) {
        this.mContext = context;
        this.flags = flags;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: " + componentName);
        mLauncherOverlay = ILauncherOverlay.Stub.asInterface(iBinder);
        Log.d(TAG, "onServiceConnected # mLauncherOverlay: " + mLauncherOverlay);
        setLauncherOverlay(mLauncherOverlay);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: " + componentName);
    }

    public final void unbindService() {
        Log.d(TAG, "unbindService: ");
        if (isConnected) {
            mContext.unbindService(this);
            isConnected = false;
        }
    }

    public final boolean isConnected() {
        return isConnected;
    }

    public final boolean reconnect() {
        if (!isConnected) {
            try {
                isConnected = mContext.bindService(LauncherClient.getIntent(mContext), this, flags);
            } catch (SecurityException e) {
                Log.e(TAG, "Unable to connect to overlay service", e);
            }
        }
        Log.d(TAG, "reconnect: " + isConnected);
        return isConnected;
    }

    public void registerLauncherClient(LauncherClient launcherClient) {
        this.weakReference = new WeakReference<>(launcherClient);
    }

    private boolean stopService;

    public void stopService(boolean stopService) {
        this.stopService = stopService;
        resetService();
    }

    public void unbindService(LauncherClient launcherClient, boolean unbindService) {
        LauncherClient e = getLauncherClient();
        if (e != null && e.equals(launcherClient)) {
            weakReference = null;
            if (unbindService) {
                unbindService();
            }
        }
    }

    private void resetService() {
        if (stopService && mLauncherOverlay == null) {
            unbindService();
        }
    }

    private void setLauncherOverlay(ILauncherOverlay overlay) {
        LauncherClient client = getLauncherClient();
        if (client != null) {
            client.setLauncherOverlay(overlay);
        }
    }

    private LauncherClient getLauncherClient() {
        if (weakReference != null) {
            return weakReference.get();
        }
        return null;
    }

}
