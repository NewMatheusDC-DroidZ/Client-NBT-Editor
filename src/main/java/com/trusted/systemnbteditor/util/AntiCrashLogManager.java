package com.trusted.systemnbteditor.util;

import java.util.ArrayList;
import java.util.List;

public class AntiCrashLogManager {
    private static final List<String> logs = new ArrayList<>();
    private static final int MAX_LOGS = 100;

    public static void addLog(String nbt) {
        synchronized (logs) {
            logs.addFirst(nbt);
            if (logs.size() > MAX_LOGS) {
                logs.removeLast();
            }
        }
    }

    public static List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public static void clear() {
        synchronized (logs) {
            logs.clear();
        }
    }
}
