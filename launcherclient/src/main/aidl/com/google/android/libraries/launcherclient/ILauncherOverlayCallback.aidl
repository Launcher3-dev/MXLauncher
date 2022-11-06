// ILauncherOverlayCallback.aidl
package com.google.android.libraries.launcherclient;

// Declare any non-default types here with import statements

interface ILauncherOverlayCallback {

    void overlayScrollChanged(float progress);

    void overlayStatusChanged(int state);

}