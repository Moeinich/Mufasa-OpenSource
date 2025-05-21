package scripts.APIClasses;

import interfaces.iGE;
import osr.mapping.GrandExchange;
import scripts.ScriptInfo;

public class GrandExchangeAPI implements iGE {
    private final GrandExchange grandExchange;
    private final ScriptInfo scriptInfo;

    public GrandExchangeAPI(GrandExchange grandExchange, ScriptInfo scriptInfo) {
        this.grandExchange = grandExchange;
        this.scriptInfo = scriptInfo;
    }

    public int getItemPrice(int itemID) {
        return grandExchange.getPrice(itemID);
    }

    public boolean isOpen() {
        return grandExchange.isOpen(scriptInfo.getCurrentEmulatorId());
    }

    public int getSlotProgress(int slot) {
        return grandExchange.getProgress(scriptInfo.getCurrentEmulatorId(), slot);
    }

    public int getCanceled() {
        return grandExchange.getCanceled(scriptInfo.getCurrentEmulatorId());
    }

    public int getCompleted() {
        return grandExchange.getCompleted(scriptInfo.getCurrentEmulatorId());
    }

    public void collectAllItems() {
        grandExchange.collectAllItems(scriptInfo.getCurrentEmulatorId());
    }

    public boolean hasCollectableItems() {
        return grandExchange.hasCollectableItems(scriptInfo.getCurrentEmulatorId());
    }

    public int buyItem(String searchString, int itemID, int quantity, int price) {
        return grandExchange.buyItem(scriptInfo.getCurrentEmulatorId(), searchString, itemID, quantity, price);
    }

    public int sellItem(int itemID, int quantity, int price) {
        return grandExchange.sellItem(scriptInfo.getCurrentEmulatorId(), itemID, quantity, price);
    }

    public int getFirstAvailableSlot() {
        return grandExchange.getFirstAvailableSlot(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isSlotAvailable(int slot) {
        return grandExchange.isSlotAvailable(scriptInfo.getCurrentEmulatorId(), slot);
    }

    public int slotsAvailable() {
        return grandExchange.slotsAvailable(scriptInfo.getCurrentEmulatorId());
    }

    public boolean has1stItemToCollect() {
        return grandExchange.has1stItemToCollect(scriptInfo.getCurrentEmulatorId());
    }

    public boolean has2ndItemToCollect() {
        return grandExchange.has2ndItemToCollect(scriptInfo.getCurrentEmulatorId());
    }
}
