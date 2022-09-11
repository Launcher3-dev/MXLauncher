package com.android.launcher3.settings;

import android.content.Context;

import com.android.launcher3.config.FeatureFlags;
import com.android.mxlibrary.util.LauncherSpUtil;

public final class MxSettings {

    /**
     * PagedView can scroll circle-endless.
     */
    private boolean isPagedViewCircleScroll = FeatureFlags.LAUNCHER3_CIRCLE_SCROLL;
    private Context mContext;


    private static class SettingHolder {
        private static final MxSettings MX_SETTINGS = new MxSettings();
    }

    public static MxSettings getInstance() {
        return SettingHolder.MX_SETTINGS;
    }

    public void loadSettings(Context context) {
        mContext = context.getApplicationContext();
        loadScreenCycle();
    }

    public void setPagedViewCircleScroll(boolean isPagedViewCircleScroll) {
        this.isPagedViewCircleScroll = isPagedViewCircleScroll;
        LauncherSpUtil.saveBooleanData(mContext, LauncherSpUtil.KEY_PAGE_CIRCLE, isPagedViewCircleScroll);
    }

    public void loadScreenCycle() {
        isPagedViewCircleScroll = LauncherSpUtil.getBooleanData(mContext, LauncherSpUtil.KEY_PAGE_CIRCLE);
    }

    public boolean isPageViewCircleScroll() {
        return isPagedViewCircleScroll;
    }

}
