package com.google.android.libraries.gsa.launcherclient;

public interface LauncherClientCallbacks {
    void onOverlayScrollChanged(float progress);

    void onServiceStateChanged(boolean z, boolean z2);
}
