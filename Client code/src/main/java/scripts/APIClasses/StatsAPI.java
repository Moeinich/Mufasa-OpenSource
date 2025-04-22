package scripts.APIClasses;

import helpers.utils.Skills;
import interfaces.iStats;
import osr.mapping.Stats;
import scripts.ScriptInfo;

public class StatsAPI implements iStats {
    private final Stats stats;
    private final ScriptInfo scriptInfo;

    public StatsAPI(Stats stats, ScriptInfo scriptInfo) {
        this.stats = stats;
        this.scriptInfo = scriptInfo;
    }

    public Integer getRealLevel(Skills skill) {
        return stats.getRealLevelCF(skill, scriptInfo.getCurrentEmulatorId());
    }

    public Integer getEffectiveLevel(Skills skill) {
        return stats.getEffectiveLevelCF(skill, scriptInfo.getCurrentEmulatorId());
    }

    public Integer getTotalLevel() {
        return stats.getTotalLevelCF(scriptInfo.getCurrentEmulatorId());
    }
}
