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
package com.android.launcher3.icons.cache;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.LocaleList;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.icons.BitmapInfo;

public interface CachingLogic<T> {

    ComponentName getComponent(T object);

    UserHandle getUser(T object);

    CharSequence getLabel(T object);

    default CharSequence getDescription(T object, CharSequence fallback) {
        return fallback;
    }

    @NonNull
    BitmapInfo loadIcon(Context context, T object);

    /**
     * Provides a option list of keywords to associate with this object
     */
    @Nullable
    default String getKeywords(T object, LocaleList localeList) {
        return null;
    }

    /**
     * Returns the timestamp the entry was last updated in cache.
     */
    default long getLastUpdatedTime(T object, PackageInfo info) {
        return info.lastUpdateTime;
    }

    /**
     * Returns true the object should be added to mem cache; otherwise returns false.
     */
    default boolean addToMemCache() {
        return true;
    }
}
