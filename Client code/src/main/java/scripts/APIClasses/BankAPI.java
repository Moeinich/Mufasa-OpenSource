package scripts.APIClasses;

import interfaces.iBank;
import osr.mapping.Bank;
import scripts.ScriptInfo;

import java.awt.*;

public class BankAPI implements iBank {
    private final Bank bank;
    private final ScriptInfo scriptInfo;

    public BankAPI(Bank bank, ScriptInfo scriptInfo) {
        this.bank = bank;
        this.scriptInfo = scriptInfo;
    }

    public void open(String bankname) {
        bank.openBank(bankname, scriptInfo.getCurrentEmulatorId());
    }

    public void close() {
        bank.closeBank(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isOpen() {
        return bank.isBankOpen(scriptInfo.getCurrentEmulatorId());
    }

    public boolean isTabSelected(int tab) {
        return bank.isTabSelected(scriptInfo.getCurrentEmulatorId(), tab);
    }

    public boolean contains(String itemId, double threshold) {
        return bank.contains(itemId, threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public boolean contains(int itemID, double threshold) {
        return bank.contains(String.valueOf(itemID), threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public boolean contains(String itemId, double threshold, Color searchColor) {
        return bank.contains(itemId, threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public boolean contains(int itemID, double threshold, Color searchColor) {
        return bank.contains(String.valueOf(itemID), threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public void withdrawItem(String itemId, double threshold) {
        bank.withdrawItem(itemId, false, threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public void withdrawItem(int itemId, double threshold) {
        bank.withdrawItem(String.valueOf(itemId), false, threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public void withdrawItem(int itemId, boolean cache, double threshold) {
        bank.withdrawItem(String.valueOf(itemId), cache, threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public void withdrawItem(int itemID, double threshold, Color searchColor) {
        bank.withdrawItem(String.valueOf(itemID), false, threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public void withdrawItem(int itemID, boolean cache, double threshold, Color searchColor) {
        bank.withdrawItem(String.valueOf(itemID), cache, threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public void withdrawItem(String itemID, double threshold, Color searchColor) {
        bank.withdrawItem(itemID, false, threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public String getCurrentTab() {
        return bank.getCurrentTab(scriptInfo.getCurrentEmulatorId());
    }

    public int getCurrentTab(boolean returnInt) {
        try {
            String currentTab = bank.getCurrentTab(scriptInfo.getCurrentEmulatorId());
            if (currentTab != null && !currentTab.isEmpty()) {
                return Integer.parseInt(currentTab);
            } else {
                // Handle the case where the string is null or empty
                System.out.println("No tab information available or tab string is empty.");
                return -1;
            }
        } catch (NumberFormatException e) {
            // Handle the case where the string could not be parsed to an integer
            System.err.println("Error parsing tab index: " + e.getMessage());
            return -1;
        }
    }

    public void openTab(int tabInt) {
        bank.openTab(tabInt, scriptInfo.getCurrentEmulatorId());
    }

    public void setCustomQuantity(int quantity) {bank.setCustomQuantity(scriptInfo.getCurrentEmulatorId(), quantity);}

    public boolean isBankPinNeeded() {
        return bank.isBankPinNeeded(scriptInfo.getCurrentEmulatorId());
    }

    public void enterBankPin() {
        bank.enterBankPin(scriptInfo.getCurrentEmulatorId());
    }

    public void setBankPin(String bankPin) {bank.enterBankPin(scriptInfo.getCurrentEmulatorId(), bankPin);}

    public String setupDynamicBank() {
        try {
            return bank.setupDynamicBank(scriptInfo.getCurrentEmulatorId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public String findDynamicBank() {
        try {
            return bank.findDynamicBank(scriptInfo.getCurrentEmulatorId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void stepToBank() {
        bank.stepToBank(scriptInfo.getCurrentEmulatorId(), null);
    }

    public void stepToBank(String bankLoc) {
        bank.stepToBank(scriptInfo.getCurrentEmulatorId(), bankLoc);
    }

    public Rectangle findBankCloseButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankclose.png");
    }

    public Rectangle findWornButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankshowequip.png");
    }

    public Rectangle findSwapButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankswapmode.png", "bankswapmodeselected.png");
    }

    public Rectangle findInsertButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankinsertmode.png", "BankInsertModeSelected");
    }

    public Rectangle findItemButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankitemoption.png", "bankitemoptionselected.png");
    }

    public Rectangle findNoteButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "banknoteoption.png", "banknoteoptionselected.png");
    }

    public Rectangle findQuantity1Button() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankqty1.png", "bankqty1selected.png");
    }

    public Rectangle findQuantity5Button() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankqty5.png", "bankqty5selected.png");
    }

    public Rectangle findQuantity10Button() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankqty10.png", "bankqty10selected.png");
    }

    public Rectangle findQuantityCustomButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankqtyx.png", "bankqtyxselected.png");
    }

    public Rectangle findQuantityAllButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankqtyall.png", "bankqtyallselected.png");
    }

    public Rectangle findPlaceholdersButton() {
        return bank.findInterfaceBankButton(scriptInfo.getCurrentEmulatorId(), "bankdisabledplaceholders.png", "bankenabledplaceholders.png");
    }

    public Rectangle findSearchButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "banksearchitem.png");
    }

    public Rectangle findDepositInventoryButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankdepositinvent.png");
    }

    public Rectangle findDepositWornButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankdepositequip.png");
    }

    public Rectangle findFirstBankSlot() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "1stbankslot.png");
    }

    public Rectangle findBankTab(int tab) {
        return bank.findBankTab(tab, scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle[] bankItemGrid() {
        return bank.bankItemGrid(scriptInfo.getCurrentEmulatorId());
    }

    public void tapWornButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankshowequip.png");
    }

    public void tapSwapButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankswapmode.png");
    }

    public void tapInsertButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankinsertmode.png");
    }

    public void tapItemButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankitemoption.png");
    }

    public void tapNoteButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "banknoteoption.png");
    }

    public void tapQuantity1Button() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankqty1.png");
    }

    public void tapQuantity5Button() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankqty5.png");
    }

    public void tapQuantity10Button() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankqty10.png");
    }

    public void tapQuantityCustomButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankqtyx.png");
    }

    public void tapQuantityAllButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankqtyall.png");
    }

    public void tapPlaceholdersButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankdisabledplaceholders.png");
    }

    public void tapSearchButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "banksearchitem.png");
    }

    public void tapDepositInventoryButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankdepositinvent.png");
    }

    public void tapDepositWornButton() {
        bank.tapButton(scriptInfo.getCurrentEmulatorId(), "bankdepositequip.png");
    }

    public boolean isSelectedSwapButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankswapmodeselected.png") != null;
    }

    public boolean isSelectedInsertButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankinsertmodeselected.png") != null;
    }

    public boolean isSelectedItemButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankitemoptionselected.png") != null;
    }

    public boolean isSelectedNoteButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "banknoteoptionselected.png") != null;
    }

    public boolean isSelectedQuantity1Button() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankqty1selected.png") != null;
    }

    public boolean isSelectedQuantity5Button() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankqty5selected.png") != null;
    }

    public boolean isSelectedQuantity10Button() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankqty10selected.png") != null;
    }

    public boolean isSelectedQuantityCustomButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankqtyxselected.png") != null;
    }

    public boolean isSelectedQuantityAllButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankqtyallselected.png") != null;
    }

    public boolean isSelectedPlaceholdersButton() {
        return bank.findButton(scriptInfo.getCurrentEmulatorId(), "bankenabledplaceholders.png") != null;
    }

    public boolean isSelectedBankTab(int tabNr) {
        String currentTab = bank.getCurrentTab(scriptInfo.getCurrentEmulatorId());
        if (currentTab != null) {
            int currentTabNumber = Integer.parseInt(currentTab);
            return currentTabNumber == tabNr;
        } else {
            return false;
        }
    }

    public int stackSize(int itemID) {
        return bank.getItemStack(scriptInfo.getCurrentEmulatorId(), itemID, null);
    }

    public int stackSize(int itemID, Color searchColor){return bank.getItemStack(scriptInfo.getCurrentEmulatorId(), itemID, searchColor);}

    public boolean isSearchOpen() {
        return bank.isSearchOpen(scriptInfo.getCurrentEmulatorId());
    }

}
