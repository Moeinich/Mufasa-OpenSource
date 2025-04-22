package scripts.APIClasses;

import interfaces.iPrayer;
import osr.mapping.Prayer;
import scripts.ScriptInfo;

public class PrayerAPI implements iPrayer {
    private final Prayer prayer;
    private final ScriptInfo scriptInfo;

    public PrayerAPI(Prayer prayer, ScriptInfo scriptInfo) {
        this.prayer = prayer;
        this.scriptInfo = scriptInfo;
    }

    // Activate methods
    public void activateAugury() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "augury.png");
    }

    public void activateBurstofStrength() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "burstofstrength.png");
    }

    public void activateChivalry() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "chivalry.png");
    }

    public void activateClarityofThought() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "clarityofthought.png");
    }

    public void activateEagleEye() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "eagleeye.png");
    }

    public void activateHawkEye() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "hawkeye.png");
    }

    public void activateImprovedReflexes() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "improvedreflexes.png");
    }

    public void activateIncredibleReflexes() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "incrediblereflexes.png");
    }

    public void activateMysticLore() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "mysticlore.png");
    }

    public void activateMysticMight() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "mysticmight.png");
    }

    public void activateMysticWill() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "mysticwill.png");
    }

    public void activatePiety() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "piety.png");
    }

    public void activatePreserve() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "preserve.png");
    }

    public void activateProtectfromMagic() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "protectfrommagic.png");
    }

    public void activateProtectfromMelee() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "protectfrommelee.png");
    }

    public void activateProtectfromMissiles() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "protectfrommissiles.png");
    }

    public void activateProtectItem() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "protectitem.png");
    }

    public void activateRapidHeal() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "rapidheal.png");
    }

    public void activateRapidRestore() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "rapidrestore.png");
    }

    public void activateRedemption() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "redemption.png");
    }

    public void activateRetribution() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "retribution.png");
    }

    public void activateRigour() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "rigour.png");
    }

    public void activateRockSkin() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "rockskin.png");
    }

    public void activateSharpEye() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "sharpeye.png");
    }

    public void activateSmite() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "smite.png");
    }

    public void activateSteelSkin() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "steelskin.png");
    }

    public void activateSuperhumanStrength() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "superhumanstrength.png");
    }

    public void activateThickSkin() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "thickskin.png");
    }

    public void activateUltimateStrength() {
        prayer.activatePrayer(scriptInfo.getCurrentEmulatorId(), "ultimatestrength.png");
    }

    // isActive methods
    public boolean isActiveAugury() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_augury.png");
    }

    public boolean isActiveBurstofStrength() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_burstofstrength.png");
    }

    public boolean isActiveChivalry() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_chivalry.png");
    }

    public boolean isActiveClarityofThought() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_clarityofthought.png");
    }

    public boolean isActiveEagleEye() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_eagleeye.png");
    }

    public boolean isActiveHawkEye() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_hawkeye.png");
    }

    public boolean isActiveImprovedReflexes() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_improvedreflexes.png");
    }

    public boolean isActiveIncredibleReflexes() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_incrediblereflexes.png");
    }

    public boolean isActiveMysticLore() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_mysticlore.png");
    }

    public boolean isActiveMysticMight() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_mysticmight.png");
    }

    public boolean isActiveMysticWill() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_mysticwill.png");
    }

    public boolean isActivePiety() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_piety.png");
    }

    public boolean isActivePreserve() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_preserve.png");
    }

    public boolean isActiveProtectfromMagic() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_protectfrommagic.png");
    }

    public boolean isActiveProtectfromMelee() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_protectfrommelee.png");
    }

    public boolean isActiveProtectfromMissiles() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_protectfrommissiles.png");
    }

    public boolean isActiveProtectItem() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_protectitem.png");
    }

    public boolean isActiveRapidHeal() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_rapidheal.png");
    }

    public boolean isActiveRapidRestore() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_rapidrestore.png");
    }

    public boolean isActiveRedemption() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_redemption.png");
    }

    public boolean isActiveRetribution() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_retribution.png");
    }

    public boolean isActiveRigour() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_rigour.png");
    }

    public boolean isActiveRockSkin() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_rockskin.png");
    }

    public boolean isActiveSharpEye() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_sharpeye.png");
    }

    public boolean isActiveSmite() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_smite.png");
    }

    public boolean isActiveSteelSkin() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_steelskin.png");
    }

    public boolean isActiveSuperhumanStrength() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_superhumanstrength.png");
    }

    public boolean isActiveThickSkin() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_thickskin.png");
    }

    public boolean isActiveUltimateStrength() {
        return prayer.isPrayerActive(scriptInfo.getCurrentEmulatorId(), "activated_ultimatestrength.png");
    }

}
