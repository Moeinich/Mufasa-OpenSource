package scripts.APIClasses;

import helpers.utils.SmithItems;
import interfaces.iInterfaces;
import osr.mapping.Interfaces;
import scripts.ScriptInfo;

import java.awt.*;
import java.util.Map;

public class InterfacesAPI implements iInterfaces {
    private final Interfaces interfaces;
    private final ScriptInfo scriptInfo;

    public InterfacesAPI(Interfaces interfaces, ScriptInfo scriptInfo) {
        this.interfaces = interfaces;
        this.scriptInfo = scriptInfo;
    }

    // General stuff
    public int readStackSize(Rectangle ROI) {
        return interfaces.readStackSize(ROI, scriptInfo.getCurrentEmulatorId());
    }

    public int readCustomStackSize(Rectangle ROI, java.util.List<Color> textColors, Map<String, int[][]> digitPatterns) {
        return interfaces.readCustomStackSize(ROI, textColors, digitPatterns, scriptInfo.getCurrentEmulatorId());
    }

    public int readCustomDigitsInArea(Rectangle ROI, java.util.List<Color> textColors, Map<String, int[][]> digitPatterns) {
        return interfaces.readCustomDigitsInArea(ROI, textColors, digitPatterns, scriptInfo.getCurrentEmulatorId());
    }

    public String readCustomLettersInArea(Rectangle ROI, java.util.List<Color> textColors, Map<String, int[][]> letterPatterns) {
        return interfaces.readCustomLettersInArea(ROI, textColors, letterPatterns, scriptInfo.getCurrentEmulatorId());
    }

    // Smithing section
    public boolean smithingIsOpen() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        return interfaces.smithingIsOpen(currentEmulatorId);
    }

    public void closeSmithingInterface() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.closeSmithingInterface(currentEmulatorId);
    }

    public void smithItem(SmithItems itemName) {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.smithItem(itemName, currentEmulatorId);
    }

    public void smithItem(int itemId) {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.smithItem(itemId, currentEmulatorId);
    }

    // Crafting section
    public boolean craftJewelleryIsOpen() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        return interfaces.craftJewelleryIsOpen(currentEmulatorId);
    }

    public void closeCraftJewelleryInterface() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.closeCraftJewelleryInterface(currentEmulatorId);
    }

    public void craftJewellery(int itemId) {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.craftJewellery(itemId, currentEmulatorId);
    }

    // General section
    public boolean isSelectedMake1() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        return interfaces.isSelectedMake(currentEmulatorId, "1");
    }

    public boolean isSelectedMakeAll() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        return interfaces.isSelectedMake(currentEmulatorId, "all");
    }

    public void selectMake1() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.selectMake(currentEmulatorId, "1");
    }

    public void selectMakeAll() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.selectMake(currentEmulatorId, "all");
    }

    public void selectMakeX() {
        String currentEmulatorId = scriptInfo.getCurrentEmulatorId();
        interfaces.selectMake(currentEmulatorId, "X");
    }
}
