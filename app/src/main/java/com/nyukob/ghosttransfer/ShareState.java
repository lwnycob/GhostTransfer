package com.nyukob.ghosttransfer;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
public class ShareState {
    public static volatile File filePath = null;
    public static volatile String text = "";
    public static volatile boolean serverRunning = false;
    private static final List<String> log = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    public static synchronized void addLog(String msg) {
        log.add("[" + sdf.format(new Date()) + "] " + msg);
        if (log.size() > 100) log.remove(0);
    }
    public static synchronized String getLogText() {
        StringBuilder sb = new StringBuilder();
        for (int i = log.size()-1; i >= 0; i--) sb.append(log.get(i)).append("\n");
        return sb.toString();
    }
    public static synchronized void clearLog() { log.clear(); }
}
