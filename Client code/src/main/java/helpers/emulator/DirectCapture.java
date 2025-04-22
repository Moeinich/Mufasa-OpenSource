package helpers.emulator;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.ptr.IntByReference;
import helpers.CacheManager;
import helpers.Logger;
import helpers.emulator.utils.DirectCaptureUtils;
import helpers.emulator.utils.EmulatorCaptureInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static UI.components.utils.Observables.USE_PW_CAPTURE;

public class DirectCapture {
    private final Logger logger;
    private final CacheManager cacheManager;
    private final DirectCaptureUtils directCaptureUtils;
    private final User32 user32;
    private final Map<HWND, CachedWindowCapture> cache = new ConcurrentHashMap<>();

    public DirectCapture(Logger logger, CacheManager cacheManager) {
        this.logger = logger;
        this.cacheManager = cacheManager;
        this.directCaptureUtils = new DirectCaptureUtils(cacheManager, logger);

        this.user32 = System.getProperty("os.name").toLowerCase().contains("windows") ? User32.INSTANCE : null;
    }

    /**
     * Takes a screenshot of the specified device.
     *
     * @param device The device identifier.
     * @return A BufferedImage representing the screenshot, or null if failed.
     */
    public BufferedImage takeScreenshot(String device) {
        // Validate device identifier
        if (device == null || device.isEmpty()) {
            logger.print("Invalid device identifier.");
            return null;
        }

        // Attempt to retrieve cached HWND first
        HWND cachedHwnd = cacheManager.getHWNDForDevice(device);
        if (cachedHwnd != null && user32.IsWindow(cachedHwnd)) {

            BufferedImage fullImage;
            if (USE_PW_CAPTURE.get()) {
                fullImage = captureWindowUsingPrintWindow(cachedHwnd);
            } else {
                fullImage = captureWindow(cachedHwnd);
            }

            return checkImage(fullImage, device);
        }

        // If no valid cached HWND, proceed with fetching EmulatorCaptureInfo
        EmulatorCaptureInfo captureInfo = directCaptureUtils.getEmulatorCaptureInfo(device);
        if (captureInfo == null) {
            logger.print("No capture info found for device: " + device);
            cacheManager.removeEmulatorCaptureInfo(device);
            return null;
        }

        // Retrieve HWND using device identifier and capture info
        HWND hwnd = getCachedOrFindWindow(captureInfo);
        if (hwnd == null) {
            logger.print("No window found for PID: " + captureInfo.getPid());
            cacheManager.removeHWNDForDevice(device);
            return null;
        }

        // Cache the HWND for future use
        cacheManager.setHWNDForDevice(device, hwnd);
        logger.print("Cached HWND for device " + device + ": " + hwnd);

        // Capture and process the image
        BufferedImage fullImage = captureWindow(hwnd);
        return checkImage(fullImage, device);
    }

    /**
     * Retrieves the cached HWND or finds a new one based on EmulatorCaptureInfo.
     *
     * @param captureInfo The EmulatorCaptureInfo object.
     * @return The HWND handle, or null if not found.
     */
    private HWND getCachedOrFindWindow(EmulatorCaptureInfo captureInfo) {
        // Attempt to retrieve cached HWND based on PID
        HWND cachedHwnd = cacheManager.getWindowHandle(captureInfo.getPid());
        if (cachedHwnd != null && User32.INSTANCE.IsWindow(cachedHwnd)) {
            return cachedHwnd;
        }

        // If not cached or invalid, find a new HWND
        HWND hwnd = findWindowByCaptureInfo(captureInfo);
        if (hwnd != null) {
            cacheManager.setWindowHandle(captureInfo.getPid(), hwnd);
            logger.print("Mapped new window handle for PID " + captureInfo.getPid() + ": " + hwnd);
        }
        return hwnd;
    }

    /**
     * Processes the captured image by resizing if necessary.
     *
     * @param fullImage The original captured image.
     * @return The processed BufferedImage.
     */
    private BufferedImage checkImage(BufferedImage fullImage, String device) {
        if (fullImage != null) {
            // Check if the image is already the target size
            if (fullImage.getWidth() == 894 && fullImage.getHeight() == 540) {
                return fullImage; // No alterations needed
            }

            // Else, proceed to check its dimensions
            return checkDimensions(fullImage, device);
        } else {
            logger.print("Captured image is null.");
            return null;
        }
    }

    /**
     * Scales the image to the target dimensions using Graphics2D.
     *
     * @param src    The source BufferedImage.
     * @return The scaled BufferedImage.
     */
    private BufferedImage checkDimensions(BufferedImage src, String device) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        // Get screen resolution and calculate scaling percentage
        int screenResolution = toolkit.getScreenResolution();
        double scaling = screenResolution / 96.0;
        int scalingPercentage = (int) (scaling * 100);

        // Log the details
        logger.debugLog("Detected wrong window size: " + src.getWidth() + "," + src.getHeight() + " :: WinScaling: " + scalingPercentage, device);

        return src;
    }

    /**
     * Finds the window handle based on emulator capture information.
     *
     * @param captureInfo The emulator capture information.
     * @return The HWND window handle, or null if not found.
     */
    private HWND findWindowByCaptureInfo(EmulatorCaptureInfo captureInfo) {
        if ("LDPlayer".equalsIgnoreCase(captureInfo.getEmulatorUsed())) {
            return findLDWindowByPID(captureInfo.getPid());
        } else if ("BlueStacks".equalsIgnoreCase(captureInfo.getEmulatorUsed())) {
            return findBluestacksWindow(captureInfo.getPid());
        }
        return null;
    }

    /**
     * Finds the LDPlayer window handle by PID.
     *
     * @param pid The process ID.
     * @return The HWND window handle, or null if not found.
     */
    private HWND findLDWindowByPID(int pid) {
        final HWND[] foundWindow = {null};

        // Step 1: Find the main window with class "LDPlayerMainFrame"
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            IntByReference windowPID = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPID);

            if (windowPID.getValue() == pid) {
                char[] classNameBuffer = new char[1024];
                User32.INSTANCE.GetClassName(hwnd, classNameBuffer, 1024);

                String windowClassName = Native.toString(classNameBuffer);

                if (windowClassName.equals("LDPlayerMainFrame")) {
                    foundWindow[0] = hwnd;
                    return false; // Stop enumeration
                }
            }
            return true; // Continue enumeration
        }, null);

        // If main window is found, search for child windows
        if (foundWindow[0] != null) {
            HWND mainWindow = foundWindow[0];

            // Step 2: Search for subWin as a child of LDPlayerMainFrame
            HWND subWin = findLDChildWindow(mainWindow, "subWin");
            if (subWin != null) {
                logger.print("Found subWin");
                return subWin; // Return early if subWin is found
            }

            // Step 3: If subWin is not found, search for RenderWindow
            HWND renderWindow = findLDChildWindow(mainWindow, "RenderWindow");
            if (renderWindow != null) {
                logger.print("Found RenderWindow");
                return renderWindow; // Return early if RenderWindow is found
            }
        }

        // Fallback: Use the original method to find any window by PID
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            IntByReference windowPID = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPID);

            if (windowPID.getValue() == pid) {
                foundWindow[0] = hwnd;
                return false; // Stop enumeration
            }
            return true; // Continue enumeration
        }, null);

        return foundWindow[0];
    }

    /**
     * Finds the LD Player child window.
     *
     * @param parent          The parent window handle.
     * @param targetClassName The target child window class name.
     * @return The child HWND window handle, or null if not found.
     */
    private HWND findLDChildWindow(HWND parent, String targetClassName) {
        final HWND[] childWindow = {null};

        User32.INSTANCE.EnumChildWindows(parent, (hwnd, data) -> {
            char[] classNameBuffer = new char[1024];
            User32.INSTANCE.GetClassName(hwnd, classNameBuffer, 1024);

            String windowClassName = Native.toString(classNameBuffer);

            if (windowClassName.equals(targetClassName)) {
                childWindow[0] = hwnd;
                return false; // Stop enumeration
            }
            return true; // Continue enumeration
        }, null);

        return childWindow[0];
    }

    /**
     * Finds the BlueStacks window handle by PID.
     *
     * @param pid The process ID.
     * @return The HWND window handle, or null if not found.
     */
    public HWND findBluestacksWindow(int pid) {
        final HWND[] foundWindow = {null};

        // Step 1: Find the main window with class "Qt5154QWindowOwnDCIcon"
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            IntByReference windowPID = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPID);

            if (windowPID.getValue() == pid) {
                char[] classNameBuffer = new char[1024];
                User32.INSTANCE.GetClassName(hwnd, classNameBuffer, 1024);

                String windowClassName = Native.toString(classNameBuffer);

                if (windowClassName.equals("Qt5154QWindowOwnDCIcon")) {
                    foundWindow[0] = hwnd;
                    return false; // Stop enumeration
                }
            }
            return true; // Continue enumeration
        }, null);

        // If the main window is found, search for child windows
        if (foundWindow[0] != null) {
            HWND mainWindow = foundWindow[0];

            // Step 2: Search for BlueStacksApp as a child of Qt5154QWindowOwnDCIcon
            HWND blueStacksApp = findBSChildWindow(mainWindow, "BlueStacksApp");
            if (blueStacksApp != null) {
                return blueStacksApp; // Return early if BlueStacksApp is found
            }

            // Step 3: If BlueStacksApp is not found, search for Qt5154QWindowIcon
            return findBSChildWindow(mainWindow, "Qt5154QWindowIcon"); // Return early if Qt5154QWindowIcon is found
        }

        return null; // Return null if no desired window is found
    }

    /**
     * Helper method to find BlueStacks child windows with a specific class name.
     *
     * @param parent          The parent window handle.
     * @param targetClassName The target child window class name.
     * @return The child HWND window handle, or null if not found.
     */
    private HWND findBSChildWindow(HWND parent, String targetClassName) {
        final HWND[] childWindow = {null};

        User32.INSTANCE.EnumChildWindows(parent, (hwnd, data) -> {
            char[] classNameBuffer = new char[1024];
            User32.INSTANCE.GetClassName(hwnd, classNameBuffer, 1024);

            String windowClassName = Native.toString(classNameBuffer);

            if (windowClassName.equals(targetClassName)) {
                childWindow[0] = hwnd;
                return false; // Stop enumeration
            }
            return true; // Continue enumeration
        }, null);

        return childWindow[0];
    }

    public BufferedImage captureWindow(HWND hWnd) {
        WinDef.RECT bounds = new WinDef.RECT();
        User32.INSTANCE.GetClientRect(hWnd, bounds);
        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;

        CachedWindowCapture cachedCapture = cache.get(hWnd);
        if (cachedCapture == null) {
            cachedCapture = new CachedWindowCapture(width, height, hWnd);
            cache.put(hWnd, cachedCapture);
        }

        // Use cached HDC and HBITMAP
        GDI32.INSTANCE.BitBlt(cachedCapture.hdcMemDC, 0, 0, width, height, cachedCapture.hdcWindow, 0, 0, 0x00CC0020); // SRCCOPY

        // Retrieve bitmap data
        GDI32.INSTANCE.GetDIBits(cachedCapture.hdcWindow, cachedCapture.hBitmap, 0, height,
                cachedCapture.buffer, cachedCapture.bmi, WinGDI.DIB_RGB_COLORS);

        // Create and return BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, cachedCapture.buffer.getIntArray(0, width * height), 0, width);

        return image;
    }

    public BufferedImage captureWindowUsingPrintWindow(HWND hWnd) {
        WinDef.RECT bounds = new WinDef.RECT();
        User32.INSTANCE.GetClientRect(hWnd, bounds);

        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;

        CachedWindowCapture cachedCapture = cache.get(hWnd);
        if (cachedCapture == null) {
            cachedCapture = new CachedWindowCapture(width, height, hWnd);
            cache.put(hWnd, cachedCapture);
        }

        // Use cached HDC and HBITMAP
        boolean success = User32.INSTANCE.PrintWindow(hWnd, cachedCapture.hdcMemDC, 0x00000002); //PW_RENDERFULLCONTENT
        if (!success) {
            System.err.println("PrintWindow failed.");
            return null;
        }

        // Retrieve bitmap data
        GDI32.INSTANCE.GetDIBits(cachedCapture.hdcWindow, cachedCapture.hBitmap, 0, height,
                cachedCapture.buffer, cachedCapture.bmi, WinGDI.DIB_RGB_COLORS);

        // Create an array to hold pixel data
        int[] pixels = cachedCapture.buffer.getIntArray(0, width * height);

        // Create a new array for adjusted pixels
        int[] adjustedPixels = new int[width * height]; // The width remains the same after adjustments

        // Fill the leftmost column with the specified color and shift the rows.
        int fillColor = 0xFF1B1817; // ARGB format for the color #1B1817

        for (int y = 0; y < height; y++) {
            // Fill the first pixel of the row
            adjustedPixels[y * width] = fillColor;

            // Copy pixels from the original row, skipping the last column
            System.arraycopy(pixels, y * width, adjustedPixels, y * width + 1, width - 1);
        }

        // Create the final BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, adjustedPixels, 0, width);
        return image;
    }

    private static class CachedWindowCapture {
        final int width, height;
        final WinDef.HDC hdcWindow, hdcMemDC;
        final WinDef.HBITMAP hBitmap;
        final Memory buffer;
        final WinGDI.BITMAPINFO bmi;

        CachedWindowCapture(int width, int height, HWND hWnd) {
            this.width = width;
            this.height = height;

            hdcWindow = User32.INSTANCE.GetDC(hWnd);
            hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
            hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);
            GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);

            buffer = new Memory((long) width * height * 4);

            bmi = new WinGDI.BITMAPINFO();
            bmi.bmiHeader.biWidth = width;
            bmi.bmiHeader.biHeight = -height; // Top-down bitmap
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = 32;
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
        }

        void cleanup() {
            GDI32.INSTANCE.DeleteObject(hBitmap);
            GDI32.INSTANCE.DeleteDC(hdcMemDC);
            User32.INSTANCE.ReleaseDC(null, hdcWindow);
        }
    }
}
