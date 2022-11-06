package com.google.android.libraries.gsa.launcherclient;

final class ServiceStatusRunnable implements Runnable {

    private final AbsServiceStatusChecker.StatusCallback statusCallback;

    private final AbsServiceStatusChecker absServiceStatusChecker;

    ServiceStatusRunnable(AbsServiceStatusChecker absServiceStatusChecker, AbsServiceStatusChecker.StatusCallback statusCallback) {
        this.absServiceStatusChecker = absServiceStatusChecker;
        this.statusCallback = statusCallback;
    }

    public void run() {
        AbsServiceStatusChecker.assertMainThread();
        this.statusCallback.isRunning(false);
    }
}
