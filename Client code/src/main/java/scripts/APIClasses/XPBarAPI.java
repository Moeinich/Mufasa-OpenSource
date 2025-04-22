package scripts.APIClasses;

import interfaces.iXPBar;
import osr.mapping.XPBar;
import scripts.ScriptInfo;

public class XPBarAPI implements iXPBar {
    private final XPBar xpBar;
    private final ScriptInfo scriptInfo;

    public XPBarAPI(XPBar xpBar, ScriptInfo scriptInfo) {
        this.xpBar = xpBar;
        this.scriptInfo = scriptInfo;
    }

    public int getXP() {
        return xpBar.readXP(scriptInfo.getCurrentEmulatorId());
    }
}
