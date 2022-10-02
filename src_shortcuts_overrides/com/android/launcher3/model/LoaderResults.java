/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.model;

import android.content.ComponentName;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.util.Log;
import android.util.Pair;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.model.BgDataModel.Callbacks;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.widget.model.WidgetsListBaseEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
import static com.android.launcher3.model.data.AppInfo.makeLaunchIntent;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;

/**
 * Helper class to handle results of {@link com.android.launcher3.model.LoaderTask}.
 */
public class LoaderResults extends BaseLoaderResults {

    public static final String TAG = "Launcher.LoaderResults";

    public LoaderResults(LauncherAppState app, BgDataModel dataModel,
                         AllAppsList allAppsList, Callbacks[] callbacks) {
        super(app, dataModel, allAppsList, callbacks, MAIN_EXECUTOR);
    }

    @Override
    public void bindDeepShortcuts() {
        final HashMap<ComponentKey, Integer> shortcutMapCopy;
        synchronized (mBgDataModel) {
            shortcutMapCopy = new HashMap<>(mBgDataModel.deepShortcutMap);
        }
        executeCallbacksTask(c -> c.bindDeepShortcutMap(shortcutMapCopy), mUiExecutor);
    }

    @Override
    public void bindAllAppToWorkspace() {
        ArrayList<AppInfo> allApps = (ArrayList<AppInfo>) mBgAllAppsList.data.clone();

        // Add the items and clear queue
        if (allApps.isEmpty()) {
            Log.d(TAG, "bindAllAppToWorkspace: all app is null");
            return;
        }
        Log.i(TAG, "bindAllAppToWorkspace: all app size is " + allApps.size());
        // add log
        mApp.getModel().addAndBindAllAddedWorkspaceItems(allApps);
    }

    @Override
    public void bindWidgets() {
        final List<WidgetsListBaseEntry> widgets =
                mBgDataModel.widgetsModel.getWidgetsListForPicker(mApp.getContext());
        executeCallbacksTask(c -> c.bindAllWidgets(widgets), mUiExecutor);
    }

    private Pair<ItemInfo, Object> createApplicationPair(AppInfo appInfo) {
        String packageName = appInfo.intent.getPackage();
        List<LauncherActivityInfo> laiList =
                mApp.getContext().getSystemService(LauncherApps.class)
                        .getActivityList(packageName, appInfo.user);

        final WorkspaceItemInfo si = appInfo.makeWorkspaceItem(mApp.getContext());
        si.user = appInfo.user;
        si.itemType = ITEM_TYPE_APPLICATION;

        LauncherActivityInfo lai;
        boolean usePackageIcon = laiList.isEmpty();
        if (usePackageIcon) {
            lai = null;
            si.intent = makeLaunchIntent(new ComponentName(packageName, ""))
                    .setPackage(packageName);
            si.status |= WorkspaceItemInfo.FLAG_AUTOINSTALL_ICON;
        } else {
            lai = laiList.get(0);
            si.intent = makeLaunchIntent(lai);
        }
        mApp.getIconCache()
                .getTitleAndIcon(si, () -> lai, usePackageIcon, false);
        return Pair.create(si, null);
    }

}
