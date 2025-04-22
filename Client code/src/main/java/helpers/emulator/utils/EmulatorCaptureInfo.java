package helpers.emulator.utils;

public class EmulatorCaptureInfo {
    private final int pid;
    private String emulatorUsed;

    public EmulatorCaptureInfo(int pid, String emulatorUsed) {
        this.pid = pid;
        this.emulatorUsed = emulatorUsed;
    }

    public void setEmulatorUsed(String emulatorUsed) {
        this.emulatorUsed = emulatorUsed;
    }

    public int getPid() {
        return pid;
    }

    public String getEmulatorUsed() {
        return emulatorUsed;
    }

    @Override
    public String toString() {
        System.out.println("EmulatorCaptureInfo{" + "pid=" + pid + ", emulatorUsed='" + emulatorUsed + '\'' + '}');
        return "EmulatorCaptureInfo{" +
                "pid=" + pid +
                ", emulatorUsed='" + emulatorUsed + '\'' +
                '}';
    }
}
