package scripts.APIClasses;

import helpers.OCR.utils.FontName;
import interfaces.iChatbox;
import osr.mapping.Chatbox;
import osr.mapping.GameOCR;
import scripts.ScriptInfo;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class ChatboxAPI implements iChatbox {
    private final Chatbox chatbox;
    private final ScriptInfo scriptInfo;
    private final GameOCR gameOCR;

    public ChatboxAPI(Chatbox chatbox, ScriptInfo scriptInfo, GameOCR gameOCR) {
        this.chatbox = chatbox;
        this.scriptInfo = scriptInfo;
        this.gameOCR = gameOCR;
    }

    // Booleans
    public boolean isSelectedMake1() {
        return chatbox.isSelectedMake1(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isSelectedMake5() {
        return chatbox.isSelectedMake5(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isSelectedMake10() {
        return chatbox.isSelectedMake10(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isSelectedMakeAll() {
        return chatbox.isSelectedMakeAll(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isMakeMenuVisible() {
        return chatbox.makeMenuVisible(scriptInfo.getCurrentEmulatorId());
    }

    // Voids
    public void makeOption(int optionNumber) {
        chatbox.makeOption(optionNumber, scriptInfo.getCurrentEmulatorId());
    }

    public void selectMake1() {
        chatbox.selectMake1(scriptInfo.getCurrentEmulatorId());
    }

    public void selectMake5() {
        chatbox.selectMake5(scriptInfo.getCurrentEmulatorId());
    }

    public void selectMake10() {
        chatbox.selectMake10(scriptInfo.getCurrentEmulatorId());
    }

    public void selectMakeX() {
        chatbox.selectMakeX(scriptInfo.getCurrentEmulatorId());
    }

    public void selectMakeAll() {
        chatbox.selectMakeAll(scriptInfo.getCurrentEmulatorId());
    }

    public void closeChatbox() {
        chatbox.closeChatbox(scriptInfo.getCurrentEmulatorId());
    }

    public void openAllChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.allButton);
    }

    public void openGameChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.gameButton);
    }

    public void openPublicChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.publicButton);
    }

    public void openPrivateChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.privateButton);
    }

    public void openChannelChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.channelButton);
    }

    public void openClanChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.clanButton);
    }

    public void openTradeChat() {
        chatbox.openChatboxHelper(scriptInfo.getCurrentEmulatorId(), chatbox.tradeButton);
    }

    public boolean isAllChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.allButton);
    }

    public boolean isGameChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.gameButton);
    }

    public boolean isPublicChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.publicButton);
    }

    public boolean isPrivateChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.privateButton);
    }

    public boolean isChannelChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.channelButton);
    }

    public boolean isClanChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.clanButton);
    }

    public boolean isTradeChatActive() {
        return chatbox.isChatboxOpenHelper(scriptInfo.getCurrentEmulatorId(), chatbox.tradeButton);
    }

    public boolean isTextVisible(Rectangle searchArea, List<Color> colors, Map<String, int[][]> letterPatterns, String stringToFind) {
        return gameOCR.isTextVisible(searchArea, colors, letterPatterns, stringToFind, scriptInfo.getCurrentEmulatorId());
    }

    // Integers
    public int readDigitsInArea(java.awt.Rectangle areaToOCR, java.util.List<java.awt.Color> colorsToScan) {
        return gameOCR.readDigitsInArea(areaToOCR, colorsToScan, scriptInfo.getCurrentEmulatorId());
    }

    // Strings
    public String readLastLine(java.awt.Rectangle areaToRead) {
        return gameOCR.readChatboxArea(scriptInfo.getCurrentEmulatorId(), areaToRead);
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

    // Rectangles
    public Rectangle findChatboxMenu() {
        return chatbox.findChatboxMenu(scriptInfo.getCurrentEmulatorId());
    }

}
