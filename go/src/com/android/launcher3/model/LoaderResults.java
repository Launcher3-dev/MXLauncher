/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.model.BgDataModel.Callbacks;

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;

/**
 * Helper class to handle results of {@link com.android.launcher3.model.LoaderTask}.
 */
public class LoaderResults extends BaseLoaderResults {

    public LoaderResults(LauncherAppState app, BgDataModel dataModel,
            AllAppsList allAppsList, Callbacks[] callbacks) {
        super(app, dataModel, allAppsList, callbacks, MAIN_EXECUTOR);
    }

    @Override
    public void bindDeepShortcuts() {
    }

    @Override
    public void bindAllAppToWorkspace() {

    }

    @Override
    public void bindWidgets() {
    }

    @Override
    public void finishBindAllApps(LauncherModel model) {

    }


}
