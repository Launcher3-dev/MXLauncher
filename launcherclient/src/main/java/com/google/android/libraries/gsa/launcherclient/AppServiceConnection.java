package com.google.android.libraries.gsa.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import com.google.android.libraries.launcherclient.ILauncherOverlay;

import java.lang.ref.WeakReference;

final class AppServiceConnection extends SimpleServiceConnection {

    private static AppServiceConnection serviceConnection;

    private WeakReference<LauncherClient> weakReference;

    private boolean stopService;

    static AppServiceConnection getInstance(Context context) {
        if (serviceConnection == null) {
            serviceConnection = new AppServiceConnection(context.getApplicationContext());
        }
        return serviceConnection;
    }

    private AppServiceConnection(Context context) {
        super(context, Context.BIND_AUTO_CREATE & Context.BIND_NOT_FOREGROUND);
    }

    public ILauncherOverlay registerLauncherClient(LauncherClient launcherClient) {
        this.weakReference = new WeakReference<>(launcherClient);
        return mLauncherOverlay;
    }

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
                if (serviceConnection == this) {
                    serviceConnection = null;
                }
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        super.onServiceConnected(componentName, iBinder);
        Log.d(TAG, "onServiceConnected: ");
        setLauncherOverlay(mLauncherOverlay);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        super.onServiceDisconnected(componentName);
        Log.d(TAG, "onServiceDisconnected: ");
        setLauncherOverlay(null);
        resetService();
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
