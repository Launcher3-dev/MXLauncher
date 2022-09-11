/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.launcher3.util.window;

import static android.view.Display.DEFAULT_DISPLAY;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION;

import static com.android.launcher3.ResourceUtils.INVALID_RESOURCE_HANDLE;
import static com.android.launcher3.ResourceUtils.NAVBAR_HEIGHT;
import static com.android.launcher3.ResourceUtils.NAVBAR_HEIGHT_LANDSCAPE;
import static com.android.launcher3.ResourceUtils.NAVBAR_LANDSCAPE_LEFT_RIGHT_SIZE;
import static com.android.launcher3.ResourceUtils.STATUS_BAR_HEIGHT;
import static com.android.launcher3.ResourceUtils.STATUS_BAR_HEIGHT_LANDSCAPE;
import static com.android.launcher3.ResourceUtils.STATUS_BAR_HEIGHT_PORTRAIT;
import static com.android.launcher3.Utilities.dpiFromPx;
import static com.android.launcher3.util.MainThreadInitializedObject.forOverride;
import static com.android.launcher3.util.RotationUtils.deltaRotation;
import static com.android.launcher3.util.RotationUtils.rotateRect;
import static com.android.launcher3.util.RotationUtils.rotateSize;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Surface;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.android.launcher3.R;
import com.android.launcher3.ResourceUtils;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.MainThreadInitializedObject;
import com.android.launcher3.util.ResourceBasedOverride;
import com.android.launcher3.util.WindowBounds;

/**
 * Utility class for mocking some window manager behaviours
 */
public class WindowManagerProxy implements ResourceBasedOverride {

    public static final int MIN_TABLET_WIDTH = 600;

    public static final MainThreadInitializedObject<WindowManagerProxy> INSTANCE =
            forOverride(WindowManagerProxy.class, R.string.window_manager_proxy_class);

    protected final boolean mTaskbarDrawnInProcess;

    /**
     * Creates a new instance of proxy, applying any overrides
     */
    public static WindowManagerProxy newInstance(Context context) {
        return Overrides.getObject(WindowManagerProxy.class, context,
                R.string.window_manager_proxy_class);
    }

    public WindowManagerProxy() {
        this(false);
    }

    protected WindowManagerProxy(boolean taskbarDrawnInProcess) {
        mTaskbarDrawnInProcess = taskbarDrawnInProcess;
    }

    /**
     * Returns a map of normalized info of internal displays to estimated window bounds
     * for that display
     */
    public ArrayMap<String, Pair<CachedDisplayInfo, WindowBounds[]>> estimateInternalDisplayBounds(
            Context context) {
        Display[] displays = getDisplays(context);
        ArrayMap<String, Pair<CachedDisplayInfo, WindowBounds[]>> result = new ArrayMap<>();
        for (Display display : displays) {
            if (isInternalDisplay(display)) {
                Context displayContext = Utilities.ATLEAST_S
                        ? context.createWindowContext(display, TYPE_APPLICATION, null)
                        : context.createDisplayContext(display);
                CachedDisplayInfo info = getDisplayInfo(displayContext, display).normalize();
                WindowBounds[] bounds = estimateWindowBounds(context, info);
                result.put(info.id, Pair.create(info, bounds));
            }
        }
        return result;
    }

    /**
     * Returns the real bounds for the provided display after applying any insets normalization
     */
    @TargetApi(Build.VERSION_CODES.R)
    public WindowBounds getRealBounds(Context windowContext,
            Display display, CachedDisplayInfo info) {
        if (!Utilities.ATLEAST_R) {
            Point smallestSize = new Point();
            Point largestSize = new Point();
            display.getCurrentSizeRange(smallestSize, largestSize);

            if (info.size.y > info.size.x) {
                // Portrait
                return new WindowBounds(info.size.x, info.size.y, smallestSize.x, largestSize.y,
                        info.rotation);
            } else {
                // Landscape
                new WindowBounds(info.size.x, info.size.y, largestSize.x, smallestSize.y,
                        info.rotation);
            }
        }

        WindowMetrics wm = windowContext.getSystemService(WindowManager.class)
                .getMaximumWindowMetrics();

        Rect insets = new Rect();
        normalizeWindowInsets(windowContext, wm.getWindowInsets(), insets);
        return new WindowBounds(wm.getBounds(), insets, info.rotation);
    }

    /**
     * Returns an updated insets, accounting for various Launcher UI specific overrides like taskbar
     */
    @TargetApi(Build.VERSION_CODES.R)
    public WindowInsets normalizeWindowInsets(Context context, WindowInsets oldInsets,
            Rect outInsets) {
        if (!Utilities.ATLEAST_R || !mTaskbarDrawnInProcess) {
            outInsets.set(oldInsets.getSystemWindowInsetLeft(), oldInsets.getSystemWindowInsetTop(),
                    oldInsets.getSystemWindowInsetRight(), oldInsets.getSystemWindowInsetBottom());
            return oldInsets;
        }

        WindowInsets.Builder insetsBuilder = new WindowInsets.Builder(oldInsets);
        Insets navInsets = oldInsets.getInsets(WindowInsets.Type.navigationBars());

        Resources systemRes = context.getResources();
        Configuration config = systemRes.getConfiguration();

        boolean isTablet = config.smallestScreenWidthDp > MIN_TABLET_WIDTH;
        boolean isGesture = isGestureNav(context);
        boolean isPortrait = config.screenHeightDp > config.screenWidthDp;

        int bottomNav = isTablet
                ? 0
                : (isPortrait
                        ? getDimenByName(systemRes, NAVBAR_HEIGHT)
                        : (isGesture
                                ? getDimenByName(systemRes, NAVBAR_HEIGHT_LANDSCAPE)
                                : 0));
        Insets newNavInsets = Insets.of(navInsets.left, navInsets.top, navInsets.right, bottomNav);
        insetsBuilder.setInsets(WindowInsets.Type.navigationBars(), newNavInsets);
        insetsBuilder.setInsetsIgnoringVisibility(WindowInsets.Type.navigationBars(), newNavInsets);

        Insets statusBarInsets = oldInsets.getInsets(WindowInsets.Type.statusBars());


        int statusBarHeight = getDimenByName(systemRes,
                (isPortrait) ? STATUS_BAR_HEIGHT_PORTRAIT : STATUS_BAR_HEIGHT_LANDSCAPE,
                STATUS_BAR_HEIGHT);

        Insets newStatusBarInsets = Insets.of(
                statusBarInsets.left,
                Math.max(statusBarInsets.top, statusBarHeight),
                statusBarInsets.right,
                statusBarInsets.bottom);
        insetsBuilder.setInsets(WindowInsets.Type.statusBars(), newStatusBarInsets);
        insetsBuilder.setInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars(), newStatusBarInsets);

        // Override the tappable insets to be 0 on the bottom for gesture nav (otherwise taskbar
        // would count towards it). This is used for the bottom protection in All Apps for example.
        if (isGesture) {
            Insets oldTappableInsets = oldInsets.getInsets(WindowInsets.Type.tappableElement());
            Insets newTappableInsets = Insets.of(oldTappableInsets.left, oldTappableInsets.top,
                    oldTappableInsets.right, 0);
            insetsBuilder.setInsets(WindowInsets.Type.tappableElement(), newTappableInsets);
        }

        WindowInsets result = insetsBuilder.build();
        Insets systemWindowInsets = result.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        outInsets.set(systemWindowInsets.left, systemWindowInsets.top, systemWindowInsets.right,
                systemWindowInsets.bottom);
        return result;
    }

    /**
     * Returns true if the display is an internal displays
     */
    protected boolean isInternalDisplay(Display display) {
        return display.getDisplayId() == Display.DEFAULT_DISPLAY;
    }

    /**
     * Returns a list of possible WindowBounds for the display keyed on the 4 surface rotations
     */
    public WindowBounds[] estimateWindowBounds(Context context, CachedDisplayInfo display) {
        int densityDpi = context.getResources().getConfiguration().densityDpi;
        int rotation = display.rotation;
        Rect safeCutout = display.cutout;

        int minSize = Math.min(display.size.x, display.size.y);
        int swDp = (int) dpiFromPx(minSize, densityDpi);

        Resources systemRes;
        {
            Configuration conf = new Configuration();
            conf.smallestScreenWidthDp = swDp;
            systemRes = context.createConfigurationContext(conf).getResources();
        }

        boolean isTablet = swDp >= MIN_TABLET_WIDTH;
        boolean isTabletOrGesture = isTablet
                || (Utilities.ATLEAST_R && isGestureNav(context));

        int statusBarHeightPortrait = getDimenByName(systemRes,
                STATUS_BAR_HEIGHT_PORTRAIT, STATUS_BAR_HEIGHT);
        int statusBarHeightLandscape = getDimenByName(systemRes,
                STATUS_BAR_HEIGHT_LANDSCAPE, STATUS_BAR_HEIGHT);

        int navBarHeightPortrait, navBarHeightLandscape, navbarWidthLandscape;

        navBarHeightPortrait = isTablet
                ? (mTaskbarDrawnInProcess
                        ? 0 : systemRes.getDimensionPixelSize(R.dimen.taskbar_size))
                : getDimenByName(systemRes, NAVBAR_HEIGHT);

        navBarHeightLandscape = isTablet
                ? (mTaskbarDrawnInProcess
                        ? 0 : systemRes.getDimensionPixelSize(R.dimen.taskbar_size))
                : (isTabletOrGesture
                        ? getDimenByName(systemRes, NAVBAR_HEIGHT_LANDSCAPE) : 0);
        navbarWidthLandscape = isTabletOrGesture
                ? 0
                : getDimenByName(systemRes, NAVBAR_LANDSCAPE_LEFT_RIGHT_SIZE);

        WindowBounds[] result = new WindowBounds[4];
        Point tempSize = new Point();
        for (int i = 0; i < 4; i++) {
            int rotationChange = deltaRotation(rotation, i);
            tempSize.set(display.size.x, display.size.y);
            rotateSize(tempSize, rotationChange);
            Rect bounds = new Rect(0, 0, tempSize.x, tempSize.y);

            int navBarHeight, navbarWidth, statusBarHeight;
            if (tempSize.y > tempSize.x) {
                navBarHeight = navBarHeightPortrait;
                navbarWidth = 0;
                statusBarHeight = statusBarHeightPortrait;
            } else {
                navBarHeight = navBarHeightLandscape;
                navbarWidth = navbarWidthLandscape;
                statusBarHeight = statusBarHeightLandscape;
            }

            Rect insets = new Rect(safeCutout);
            rotateRect(insets, rotationChange);
            insets.top = Math.max(insets.top, statusBarHeight);
            insets.bottom = Math.max(insets.bottom, navBarHeight);

            if (i == Surface.ROTATION_270 || i == Surface.ROTATION_180) {
                // On reverse landscape (and in rare-case when the natural orientation of the
                // device is landscape), navigation bar is on the right.
                insets.left = Math.max(insets.left, navbarWidth);
            } else {
                insets.right = Math.max(insets.right, navbarWidth);
            }
            result[i] = new WindowBounds(bounds, insets, i);
        }
        return result;
    }

    /**
     * Wrapper around the utility method for easier emulation
     */
    protected int getDimenByName(Resources res, String resName) {
        return ResourceUtils.getDimenByName(resName, res, 0);
    }

    /**
     * Wrapper around the utility method for easier emulation
     */
    protected int getDimenByName(Resources res, String resName, String fallback) {
        int dimen = ResourceUtils.getDimenByName(resName, res, -1);
        return dimen > -1 ? dimen : getDimenByName(res, fallback);
    }

    protected boolean isGestureNav(Context context) {
        return ResourceUtils.getIntegerByName("config_navBarInteractionMode",
                context.getResources(), INVALID_RESOURCE_HANDLE) == 2;
    }

    /**
     * Returns a CachedDisplayInfo initialized for the current display
     */
    @TargetApi(Build.VERSION_CODES.S)
    public CachedDisplayInfo getDisplayInfo(Context displayContext, Display display) {
        int rotation = getRotation(displayContext);
        Rect cutoutRect = new Rect();
        Point size = new Point();
        if (Utilities.ATLEAST_S) {
            WindowMetrics wm = displayContext.getSystemService(WindowManager.class)
                    .getMaximumWindowMetrics();
            DisplayCutout cutout = wm.getWindowInsets().getDisplayCutout();
            if (cutout != null) {
                cutoutRect.set(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                        cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
            }

            size.set(wm.getBounds().right, wm.getBounds().bottom);
        } else {
            display.getRealSize(size);
        }
        return new CachedDisplayInfo(getDisplayId(display), size, rotation, cutoutRect);
    }

    /**
     * Returns a unique ID representing the display
     */
    protected String getDisplayId(Display display) {
        return Integer.toString(display.getDisplayId());
    }

    /**
     * Returns rotation of the display associated with the context.
     */
    public int getRotation(Context context) {
        Display d = null;
        if (Utilities.ATLEAST_R) {
            try {
                d = context.getDisplay();
            } catch (UnsupportedOperationException e) {
                // Ignore
            }
        }
        if (d == null) {
            d = context.getSystemService(DisplayManager.class).getDisplay(DEFAULT_DISPLAY);
        }
        return d.getRotation();
    }

    /**
     * Returns all currently valid logical displays.
     */
    protected Display[] getDisplays(Context context) {
        return context.getSystemService(DisplayManager.class).getDisplays();
    }
}
