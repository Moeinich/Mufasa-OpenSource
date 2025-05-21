package scripts.APIClasses;

import helpers.OCR.utils.FontName;
import interfaces.iOCR;
import osr.mapping.GameOCR;
import scripts.ScriptInfo;

import java.util.List;

public class OcrAPI implements iOCR {
    private final ScriptInfo scriptInfo;
    private final GameOCR gameOCR;

    public OcrAPI(ScriptInfo scriptInfo, GameOCR gameOCR) {
        this.scriptInfo = scriptInfo;
        this.gameOCR = gameOCR;
    }

    public String readAnyText(java.awt.Rectangle areaToRead, List<java.awt.Color> colors) {
        return gameOCR.readText(scriptInfo.getCurrentEmulatorId(), areaToRead, FontName.ANY, colors);
    }

    public String readBold12Text(java.awt.Rectangle areaToRead, List<java.awt.Color> colors) {
        return gameOCR.readText(scriptInfo.getCurrentEmulatorId(), areaToRead, FontName.BOLD_12, colors);
    }

    public String readPlain11Text(java.awt.Rectangle areaToRead, List<java.awt.Color> colors) {
        return gameOCR.readText(scriptInfo.getCurrentEmulatorId(), areaToRead, FontName.PLAIN_11, colors);
    }

    public String readPlain12Text(java.awt.Rectangle areaToRead, List<java.awt.Color> colors) {
        return gameOCR.readText(scriptInfo.getCurrentEmulatorId(), areaToRead, FontName.PLAIN_12, colors);
    }

    public String readQuillText(java.awt.Rectangle areaToRead, List<java.awt.Color> colors) {
        return gameOCR.readText(scriptInfo.getCurrentEmulatorId(), areaToRead, FontName.QUILL, colors);
    }

    public String readQuill8Text(java.awt.Rectangle areaToRead, List<java.awt.Color> colors) {
        return gameOCR.readText(scriptInfo.getCurrentEmulatorId(), areaToRead, FontName.QUILL_8, colors);
    }

    // Other OCR methods
    public String readLastLine(java.awt.Rectangle areaToRead) {
        return gameOCR.readChatboxArea(scriptInfo.getCurrentEmulatorId(), areaToRead);
    }
    public int readDigitsInArea(java.awt.Rectangle areaToOCR, java.util.List<java.awt.Color> colorsToScan) {
        return gameOCR.readDigitsInArea(areaToOCR, colorsToScan, scriptInfo.getCurrentEmulatorId());
    }
}
