package com.android.launcher3.settings;

import android.content.Context;

import com.android.launcher3.config.FeatureFlags;
import com.android.mxlibrary.util.LauncherSpUtil;

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

    private Context mContext;


    private static class SettingHolder {
        private static final MxSettings MX_SETTINGS = new MxSettings();
    }

    public static MxSettings getInstance() {
        return SettingHolder.MX_SETTINGS;
    }

    public void loadSettings(Context context) {
        mContext = context.getApplicationContext();
        init();
    }

    private void init() {
        isPagedViewCircleScroll = LauncherSpUtil.getBooleanData(mContext, LauncherSpUtil.KEY_PAGE_CIRCLE);
        isDrawerEnable = LauncherSpUtil.getBooleanData(mContext, LauncherSpUtil.KEY_DRAWER_ENABLE);
    }

    public void setPagedViewCircleScroll(boolean isPagedViewCircleScroll) {
        this.isPagedViewCircleScroll = isPagedViewCircleScroll;
        LauncherSpUtil.saveBooleanData(mContext, LauncherSpUtil.KEY_PAGE_CIRCLE, isPagedViewCircleScroll);
    }

    public boolean isPageViewCircleScroll() {
        return isPagedViewCircleScroll;
    }


    public boolean isDrawerEnable() {
        return isDrawerEnable;
    }

    public void setDrawerEnable(boolean drawerEnable) {
        isDrawerEnable = drawerEnable;
        LauncherSpUtil.saveBooleanData(mContext, LauncherSpUtil.KEY_DRAWER_ENABLE, drawerEnable);
    }

}
