package com.google.android.libraries.launcherclient;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.WindowManager;

import com.google.android.launcherclient.BaseProxy;
import com.google.android.launcherclient.Codecs;

public final class LauncherOverlayProxy extends BaseProxy implements ILauncherOverlay {
    LauncherOverlayProxy(IBinder iBinder) {
        super(iBinder, "com.google.android.libraries.launcherclient.ILauncherOverlay");
    }

    public void startScroll() throws RemoteException {
        transact(1, obtain());
    }

    public void onScroll(float progress) throws RemoteException {
        Parcel a = obtain();
        a.writeFloat(progress);
        transact(2, a);
    }

    public void endScroll() throws RemoteException {
        transact(3, obtain());
    }

    public void windowAttached(WindowManager.LayoutParams layoutParams, ILauncherOverlayCallback dVar, int i) throws RemoteException {
        Parcel a = obtain();
        Codecs.writeParcelable(a, layoutParams);
        Codecs.writeInterfaceToken(a, dVar);
        a.writeInt(i);
        transact(4, a);
    }

    public void windowAttached2(Bundle bundle, ILauncherOverlayCallback dVar) throws RemoteException {
        Parcel a = obtain();
        Codecs.writeParcelable(a, bundle);
        Codecs.writeInterfaceToken(a, dVar);
        transact(14, a);
    }

    public void windowDetached(boolean changingConfigurations) throws RemoteException {
        Parcel a = obtain();
        Codecs.writeChangingConfiguration(a, changingConfigurations);
        transact(5, a);
    }

    public void closeOverlay(int i) throws RemoteException {
        Parcel a = obtain();
        a.writeInt(i);
        transact(6, a);
    }

    public void mo17c() throws RemoteException {
        transact(7, obtain());
    }

    public void mo19d() throws RemoteException {
        transact(8, obtain());
    }

    public void mo15b(int i) throws RemoteException {
        Parcel a = obtain();
        a.writeInt(i);
        transact(16, a);
    }

    public void openOverlay(int i) throws RemoteException {
        Parcel a = obtain();
        a.writeInt(i);
        transact(9, a);
    }

    public void requestVoiceDetection(boolean start) throws RemoteException {
        Parcel a = obtain();
        Codecs.writeChangingConfiguration(a, start);
        transact(10, a);
    }

    public boolean isVoiceDetectionRunning() throws RemoteException {
        Parcel a = obtain(12, obtain());
        boolean a2 = Codecs.check(a);
        a.recycle();
        return a2;
    }

    public boolean mo21f() throws RemoteException {
        Parcel a = obtain(13, obtain());
        boolean a2 = Codecs.check(a);
        a.recycle();
        return a2;
    }
}