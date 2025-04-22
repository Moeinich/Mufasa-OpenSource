package scripts.APIClasses;

import helpers.utils.Spells;
import interfaces.iMagic;
import osr.mapping.Magic;
import scripts.ScriptInfo;

public class MagicAPI implements iMagic {
    private final Magic magic;
    private final ScriptInfo scriptInfo;

    public MagicAPI(Magic magic, ScriptInfo scriptInfo) {
        this.magic = magic;
        this.scriptInfo = scriptInfo;
    }

    public void castSpell(Spells spell) {
        magic.castSpell(spell, scriptInfo.getCurrentEmulatorId());
    }

    public boolean isCastable(Spells spell) {
        return magic.isCastable(spell, scriptInfo.getCurrentEmulatorId());
    }

    public boolean isInfoEnabled() {
        return magic.isInfoEnabled(scriptInfo.getCurrentEmulatorId());
    }
}
