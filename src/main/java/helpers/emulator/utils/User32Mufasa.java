package helpers.emulator.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.UINT;

public interface User32Mufasa extends Library {
    User32Mufasa INSTANCE = Native.load("user32", User32Mufasa.class);

    // Check if a window is minimized
    int IsIconic(HWND hWnd);

    // Retrieve the window rectangle (position and size)
    boolean GetWindowRect(HWND hWnd, RECT rect);

    // Set the window position and size
    boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, UINT uFlags);
    boolean ShowWindow(HWND hWnd, int nCmdShow);

    HWND HWND_TOP = new HWND(Pointer.createConstant(0)); // Keep window in its current Z order.
    int SWP_NOZORDER = 0x0004;
    int SWP_NOMOVE = 0x0002;
    int SW_RESTORE = 9;
}
