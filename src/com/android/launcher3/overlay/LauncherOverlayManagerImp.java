package com.android.launcher3.overlay;

import android.app.Activity;
import android.os.Bundle;

import com.android.launcher3.Launcher;
import com.android.systemui.plugins.shared.LauncherOverlayManager;

import java.io.PrintWriter;

/**
 * Created by yuchuan
 * DATE 2022/10/29
 * TIME 21:04
 */
public class LauncherOverlayManagerImp implements LauncherOverlayManager,
        LauncherOverlayManager.LauncherOverlay {

    public static final String TAG = "Launcher.LauncherOverlayManagerImp";

    private LauncherOverlayManager mOverlayManager;
    private LauncherOverlayManager.LauncherOverlay mLauncherOverlay;

    public LauncherOverlayManagerImp(Launcher launcher) {
        initGoogleOverlay(launcher);
    }

    private void initGoogleOverlay(Launcher launcher) {
        GoogleOverlay googleOverlay  =  new GoogleOverlay(launcher);
        mOverlayManager = googleOverlay;
        mLauncherOverlay = googleOverlay;
    }

    public void onDeviceProvideChanged() {
        if (mOverlayManager != null) {
            mOverlayManager.onDeviceProvideChanged();
        }
    }

    public void onAttachedToWindow() {
        if (mOverlayManager != null) {
            mOverlayManager.onAttachedToWindow();
        }
    }

    public void onDetachedFromWindow() {
        if (mOverlayManager != null) {
            mOverlayManager.onDetachedFromWindow();
            mOverlayManager = null;
        }
        mLauncherOverlay = null;
    }

    public void dump(String prefix, PrintWriter w) {
        if (mOverlayManager != null) {
            mOverlayManager.dump(prefix, w);
        }
    }

    public void openOverlay() {
        if (mOverlayManager != null) {
            mOverlayManager.openOverlay();
        }
    }

    public void hideOverlay(boolean animate) {
        hideOverlay(animate ? 200 : 0);
    }

    public void hideOverlay(int duration) {
        if (mOverlayManager != null) {
            mOverlayManager.hideOverlay(duration);
        }
    }

    public boolean startSearch(byte[] config, Bundle extras) {
        if (mOverlayManager != null) {
            return mOverlayManager.startSearch(config, extras);
        }
        return false;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivityCreated(activity, bundle);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivityStarted(activity);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivityResumed(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivityStopped(activity);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivitySaveInstanceState(activity, bundle);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mOverlayManager != null) {
            mOverlayManager.onActivityDestroyed(activity);
        }
    }

    @Override
    public void onScrollInteractionBegin() {
        if (mLauncherOverlay != null){
            mLauncherOverlay.onScrollInteractionBegin();
        }
    }

    @Override
    public void onScrollInteractionEnd() {
        if (mLauncherOverlay != null){
            mLauncherOverlay.onScrollInteractionEnd();
        }
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        if (mLauncherOverlay != null){
            mLauncherOverlay.onScrollChange(progress, rtl);
        }
    }

    @Override
    public void setOverlayCallbacks(LauncherOverlayCallbacks callbacks) {
        if (mLauncherOverlay != null){
            mLauncherOverlay.setOverlayCallbacks(callbacks);
        }
    }
}
