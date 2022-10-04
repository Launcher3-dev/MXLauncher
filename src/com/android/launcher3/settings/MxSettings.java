package com.android.launcher3.settings;

import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.LauncherSpUtil;

public final class MxSettings {

    /**
     * PagedView can scroll circle-endless.
     */
    private boolean isPagedViewCircleScroll = FeatureFlags.LAUNCHER3_CIRCLE_SCROLL;

    /**
     * ture: all apps show in LauncherAllAppsContainerView,
     * false: all apps show in workspace
     */
    private boolean isDrawerEnable = FeatureFlags.LAUNCHER3_ENABLE_DRAWER;

    private Launcher mLauncher;

    private static class SettingHolder {
        private static final MxSettings MX_SETTINGS = new MxSettings();
    }

    public static MxSettings getInstance() {
        return SettingHolder.MX_SETTINGS;
    }

    public void loadSettings(Launcher launcher) {
        mLauncher = launcher;
        init();
    }

    private void init() {
        isPagedViewCircleScroll = LauncherSpUtil.getBooleanData(mLauncher, LauncherSpUtil.KEY_PAGE_CIRCLE);
        isDrawerEnable = LauncherSpUtil.getBooleanDataWithDefault(mLauncher, LauncherSpUtil.PREF_DRAWER_ENABLE, FeatureFlags.LAUNCHER3_ENABLE_DRAWER);
    }

    public void setPagedViewCircleScroll(boolean isPagedViewCircleScroll) {
        this.isPagedViewCircleScroll = isPagedViewCircleScroll;
        LauncherSpUtil.saveBooleanData(mLauncher, LauncherSpUtil.KEY_PAGE_CIRCLE, isPagedViewCircleScroll);
    }

    public boolean isPageViewCircleScroll() {
        return isPagedViewCircleScroll;
    }


    public boolean isDrawerEnable() {
        return isDrawerEnable;
    }

    public void setDrawerEnable(boolean drawerEnable) {
        isDrawerEnable = drawerEnable;
        LauncherSpUtil.saveBooleanData(mLauncher, LauncherSpUtil.PREF_DRAWER_ENABLE, drawerEnable);
        mLauncher.getModel().forceReload();
    }

}
