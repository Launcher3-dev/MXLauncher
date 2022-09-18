package com.google.android.libraries.gsa.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

class SimpleServiceConnection implements ServiceConnection {

    private final Context mContext;

    private final int flags;

    private boolean isConnected;

    SimpleServiceConnection(Context context, int flags) {
        this.mContext = context;
        this.flags = flags;
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
    }

    public void onServiceDisconnected(ComponentName componentName) {
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
        return isConnected;
    }
}
