package com.google.android.libraries.gsa.launcherclient;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class EventLogArray {

    private static final int TYPE_ONE_OFF = 0;
    private static final int TYPE_FLOAT = 1;
    private static final int TYPE_INTEGER = 2;
    private static final int TYPE_BOOL_TRUE = 3;
    private static final int TYPE_BOOL_FALSE = 4;

    private final String tag;

    private final LogInfo[] logs;

    private int nextIndex = 0;

    public EventLogArray(String event, int size) {
        tag = event;
        logs = new LogInfo[size];
    }

    public void print(String event) {
        print(TYPE_ONE_OFF, event, 0.0f);
    }

    public void print(String event, int extras) {
        print(TYPE_INTEGER, event, (float) extras);
    }

    public void print(String event, float extras) {
        print(TYPE_FLOAT, event, extras);
    }

    public void print(String event, boolean extras) {
        print(extras ? TYPE_BOOL_TRUE : TYPE_BOOL_FALSE, event, 0.0f);
    }

    private void print(int type, String event, float extras) {
        int last = ((nextIndex + logs.length) - 1) % logs.length;
        int secondLast = ((nextIndex + logs.length) - 2) % logs.length;
        if (!isEntrySame(logs[last], type, event) || !isEntrySame(logs[secondLast], type, event)) {
            if (logs[nextIndex] == null) {
                logs[nextIndex] = new LogInfo((byte) 0);
            }
            logs[nextIndex].update(type, event, extras);
            nextIndex = (nextIndex + 1) % logs.length;
            return;
        }
        logs[last].update(type, event, extras);
        LogInfo.updateDuplicateCount(logs[secondLast]);
    }

    public void print(String event, PrintWriter printWriter) {
        String str2 = tag;
        printWriter.println(new StringBuilder(String.valueOf(event).length() + 15
                + String.valueOf(str2).length()).append(event).append(str2).append(" event history:"));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("  HH:mm:ss.SSSZ  ", Locale.US);
        Date date = new Date();
        for (int i = 0; i < logs.length; i++) {
            LogInfo logInfo = logs[(((nextIndex + logs.length) - i) - 1) % logs.length];
            if (logInfo != null) {
                date.setTime(logInfo.time);
                StringBuilder append = new StringBuilder(event).append(simpleDateFormat.format(date)).append(logInfo.event);
                switch (logInfo.type) {
                    case TYPE_FLOAT:
                        append.append(": ").append(logInfo.extras);
                        break;
                    case TYPE_INTEGER:
                        append.append(": ").append((int) logInfo.extras);
                        break;
                    case TYPE_BOOL_TRUE:
                        append.append(": true");
                        break;
                    case TYPE_BOOL_FALSE:
                        append.append(": false");
                        break;
                }
                if (logInfo.duplicateCount > 0) {
                    append.append(" & ").append(logInfo.duplicateCount).append(" similar events");
                }
                printWriter.println(append);
            }
        }
    }

    private static boolean isEntrySame(LogInfo logInfo, int type, String event) {
        return logInfo != null && logInfo.type == type && logInfo.event.equals(event);
    }
}
