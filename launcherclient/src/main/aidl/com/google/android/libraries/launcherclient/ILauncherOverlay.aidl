// ILauncherOverlay.aidl
package com.google.android.libraries.launcherclient;

import android.os.Bundle;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback;
// Declare any non-default types here with import statements

interface ILauncherOverlay {

    void onScroll(float progress);

    void closeOverlay(int i);

    void onAttachedToWindow(in Bundle bundle, ILauncherOverlayCallback dVar);

    void onDetachedFromWindow(boolean changingConfigurations);
    
    void startScroll();

    void endScroll();

    void onStart(int i);

    void requestVoiceDetection(boolean start);

    void onPause();

    void openOverlay(int i);

    void onResume();

    boolean isVoiceDetectionRunning();

    boolean requestState();
}