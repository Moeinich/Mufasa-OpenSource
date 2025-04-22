package helpers.emulator.utils;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import helpers.CacheManager;
import helpers.Logger;
import helpers.ThreadManager;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DirectCaptureUtils {
    private final CacheManager cacheManager;
    private final Logger logger;

    private final String UNINSTALL_STRING_QUERY = "reg query \"HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\LDPlayer9\" /v UninstallString";
    private final String DNCONSOLE_EXE = "dnconsole.exe";
    private final String LDPLAYER_CONFIG_REGEX = "leidian[0-9]+\\.config";
    private final String BLUESTACKS_PROCESS = "HD-Player.exe";
    private final String LDPLAYER_PROCESS = "dnplayer.exe";

    // Caching and Throttling
    private final ConcurrentHashMap<Long, String> windowTitleCache = new ConcurrentHashMap<>();
    private final AtomicLong lastWindowTitleFetchTime = new AtomicLong(0);
    private static final long WINDOW_TITLE_FETCH_COOLDOWN_MS = 500; // 0.5 seconds

    private final ExecutorService executor = ThreadManager.getInstance().getUnifiedExecutor();

    public DirectCaptureUtils(CacheManager cacheManager, Logger logger) {
        this.cacheManager = cacheManager;
        this.logger = logger;
    }

    /**
     * Retrieves the EmulatorCaptureInfo for a specific device.
     * If the device is not in the cache, it attempts to map and cache it.
     *
     * @param device The device identifier (e.g., "emulator-5556").
     * @return The EmulatorCaptureInfo object, or null if mapping fails.
     */
    public EmulatorCaptureInfo getEmulatorCaptureInfo(String device) {
        // Check if the device is already cached
        EmulatorCaptureInfo cachedInfo = cacheManager.getEmulatorCaptureInfo(device);
        if (cachedInfo != null) {
            if (isProcessRunning(cachedInfo.getPid())) {
                return cachedInfo;
            } else {
                logger.print("Cached PID " + cachedInfo.getPid() + " for device " + device + " is no longer active.");
                cacheManager.removeEmulatorCaptureInfo(device);
            }
        }

        String portStr = extractPort(device);
        if (portStr == null) {
            logger.print("Failed to extract port for device: " + device);
            return null;
        }

        int portNumber;
        try {
            portNumber = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            logger.print("Invalid port number extracted for device " + device + ": " + portStr);
            return null;
        }

        EmulatorCaptureInfo emulatorCaptureInfo = findEmulatorPIDByPort(portNumber);
        if (emulatorCaptureInfo != null) {
            logger.print("Mapped EmulatorCaptureInfo for device " + device + " with PID: " + emulatorCaptureInfo.getPid());
            cacheManager.setEmulatorCaptureInfo(device, emulatorCaptureInfo);
            return emulatorCaptureInfo;
        } else {
            logger.print("Failed to map EmulatorCaptureInfo for device " + device);
            return null;
        }
    }

    /**
     * Checks if a process with the given PID is still running.
     *
     * @param pid The process ID.
     * @return True if the process is running, false otherwise.
     */
    private boolean isProcessRunning(int pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private String findLdplayerPath() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(UNINSTALL_STRING_QUERY);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String dnconsolePath = reader.lines()
                        .filter(line -> line.contains("UninstallString"))
                        .map(line -> line.split("REG_SZ")[1].trim())
                        .map(uninstallPath -> Paths.get(uninstallPath).getParent().resolve(DNCONSOLE_EXE).toString())
                        .findFirst()
                        .orElse(null);
                process.waitFor();
                return dnconsolePath;
            }
        } catch (Exception e) {
            logger.print("Error finding LDPlayer path: " + e);
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Retrieves the window title for a given PID using JNA.
     *
     * @param pid The process ID.
     * @return The window title as a String, or "Unknown" if not found.
     */
    private String getWindowTitle(long pid) {
        User32 user32 = User32.INSTANCE;

        HWND hwnd = getMainWindowHandle(pid);
        if (hwnd == null) {
            logger.print("No window handle found for PID: " + pid);
            return "Unknown";
        }

        char[] windowText = new char[512];
        user32.GetWindowText(hwnd, windowText, 512);
        String title = Native.toString(windowText);

        if (title.isEmpty()) {
            logger.print("Window title is empty for PID: " + pid);
            return "Unknown";
        }

        return title;
    }

    /**
     * Retrieves the main window handle for a given PID.
     *
     * @param pid The process ID.
     * @return The HWND handle, or null if not found.
     */
    private HWND getMainWindowHandle(long pid) {
        final HWND[] foundHwnd = {null};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            IntByReference windowPid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);
            if (windowPid.getValue() == pid) {
                // Check if the window is visible and has a title
                if (User32.INSTANCE.IsWindowVisible(hwnd)) {
                    char[] windowText = new char[512];
                    User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
                    String title = Native.toString(windowText);
                    if (!title.isEmpty()) {
                        foundHwnd[0] = hwnd;
                        return false; // Stop enumeration
                    }
                }
            }
            return true; // Continue enumeration
        }, null);

        // Check Window state here
        checkWindowState(foundHwnd[0]);

        return foundHwnd[0];
    }

    /**
     * Retrieves the window title with caching and throttling.
     *
     * @param pid The process ID.
     * @return The window title as a String, or "Unknown" if not found.
     */
    public String getWindowTitleCachedOrThrottled(long pid) {
        // Check cache first
        String cachedTitle = windowTitleCache.get(pid);
        if (cachedTitle != null) {
            return cachedTitle;
        }

        long currentTime = System.currentTimeMillis();
        long lastFetchTime = lastWindowTitleFetchTime.get();

        if ((currentTime - lastFetchTime) < WINDOW_TITLE_FETCH_COOLDOWN_MS) {
            // Return cached title or "Unknown" to avoid fetching
            return windowTitleCache.getOrDefault(pid, "Unknown");
        }

        if (lastWindowTitleFetchTime.compareAndSet(lastFetchTime, currentTime)) {
            // Safe to fetch
            String title = getWindowTitle(pid);
            windowTitleCache.put(pid, title);
            return title;
        } else {
            // Another thread updated the fetch time; return cached title
            return windowTitleCache.getOrDefault(pid, "Unknown");
        }
    }

    /**
     * Finds Emulator PID by port number.
     *
     * @param port The port number.
     * @return EmulatorCaptureInfo if found, null otherwise.
     */
    public EmulatorCaptureInfo findEmulatorPIDByPort(int port) {
        List<ProcessHandle> ldplayerProcesses = getProcessHandles(LDPLAYER_PROCESS);
        List<ProcessHandle> bluestacksProcesses = getProcessHandles(BLUESTACKS_PROCESS);

        EmulatorCaptureInfo ldPlayerInfo = matchLdplayerProcess(port, ldplayerProcesses);
        if (ldPlayerInfo != null) {
            logger.print("LDPlayer process matched for port " + port + " with PID: " + ldPlayerInfo.getPid());
            return ldPlayerInfo;
        } else {
            logger.print("No LDPlayer process matched for port " + port);
        }

        EmulatorCaptureInfo bluestacksInfo = matchBluestacksProcess(port, bluestacksProcesses);
        if (bluestacksInfo != null) {
            logger.print("BlueStacks process matched for port " + port + " with PID: " + bluestacksInfo.getPid());
            return bluestacksInfo;
        } else {
            logger.print("No BlueStacks process matched for port " + port);
        }

        return null;
    }

    /**
     * Retrieves process handles for a given process name.
     *
     * @param processName The name of the process executable.
     * @return List of matching ProcessHandle instances.
     */
    private List<ProcessHandle> getProcessHandles(String processName) {
        return ProcessHandle.allProcesses()
                .filter(ph -> ph.info().command().isPresent() && ph.info().command().get().endsWith(processName))
                .collect(Collectors.toList());
    }

    /**
     * Matches LDPlayer process based on port.
     *
     * @param port             The port number.
     * @param ldplayerProcesses List of LDPlayer ProcessHandle instances.
     * @return EmulatorCaptureInfo if matched, null otherwise.
     */
    private EmulatorCaptureInfo matchLdplayerProcess(int port, List<ProcessHandle> ldplayerProcesses) {
        logger.print("Attempting to match LDPlayer process for port: " + port);

        if (ldplayerProcesses.isEmpty()) {
            logger.print("No LDPlayer processes found.");
            return null;
        }

        String ldplayerPath = findLdplayerPath();
        if (ldplayerPath == null) {
            logger.print("LDPlayer path not found.");
            return null;
        }
        logger.print("LDPlayer path: " + ldplayerPath);

        String basePath = ldplayerPath.substring(0, ldplayerPath.lastIndexOf(DNCONSOLE_EXE));
        File configDir = new File(basePath + "vms\\config");
        if (!configDir.exists() || !configDir.isDirectory()) {
            logger.print("Config directory does not exist or is not a directory: " + configDir.getPath());
            return null;
        }
        logger.print("Config directory located: " + configDir.getPath());

        File[] configFiles = configDir.listFiles((dir, name) -> name.matches(LDPLAYER_CONFIG_REGEX));
        if (configFiles == null || configFiles.length == 0) {
            logger.print("No matching LDPlayer configuration files found.");
            return null;
        }
        logger.print("Found " + configFiles.length + " config files matching regex.");

        Arrays.sort(configFiles, Comparator.comparingInt(file -> {
            try {
                return Integer.parseInt(file.getName().replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                logger.print("Invalid config file name: " + file.getName());
                return -1; // Place invalid files first
            }
        }));

        int targetConfigNumber = (port - 5554) / 2;
        logger.print("Target config number for port " + port + ": " + targetConfigNumber);

        File matchingConfigFile = null;
        for (File configFile : configFiles) {
            int configFileNumber;
            try {
                configFileNumber = Integer.parseInt(configFile.getName().replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                logger.print("Invalid config file name: " + configFile.getName());
                continue;
            }
            logger.print("Checking config file: " + configFile.getName() + " with number: " + configFileNumber);

            if (configFileNumber == targetConfigNumber) {
                matchingConfigFile = configFile;
                logger.print("Matching config file found: " + configFile.getName());
                break;
            }
        }

        if (matchingConfigFile != null) {
            String playerName = extractPlayerName(matchingConfigFile);
            if (playerName == null && matchingConfigFile.getName().equals("leidian0.config")) {
                playerName = "LDPlayer";
                logger.print("Defaulting player name to 'LDPlayer' for config file: " + matchingConfigFile.getName());
            }

            if (playerName != null) {
                logger.print("Extracted player name: " + playerName);
                return matchPlayerNameWithProcesses(playerName, ldplayerProcesses, port, "LDPlayer");
            } else {
                logger.print("No player name found in config file: " + matchingConfigFile.getName());
            }
        } else {
            logger.print("Port: " + port + " does not correspond to any config file index.");
        }

        return null;
    }

    /**
     * Extracts player name from config file.
     *
     * @param configFile The configuration file.
     * @return The player name, or null if not found.
     */
    private String extractPlayerName(File configFile) {
        logger.print("Extracting player name from config file: " + configFile.getName());
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile))) {
            String playerName = bufferedReader.lines()
                    .filter(line -> line.contains("\"statusSettings.playerName\""))
                    .map(line -> {
                        int startIndex = line.indexOf(":") + 1;
                        String extractedName = line.substring(startIndex).trim();
                        if (extractedName.endsWith("\",")) {
                            extractedName = extractedName.substring(1, extractedName.length() - 2);
                        } else {
                            extractedName = extractedName.substring(1, extractedName.length() - 1);
                        }
                        return extractedName.replace("\\\"", "\"");
                    })
                    .findFirst()
                    .orElse(null);

            logger.print("Extracted player name: " + (playerName != null ? playerName : "None found"));
            return playerName;
        } catch (Exception e) {
            logger.print("Error reading config file " + configFile.getName() + ": " + e);
            return null;
        }
    }

    /**
     * Matches player name with processes to find the correct PID.
     *
     * @param playerName       The player name extracted from config.
     * @param ldplayerProcesses List of LDPlayer ProcessHandle instances.
     * @param port             The port number.
     * @param emulatorUsed     The emulator name ("LDPlayer").
     * @return EmulatorCaptureInfo if matched, null otherwise.
     */
    private EmulatorCaptureInfo matchPlayerNameWithProcesses(String playerName, List<ProcessHandle> ldplayerProcesses, int port, String emulatorUsed) {
        logger.print("Matching player name with processes for port " + port + ": " + playerName);

        Optional<ProcessHandle> matchedProcess = null;

        try {
            matchedProcess = CompletableFuture.supplyAsync(() -> {
                // Sequential stream processing
                for (ProcessHandle ph : ldplayerProcesses) {
                    String windowTitle = getWindowTitleCachedOrThrottled(ph.pid());
                    if (windowTitle.contains(playerName)) {
                        return Optional.of(ph);
                    }
                }
                return Optional.<ProcessHandle>empty();
            }, executor).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restore interrupt status
        }

        if (matchedProcess.isPresent()) {
            EmulatorCaptureInfo captureInfo = new EmulatorCaptureInfo((int) matchedProcess.get().pid(), emulatorUsed);
            logger.print("Match found: PID " + matchedProcess.get().pid() + ", Player Name: " + playerName);
            cacheManager.setEmulatorCaptureInfo("localhost:" + port, captureInfo);
            return captureInfo;
        }

        logger.print("No matching process found for port: " + port + " with player name: " + playerName);
        return null;
    }

    /**
     * Matches BlueStacks process based on port.
     *
     * @param port               The port number.
     * @param bluestacksProcesses List of BlueStacks ProcessHandle instances.
     * @return EmulatorCaptureInfo if matched, null otherwise.
     */
    private EmulatorCaptureInfo matchBluestacksProcess(int port, List<ProcessHandle> bluestacksProcesses) {
        if (bluestacksProcesses.isEmpty()) {
            return null;
        }

        for (ProcessHandle ph : bluestacksProcesses) {
            long pid = ph.pid();
            if (isProcessUsingPort(pid, port)) {
                EmulatorCaptureInfo captureInfo = new EmulatorCaptureInfo((int) pid, "BlueStacks");
                cacheManager.setEmulatorCaptureInfo("localhost:" + port, captureInfo);
                return captureInfo;
            }
        }

        return null;
    }

    /**
     * Determines if a process is using a specific port.
     *
     * @param pid  The process ID.
     * @param port The port number.
     * @return True if the process is using the port, false otherwise.
     */
    private boolean isProcessUsingPort(long pid, int port) {
        String command = "netstat -aon | findstr :" + port + " | findstr " + pid;
        String output = executeCommand(command);
        return output != null && !output.isEmpty();
    }

    /**
     * Executes a system command and returns the output.
     *
     * @param command The command to execute.
     * @return The output of the command, or null if failed.
     */
    private String executeCommand(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("cmd.exe", "/c", command).start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining(System.lineSeparator()));
            process.waitFor();
            return output;
        } catch (Exception e) {
            logger.print("Error executing command '" + command + "': " + e.getMessage());
            return null;
        } finally {
            if (process != null) {
                process.destroy();
                try {
                    if (!process.waitFor(1, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException ie) {
                    process.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Extracts port number from device identifier.
     *
     * @param device The device identifier (e.g., "emulator-5556").
     * @return The port number as a string, or null if extraction fails.
     */
    public String extractPort(String device) {
        if (device.startsWith("emulator-")) {
            return device.replace("emulator-", "");
        } else if (device.startsWith("localhost:") || device.startsWith("127.0.0.1:")) {
            return device.split(":")[1];
        }
        return null;
    }

    public boolean isWindowMinimized(HWND hwnd) {
        int windowState = User32Mufasa.INSTANCE.IsIconic(hwnd);
        return windowState != 0; // Returns non-zero if minimized
    }

    public Dimension getWindowSize(HWND hwnd) {
        RECT rect = new RECT();
        if (User32Mufasa.INSTANCE.GetWindowRect(hwnd, rect)) {
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;
            return new Dimension(width, height);
        } else {
            logger.print("Failed to get window size for HWND: " + hwnd);
            return null;
        }
    }

    public void checkWindowState(HWND hwnd) {
        // Minimize check part
        boolean minimized = isWindowMinimized(hwnd);
        if (minimized) {
            logger.print("Window is minimized; restoring it.");
            User32Mufasa.INSTANCE.ShowWindow(hwnd, User32Mufasa.SW_RESTORE); // Restore the window if minimized
        }

        minimized = isWindowMinimized(hwnd);
        logger.print("Window minimized: " + minimized);

        // Window size check part
        //Dimension size = getWindowSize(hwnd);
        //if (size != null) {
        //    // Desired dimensions
        //    int targetWidth = 894;
        //    int targetHeight = 540;

        //    // Resize if not already at the target dimensions
        //    if (size.width != targetWidth || size.height != targetHeight) {
        //        logger.print("Window is not 894x540 (" + size.width + "x" + size.height + ")! Cannot properly run a script like this. Please setup your emulator properly with a " + targetWidth + "x" + targetHeight + " resolution.");
        //    }
        //} else {
        //    logger.print("Could not retrieve window size.");
        //}
    }
}
