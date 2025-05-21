package scripts.APIClasses;

import interfaces.iLogout;
import osr.mapping.Logout;
import scripts.ScriptInfo;

import java.awt.*;

public class LogoutAPI implements iLogout {
    private final Logout logout;
    private final ScriptInfo scriptInfo;

    public LogoutAPI(Logout logout, ScriptInfo scriptInfo) {
        this.logout = logout;
        this.scriptInfo = scriptInfo;
    }

    public Rectangle findLogoutOption() {
        return logout.findLogoutOption(scriptInfo.getCurrentEmulatorId());
    }

    public void logout() {
        logout.logout(scriptInfo.getCurrentEmulatorId(), false);
    }

    public Boolean isLoggedOut() {
        return logout.isLoggedOut(scriptInfo.getCurrentEmulatorId());
    }
}
