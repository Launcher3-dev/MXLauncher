package com.google.android.libraries.gsa.launcherclient;

final class LogInfo {
    public int type;
    public String event;
    public float extras;
    public long time;
    public int duplicateCount;

    private LogInfo() {
    }

    public void update(int type, String event, float extras) {
        this.type = type;
        this.event = event;
        this.extras = extras;
        this.time = System.currentTimeMillis();
        this.duplicateCount = 0;
    }

    static int updateDuplicateCount(LogInfo eVar) {
        int duplicateCount = eVar.duplicateCount;
        eVar.duplicateCount = duplicateCount + 1;
        return duplicateCount;
    }

    LogInfo(byte b) {
        this();
    }
}
