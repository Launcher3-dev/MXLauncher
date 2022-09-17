package com.google.android.libraries.gsa.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;

import com.google.android.libraries.launcherclient.ILauncherOverlayStub;
import com.google.android.libraries.launcherclient.ILauncherOverlay;

import java.lang.ref.WeakReference;

final class AppServiceConnection extends SimpleServiceConnection {

    private static AppServiceConnection serviceConnection;

    private ILauncherOverlay launcherOverlay;

    private WeakReference<LauncherClient> weakReference;

    private boolean stopService;

    static AppServiceConnection getInstance(Context context) {
        if (serviceConnection == null) {
            serviceConnection = new AppServiceConnection(context.getApplicationContext());
        }
        return serviceConnection;
    }

    private AppServiceConnection(Context context) {
        super(context, 33);
    }

    public ILauncherOverlay registerLauncherClient(LauncherClient launcherClient) {
        this.weakReference = new WeakReference<>(launcherClient);
        return this.launcherOverlay;
    }

    public void stopService(boolean stopService) {
        this.stopService = stopService;
        resetService();
    }

    public void unbindService(LauncherClient launcherClient, boolean unbindService) {
        LauncherClient e = getLauncherClient();
        if (e != null && e.equals(launcherClient)) {
            this.weakReference = null;
            if (unbindService) {
                unbindService();
                if (serviceConnection == this) {
                    serviceConnection = null;
                }
            }
        }
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        setLauncherOverlay(ILauncherOverlayStub.asInterface(iBinder));
    }

    public void onServiceDisconnected(ComponentName componentName) {
        setLauncherOverlay(null);
        resetService();
    }

    private void resetService() {
        if (this.stopService && this.launcherOverlay == null) {
            unbindService();
        }
    }

    private void setLauncherOverlay(ILauncherOverlay overlay) {
        launcherOverlay = overlay;
        LauncherClient client = getLauncherClient();
        if (client != null) {
            client.setLauncherOverlay(launcherOverlay);
        }
    }

    private LauncherClient getLauncherClient() {
        if (weakReference != null) {
            return weakReference.get();
        }
        return null;
    }
}
