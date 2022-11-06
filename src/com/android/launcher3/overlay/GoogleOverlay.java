package com.android.launcher3.overlay;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.systemui.plugins.shared.LauncherOverlayManager;
import com.google.android.libraries.gsa.launcherclient.LauncherClient;
import com.google.android.libraries.gsa.launcherclient.LauncherClientCallbacks;

import java.io.PrintWriter;

/**
 * Created by yuchuan
 * DATE 2022/10/29
 * TIME 21:01
 */
public class GoogleOverlay implements LauncherOverlayManager, LauncherOverlayManager.LauncherOverlay,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "Launcher.GoogleOverlay";

    private static final String KEY_ENABLE_MINUS_ONE = "pref_enable_minus_one";

    private Launcher mLauncher;
    private LauncherClient mClient;

    private boolean mWasOverlayAttached = false;
    private LauncherOverlayManager.LauncherOverlayCallbacks mLauncherOverlayCallbacks;

    public GoogleOverlay(Launcher launcher) {
        mLauncher = launcher;
        SharedPreferences prefs = Utilities.getPrefs(launcher);
        LauncherClientCallbacks mLauncherClientCallbacks = new LauncherClientCallbacks() {
            @Override
            public void onOverlayScrollChanged(float progress) {
                Log.d(TAG, "onOverlayScrollChanged: " + progress);
                if (mLauncherOverlayCallbacks != null) {
                    mLauncherOverlayCallbacks.onScrollChanged(progress);
                }
            }

            @Override
            public void onServiceStateChanged(boolean overlayAttached, boolean hotwordActive) {
                Log.d(TAG, "onServiceStateChanged: " + overlayAttached);
                if (mWasOverlayAttached != overlayAttached) {
                    mWasOverlayAttached = overlayAttached;
                    mLauncher.setLauncherOverlay(overlayAttached ? GoogleOverlay.this : null);
                }
            }
        };
        mClient = new LauncherClient(launcher, mLauncherClientCallbacks, getClientOptions(prefs));
        prefs.registerOnSharedPreferenceChangeListener(this);
        Log.d(TAG, "GoogleOverlay: init");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d(TAG, "onActivityStarted: ");
        if (mClient != null) {
            mClient.onStart();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(TAG, "onActivityResumed: ");
        if (mClient != null) {
            mClient.onResume();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "onActivityPaused: ");
        if (mClient != null) {
            mClient.onPause();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d(TAG, "onActivityStopped: ");
        if (mClient != null) {
            mClient.onStop();
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mClient != null) {
            mClient.onDestroy();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow: ");
        if (mClient != null) {
            mClient.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow: ");
        if (mClient != null) {
            mClient.onDetachedFromWindow();
            mClient = null;
        }
        mLauncher.getSharedPrefs().unregisterOnSharedPreferenceChangeListener(this);
        mLauncher = null;
    }

    @Override
    public void openOverlay() {
        Log.d(TAG, "openOverlay: ");
        if (mClient != null) {
            mClient.showOverlay(true);
        }
    }

    @Override
    public void hideOverlay(int duration) {
        Log.d(TAG, "hideOverlay: ");
        if (mClient != null) {
            mClient.hideOverlay(duration);
        }
    }

    @Override
    public void hideOverlay(boolean animate) {
        Log.d(TAG, "hideOverlay: ");
        if (mClient != null) {
            mClient.hideOverlay(animate);
        }
    }

    @Override
    public void dump(String prefix, PrintWriter w) {
        if (mClient != null) {
            mClient.dump(prefix, w);
        }
    }

    @Override
    public void onScrollInteractionBegin() {
        Log.d(TAG, "onScrollInteractionBegin: ");
        if (mClient != null) {
            mClient.startMove();
        }
    }

    @Override
    public void onScrollInteractionEnd() {
        Log.d(TAG, "onScrollInteractionEnd: ");
        if (mClient != null) {
            mClient.endMove();
        }
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        Log.d(TAG, "onScrollChange # progress: " + progress + " ,rtl: " + rtl);
        if (mClient != null) {
            mClient.updateMove(progress);
        }
    }

    @Override
    public void setOverlayCallbacks(LauncherOverlayManager.LauncherOverlayCallbacks callbacks) {
        mLauncherOverlayCallbacks = callbacks;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mClient != null) {
            if (KEY_ENABLE_MINUS_ONE.equals(key)) {
                mClient.setClientOptions(getClientOptions(sharedPreferences));
            }
        }
    }

    private LauncherClient.ClientOptions getClientOptions(SharedPreferences prefs) {
        return new LauncherClient.ClientOptions(
                prefs.getBoolean(KEY_ENABLE_MINUS_ONE, true),
                true, /* enableHotword */
                true /* enablePrewarming */
        );
    }

}
