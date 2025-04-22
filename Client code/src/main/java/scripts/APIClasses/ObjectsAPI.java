package scripts.APIClasses;

import interfaces.iObjects;
import osr.mapping.Objects;
import scripts.ScriptInfo;

import java.awt.*;
import java.util.List;

public class ObjectsAPI implements iObjects {
    private final Objects objects;

    private final ScriptInfo scriptInfo;

    public ObjectsAPI(Objects objects, ScriptInfo scriptInfo) {
        this.objects = objects;
        this.scriptInfo = scriptInfo;
    }

    public List<Rectangle> within(int tileRadius, boolean returnAll, String filePath) {
        return objects.within(scriptInfo.getCurrentEmulatorId(), tileRadius, returnAll, filePath);
    }

    public java.util.List<Rectangle> within(Rectangle rect, boolean returnAll, String filePath) {
        return objects.within(scriptInfo.getCurrentEmulatorId(), rect, returnAll, filePath);
    }

    public Rectangle getNearest(String filePath) {
        return objects.getNearest(scriptInfo.getCurrentEmulatorId(), filePath);
    }

    public Rectangle getBestMatch(String filePath, double threshold) {
        return objects.getBestMatch(scriptInfo.getCurrentEmulatorId(), filePath, threshold);
    }
}
