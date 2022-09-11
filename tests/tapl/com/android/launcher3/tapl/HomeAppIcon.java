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

package com.android.launcher3.tapl;

import android.graphics.Point;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject2;

import java.util.function.Supplier;

/**
 * App icon on the workspace or all apps.
 */
public abstract class HomeAppIcon extends AppIcon implements FolderDragTarget, WorkspaceDragSource {

    private final String mAppName;

    HomeAppIcon(LauncherInstrumentation launcher, UiObject2 icon) {
        super(launcher, icon);
        mAppName = icon.getText();
    }

    /**
     * Drag the AppIcon to the given position of other icon. The drag must result in a folder.
     *
     * @param target the destination icon.
     */
    @NonNull
    public FolderIcon dragToIcon(FolderDragTarget target) {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer("want to drag icon")) {
            final Rect dropBounds = target.getDropLocationBounds();
            Workspace.dragIconToWorkspace(
                    mLauncher, this,
                    () -> {
                        final Rect bounds = target.getDropLocationBounds();
                        return new Point(bounds.centerX(), bounds.centerY());
                    });
            FolderIcon result = target.getTargetFolder(dropBounds);
            mLauncher.assertTrue("Can't find the target folder.", result != null);
            return result;
        }
    }

    /** This method requires public access, however should not be called in tests. */
    @Override
    public Rect getDropLocationBounds() {
        return mLauncher.getVisibleBounds(mObject);
    }

    /** This method requires public access, however should not be called in tests. */
    @Override
    public FolderIcon getTargetFolder(Rect bounds) {
        for (FolderIcon folderIcon : mLauncher.getWorkspace().getFolderIcons()) {
            final Rect folderIconBounds = folderIcon.getDropLocationBounds();
            if (bounds.contains(folderIconBounds.centerX(), folderIconBounds.centerY())) {
                return folderIcon;
            }
        }
        return null;
    }

    @Override
    public HomeAppIconMenu openDeepShortcutMenu() {
        return (HomeAppIconMenu) super.openDeepShortcutMenu();
    }

    @Override
    protected HomeAppIconMenu createMenu(UiObject2 menu) {
        return new HomeAppIconMenu(mLauncher, menu);
    }

    /**
     * Uninstall the appIcon by dragging it to the 'uninstall' drop point of the drop_target_bar.
     *
     * @return validated workspace after the existing appIcon being uninstalled.
     */
    public Workspace uninstall() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     "uninstalling app icon")) {
            return Workspace.uninstallAppIcon(
                    mLauncher, this,
                    this::addExpectedEventsForLongClick
            );
        }
    }

    /**
     * Drag an object to the given cell in workspace. The target cell must be empty.
     *
     * @param cellX zero based column number, starting from the left of the screen.
     * @param cellY zero based row number, starting from the top of the screen.
     */
    public HomeAppIcon dragToWorkspace(int cellX, int cellY) {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     String.format("want to drag the icon to cell(%d, %d)", cellX, cellY))
        ) {
            final Supplier<Point> dest = () -> Workspace.getCellCenter(mLauncher, cellX, cellY);
            Workspace.dragIconToWorkspace(
                    mLauncher,
                    /* launchable= */ this,
                    dest,
                    () -> addExpectedEventsForLongClick(),
                    /*expectDropEvents= */ null);
            try (LauncherInstrumentation.Closable ignore = mLauncher.addContextLayer("dragged")) {
                WorkspaceAppIcon appIcon =
                        (WorkspaceAppIcon) mLauncher.getWorkspace().getWorkspaceAppIcon(mAppName);
                mLauncher.assertTrue(
                        String.format(
                                "The %s icon should be in the cell (%d, %d).", mAppName, cellX,
                                cellY),
                        appIcon.isInCell(cellX, cellY));
                return appIcon;
            }
        }
    }


    /** This method requires public access, however should not be called in tests. */
    @Override
    public Launchable getLaunchable() {
        return this;
    }
}
