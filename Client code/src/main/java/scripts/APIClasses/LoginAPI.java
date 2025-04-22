package scripts.APIClasses;

import interfaces.iLogin;
import osr.mapping.Login;
import scripts.ScriptInfo;

import java.awt.*;

import static osr.mapping.Login.TAP_HERE_TO_PLAY_CHECK_RECT;

public class LoginAPI implements iLogin {
    private final Login login;
    private final ScriptInfo scriptInfo;

    public LoginAPI(Login login, ScriptInfo scriptInfo) {
        this.login = login;
        this.scriptInfo = scriptInfo;
    }

    public Rectangle findPlayNowOption() {
        return login.findPlayNowOption(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findTapToPlayOption() {
        return TAP_HERE_TO_PLAY_CHECK_RECT;
    }

    public void login() {
        login.login(scriptInfo.getCurrentEmulatorId());
    }

    public void preSetup(boolean skipZoom) {
        login.preSetup(scriptInfo.getCurrentEmulatorId(), skipZoom);
    }
}
