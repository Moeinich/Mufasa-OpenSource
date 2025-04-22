package scripts;

import helpers.scripts.utils.Script;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;

public class ScriptResourceManager {
    private final ScriptInfo scriptInfo;

    public ScriptResourceManager(ScriptInfo scriptInfo) {
        this.scriptInfo = scriptInfo;
    }

    public byte[] getResourceFromCurrentScript(String deviceID, String resourcePath) {
        Script script = scriptInfo.getCurrentScript(deviceID);
        if (script != null) {
            return script.getResource(resourcePath);
        }
        return null; // or throw an exception if the script or resource is not found
    }

    public Image getResourceImageFromCurrentScript(String deviceID, String resourcePath) {
        Script script = scriptInfo.getCurrentScript(deviceID);
        if (script != null) {
            byte[] resourceData = script.getResource(resourcePath);
            if (resourceData != null) {
                // Convert the byte array to a JavaFX Image
                try (ByteArrayInputStream bais = new ByteArrayInputStream(resourceData)) {
                    return new Image(bais);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
