package com.google.android.libraries.gsa.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.libraries.launcherclient.ILauncherOverlay;

class SimpleServiceConnection implements ServiceConnection {

    public static final String TAG = "LauncherClient.SimpleServiceConnection";

    private final Context mContext;

    private final int flags;

    private boolean isConnected;
    protected ILauncherOverlay mLauncherOverlay;

    SimpleServiceConnection(Context context, int flags) {
        this.mContext = context;
        this.flags = flags;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: " + componentName);
        mLauncherOverlay = ILauncherOverlay.Stub.asInterface(iBinder);
        Log.d(TAG, "onServiceConnected # mLauncherOverlay: " + mLauncherOverlay);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: " + componentName);
    }

    public final void unbindService() {
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
                Log.e("LauncherClient", "Unable to connect to overlay service", e);
            }
        }
        Log.d(TAG, "reconnect: " + isConnected);
        return isConnected;
    }
}
