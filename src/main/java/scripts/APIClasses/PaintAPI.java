package scripts.APIClasses;

import UI.components.PaintBar;
import helpers.annotations.ScriptManifest;
import interfaces.iPaint;
import javafx.scene.image.Image;
import scripts.ScriptInfo;

import osr.utils.ImageUtils;

public class PaintAPI implements iPaint {
    private final PaintBar paintBar;
    private final ScriptInfo scriptInfo;
    private final ImageUtils imageUtils;

    public PaintAPI(PaintBar paintBar, ScriptInfo scriptInfo, ImageUtils imageUtils) {
        this.paintBar = paintBar;
        this.scriptInfo = scriptInfo;
        this.imageUtils = imageUtils;
    }

    public void Create(String optionalImagePath) {
        Image image = null;

        if (optionalImagePath != null) {
            image = imageUtils.pathToJavaFXImage(optionalImagePath);
        }

        ScriptManifest manifest = scriptInfo.getScriptManifest(scriptInfo.getCurrentEmulatorId());
        if (manifest != null) {
            paintBar.createInstance(scriptInfo.getCurrentEmulatorId(), manifest.name(), image);
        }
    }

    public int createBox(String labeltext, int itemID, int integer) {
        PaintBar paintBarInstance = paintBar.getInstance(scriptInfo.getCurrentEmulatorId());
        if (paintBarInstance != null) {
            int index = paintBarInstance.getFirstAvailableBoxIndex();
            paintBarInstance.setBoxLabelText(index, labeltext);
            paintBarInstance.setBoxImage(index, itemID);
            paintBarInstance.setBoxInteger(index, integer);
            return index;
        }
        return -1;
    }

    public void updateBox(int index, int number) {
        PaintBar paintBarInstance = paintBar.getInstance(scriptInfo.getCurrentEmulatorId());
        if (paintBarInstance != null) {
            paintBarInstance.setBoxInteger(index, number);
        }
    }

    public void setStatus(String text) {
        PaintBar paintBarInstance = paintBar.getInstance(scriptInfo.getCurrentEmulatorId());
        if (paintBarInstance != null) {
            paintBarInstance.enableActionLabel();
            paintBarInstance.setActionLabelText(text);
        }
    }

    public void setStatistic(String text) {
        PaintBar paintBarInstance = paintBar.getInstance(scriptInfo.getCurrentEmulatorId());
        if (paintBarInstance != null) {
            paintBarInstance.enableStatisticLabel();
            paintBarInstance.setStatisticLabelText(text);
        }
    }
}
