package helpers.utils;

import java.util.LinkedList;

public class LogBuffer {
    private final LinkedList<String> logs = new LinkedList<>();
    private final int maxEntries;

    public LogBuffer(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public synchronized void addLog(String log) {
        while (logs.size() >= maxEntries) {
            logs.poll(); // Remove the first log entry if we're at capacity
        }
        logs.add(log);
    }

    public synchronized String getAllLogs() {
        StringBuilder builder = new StringBuilder();
        for (String log : logs) {
            builder.append(log).append("\n");
        }
        return builder.toString();
    }
}
