package com.google.android.libraries.gsa.launcherclient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.launcherclient.Constant;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallbackSub;
import com.google.android.libraries.launcherclient.ILauncherOverlay;

import java.io.PrintWriter;

public class LauncherClient {

    private static int sServiceVersion = -1;
    public final Activity mActivity;
    public final LauncherClientCallbacks mLauncherClientCallbacks;
    private final EventLogArray mClientLog;
    public final EventLogArray mServiceLog;
    public final SimpleServiceConnection mSimpleServiceConnection;
    public final AppServiceConnection sApplicationConnection;
    private final BroadcastReceiver mUpdateReceiver;
    private ILauncherOverlay mOverlay;
    public int mState;
    private boolean mDestroyed;
    public int mServiceStatus;
    private int mServiceConnectionOptions;
    private WindowManager.LayoutParams mWindowAttrs;
    private OverlayCallbacks mCurrentCallbacks;

    public static class ClientOptions {
        public final int f19a;

        public ClientOptions(boolean enableMinusOne, boolean enableHotword, boolean enablePrewarming) {
            int i;
            int i2;
            int i3 = 0;
            if (enableMinusOne) {
                i = 1;
            } else {
                i = 0;
            }
            int i4 = i | 0;
            if (enableHotword) {
                i2 = 2;
            } else {
                i2 = 0;
            }
            f19a = (enablePrewarming ? 4 : i3) | i2 | i4;
        }
    }

    public LauncherClient(Activity activity) {
        this(activity, new LauncherClientCallbacksAdapter());
    }

    public LauncherClient(Activity activity, LauncherClientCallbacks launcherClientCallbacks) {
        this(activity, launcherClientCallbacks, new ClientOptions(true, true, true));
    }

    private static class OverlayCallbacks extends ILauncherOverlayCallbackSub implements Handler.Callback {

        private final Handler mUIHandler = new Handler(Looper.getMainLooper(), this);

        private LauncherClient mClient;

        private WindowManager mWindowManager;

        private int mWindowShift;

        private Window mWindow;

        private boolean mWindowHidden = false;

        OverlayCallbacks() {
        }

        public final void setClient(LauncherClient launcherClient) {
            mClient = launcherClient;
            mWindowManager = launcherClient.mActivity.getWindowManager();
            Point point = new Point();
            mWindowManager.getDefaultDisplay().getRealSize(point);
            mWindowShift = -Math.max(point.x, point.y);
            mWindow = launcherClient.mActivity.getWindow();
        }

        public final void clear() {
            mClient = null;
            mWindowManager = null;
            mWindow = null;
        }

        public final void overlayScrollChanged(float f) throws RemoteException {
            mUIHandler.removeMessages(2);
            Message.obtain(mUIHandler, 2, f).sendToTarget();
            if (f > 0.0f && mWindowHidden) {
                mWindowHidden = false;
            }
        }

        public final void overlayStatusChanged(int i) {
            Message.obtain(mUIHandler, 4, i, 0).sendToTarget();
        }

        public final boolean handleMessage(Message message) {
            if (mClient == null) {
                return true;
            }
            switch (message.what) {
                case 2:
                    if ((mClient.mServiceStatus & 1) != 0) {
                        float floatValue = (Float) message.obj;
                        mClient.mLauncherClientCallbacks.onOverlayScrollChanged(floatValue);
                        if (floatValue <= 0.0f) {
                            mClient.mServiceLog.print("onScroll 0, overlay closed");
                        } else if (floatValue >= 1.0f) {
                            mClient.mServiceLog.print("onScroll 1, overlay opened");
                        } else {
                            mClient.mServiceLog.print("onScroll", floatValue);
                        }
                    }
                    return true;
                case 3:
                    WindowManager.LayoutParams attributes = mWindow.getAttributes();
                    if ((Boolean) message.obj) {
                        attributes.x = mWindowShift;
                        attributes.flags |= 512;
                    } else {
                        attributes.x = 0;
                        attributes.flags &= -513;
                    }
                    mWindowManager.updateViewLayout(mWindow.getDecorView(), attributes);
                    return true;
                case 4:
                    mClient.notifyStatusChanged(message.arg1);
                    mClient.mServiceLog.print("stateChanged", message.arg1);
                    if (mClient.mLauncherClientCallbacks instanceof PrivateCallbacks) {
                        int i = message.arg1;
                        ((PrivateCallbacks) mClient.mLauncherClientCallbacks).mo71a();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    public LauncherClient(Activity activity, LauncherClientCallbacks launcherClientCallbacks, ClientOptions clientOptions) {
        mClientLog = new EventLogArray("Client", 20);
        mServiceLog = new EventLogArray("Service", 10);
        mUpdateReceiver = new UpdateReceiver(this);
        mState = 0;
        mDestroyed = false;
        mServiceStatus = 0;
        mActivity = activity;
        mLauncherClientCallbacks = launcherClientCallbacks;
        mSimpleServiceConnection = new SimpleServiceConnection(activity, 65);
        mServiceConnectionOptions = clientOptions.f19a;
        sApplicationConnection = AppServiceConnection.getInstance(activity);
        mOverlay = sApplicationConnection.registerLauncherClient(this);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 19) {
            intentFilter.addDataSchemeSpecificPart(Constant.GSA_PACKAGE, 0);
        }
        mActivity.registerReceiver(mUpdateReceiver, intentFilter);
        if (sServiceVersion <= 0) {
            loadApiVersion(activity);
        }
        reconnect();
        if (Build.VERSION.SDK_INT >= 19 && mActivity.getWindow() != null
                && mActivity.getWindow().peekDecorView() != null
                && mActivity.getWindow().peekDecorView().isAttachedToWindow()) {
            onAttachedToWindow();
        }
    }

    public final void onAttachedToWindow() {
        if (!mDestroyed) {
            mClientLog.print("attachedToWindow");
            setWindowAttrs(mActivity.getWindow().getAttributes());
        }
    }

    public final void onDetachedFromWindow() {
        if (!mDestroyed) {
            mClientLog.print("detachedFromWindow");
            setWindowAttrs((WindowManager.LayoutParams) null);
        }
    }

    public void onResume() {
        if (!mDestroyed) {
            mState |= 2;
            if (!(mOverlay == null || mWindowAttrs == null)) {
                try {
                    if (sServiceVersion < 4) {
                        mOverlay.mo19d();
                    } else {
                        mOverlay.mo15b(mState);
                    }
                } catch (RemoteException e) {
                }
            }
            mClientLog.print("stateChanged ", mState);
        }
    }

    public void onPause() {
        if (!mDestroyed) {
            mState &= -3;
            if (!(mOverlay == null || mWindowAttrs == null)) {
                try {
                    if (sServiceVersion < 4) {
                        mOverlay.mo17c();
                    } else {
                        mOverlay.mo15b(mState);
                    }
                } catch (RemoteException e) {
                }
            }
            mClientLog.print("stateChanged ", mState);
        }
    }

    public void onStart() {
        if (!mDestroyed) {
            sApplicationConnection.stopService(false);
            reconnect();
            mState |= 1;
            if (!(mOverlay == null || mWindowAttrs == null)) {
                try {
                    mOverlay.mo15b(mState);
                } catch (RemoteException e) {
                }
            }
            mClientLog.print("stateChanged ", mState);
        }
    }

    public void onStop() {
        if (!mDestroyed) {
            sApplicationConnection.stopService(true);
            mSimpleServiceConnection.unbindService();
            mState &= -2;
            if (!(mOverlay == null || mWindowAttrs == null)) {
                try {
                    mOverlay.mo15b(mState);
                } catch (RemoteException e) {
                }
            }
            mClientLog.print("stateChanged ", mState);
        }
    }

    public void onDestroy() {
        disconnect(!mActivity.isChangingConfigurations());
    }

    public void disconnect() {
        disconnect(true);
    }

    public void setClientOptions(ClientOptions clientOptions) {
        if (clientOptions.f19a != mServiceConnectionOptions) {
            mServiceConnectionOptions = clientOptions.f19a;
            if (mWindowAttrs != null) {
                applyWindowToken();
            }
            mClientLog.print("setClientOptions ", mServiceConnectionOptions);
        }
    }

    private void disconnect(boolean unbindService) {
        if (!mDestroyed) {
            mActivity.unregisterReceiver(mUpdateReceiver);
        }
        mDestroyed = true;
        mSimpleServiceConnection.unbindService();
        if (mCurrentCallbacks != null) {
            mCurrentCallbacks.clear();
            mCurrentCallbacks = null;
        }
        sApplicationConnection.unbindService(this, unbindService);
    }

    public void reconnect() {
        if (!mDestroyed) {
            if (!sApplicationConnection.reconnect() || !mSimpleServiceConnection.reconnect()) {
                mActivity.runOnUiThread(new NotifyStatusRunnable(this));
            }
        }
    }

    private final void setWindowAttrs(WindowManager.LayoutParams layoutParams) {
        if (mWindowAttrs != layoutParams) {
            mWindowAttrs = layoutParams;
            if (mWindowAttrs != null) {
                applyWindowToken();
            } else if (mOverlay != null) {
                try {
                    mOverlay.windowDetached(mActivity.isChangingConfigurations());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mOverlay = null;
            }
        }
    }

    private final void applyWindowToken() {
        if (mOverlay != null) {
            try {
                if (mCurrentCallbacks == null) {
                    mCurrentCallbacks = new OverlayCallbacks();
                }
                mCurrentCallbacks.setClient(this);
                if (sServiceVersion < 3) {
                    mOverlay.windowAttached(mWindowAttrs, mCurrentCallbacks, mServiceConnectionOptions);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("layout_params", mWindowAttrs);
                    bundle.putParcelable("configuration", mActivity.getResources().getConfiguration());
                    bundle.putInt("client_options", mServiceConnectionOptions);
                    mOverlay.windowAttached2(bundle, mCurrentCallbacks);
                }
                if (sServiceVersion >= 4) {
                    mOverlay.mo15b(mState);
                } else if ((mState & 2) != 0) {
                    mOverlay.mo19d();
                } else {
                    mOverlay.mo17c();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private final boolean isConnected() {
        return mOverlay != null;
    }

    public void startMove() {
        mClientLog.print("startMove");
        if (isConnected()) {
            try {
                mOverlay.startScroll();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void endMove() {
        mClientLog.print("endMove");
        if (isConnected()) {
            try {
                mOverlay.endScroll();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateMove(float progressX) {
        mClientLog.print("updateMove", progressX);
        if (isConnected()) {
            try {
                mOverlay.onScroll(progressX);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private static int calculateTime(int duration) {
        if (duration > 0 && duration <= 2047) {
            return (duration << 2) | 1;
        }
        throw new IllegalArgumentException("Invalid duration");
    }

    public void hideOverlay(boolean animate) {
        mClientLog.print("hideOverlay", animate);
        if (mOverlay != null) {
            try {
                mOverlay.closeOverlay(animate ? 1 : 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void hideOverlay(int duration) {
        int a = calculateTime(duration);
        mClientLog.print("hideOverlay", duration);
        if (mOverlay != null) {
            try {
                mOverlay.closeOverlay(a);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void showOverlay(boolean z) {
        mClientLog.print("showOverlay", z);
        if (mOverlay != null) {
            try {
                mOverlay.openOverlay(z ? 1 : 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void showOverlay(int duration) {
        int a = calculateTime(duration);
        mClientLog.print("showOverlay", duration);
        if (mOverlay != null) {
            try {
                mOverlay.openOverlay(a);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void requestHotwordDetection(boolean z) {
        mClientLog.print("requestHotwordDetection", z);
        if (mOverlay != null) {
            try {
                mOverlay.requestVoiceDetection(z);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void reattachOverlay() {
        mClientLog.print("reattachOverlay");
        if (mWindowAttrs != null && sServiceVersion >= 7) {
            applyWindowToken();
        }
    }

    public final void setLauncherOverlay(ILauncherOverlay overlay) {
        mServiceLog.print("Connected", overlay != null);
        mOverlay = overlay;
        if (mOverlay == null) {
            notifyStatusChanged(0);
        } else if (mWindowAttrs != null) {
            applyWindowToken();
        }
    }

    public final void notifyStatusChanged(int status) {
        boolean z2 = true;
        if (mServiceStatus != status) {
            mServiceStatus = status;
            if ((status & 2) == 0) {
                z2 = false;
            }
            mLauncherClientCallbacks.onServiceStateChanged((status & 1) != 0, z2);
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.println(String.valueOf(str).concat("LauncherClient"));
        String concat = String.valueOf(str).concat("  ");
        printWriter.println(new StringBuilder(concat.length() + 18).append(concat).append("isConnected: ").append(isConnected()).toString());
        printWriter.println(new StringBuilder(concat.length() + 18).append(concat).append("act.isBound: ").append(mSimpleServiceConnection.isConnected()).toString());
        printWriter.println(new StringBuilder(concat.length() + 18).append(concat).append("app.isBound: ").append(sApplicationConnection.isConnected()).toString());
        printWriter.println(new StringBuilder(concat.length() + 27).append(concat).append("serviceVersion: ").append(sServiceVersion).toString());
        printWriter.println(new StringBuilder(concat.length() + 17).append(concat).append("clientVersion: 14").toString());
        printWriter.println(new StringBuilder(concat.length() + 27).append(concat).append("mActivityState: ").append(mState).toString());
        printWriter.println(new StringBuilder(concat.length() + 27).append(concat).append("mServiceStatus: ").append(mServiceStatus).toString());
        printWriter.println(new StringBuilder(concat.length() + 45).append(concat).append("mCurrentServiceConnectionOptions: ").append(mServiceConnectionOptions).toString());
        mClientLog.print(concat, printWriter);
        mServiceLog.print(concat, printWriter);
    }

    static Intent getIntent(Context context) {
        String packageName = context.getPackageName();
        return new Intent(Constant.ACTION)
                .setPackage(Constant.GSA_PACKAGE)
                .setData(Uri.parse(new StringBuilder(String.valueOf(packageName).length() + 18)
                        .append("app://")
                        .append(packageName)
                        .append(":")
                        .append(Process.myUid())
                        .toString()).buildUpon()
                        .appendQueryParameter("v", Integer.toString(9))
                        .appendQueryParameter("cv", Integer.toString(14))
                        .build());
    }

    public static void loadApiVersion(Context context) {
        ResolveInfo resolveService = context.getPackageManager().resolveService(getIntent(context), PackageManager.GET_META_DATA);
        if (resolveService == null || resolveService.serviceInfo.metaData == null) {
            sServiceVersion = 1;
        } else {
            sServiceVersion = resolveService.serviceInfo.metaData.getInt("service.api.version", 1);
        }
    }
}
