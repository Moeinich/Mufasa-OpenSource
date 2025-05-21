package scripts.APIClasses;

import interfaces.iDepositBox;
import osr.mapping.DepositBox;
import scripts.ScriptInfo;

import java.awt.*;

public class DepositBoxAPI implements iDepositBox {
    private final DepositBox depositBox;
    private final ScriptInfo scriptInfo;

    public DepositBoxAPI(DepositBox depositBox, ScriptInfo scriptInfo) {
        this.depositBox = depositBox;
        this.scriptInfo = scriptInfo;
    }

    public Boolean open() {
        //Logic to open the bank
        return false;
    }

    public boolean opened() {
        //Logic to determine if bank interface is open
        return false;
    }

    public Rectangle findQuantity1() {
        return depositBox.findQuantity1(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findQuantity5() {
        return depositBox.findQuantity5(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findQuantity10() {
        return depositBox.findQuantity10(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findQuantityCustom() {
        return depositBox.findQuantityCustom(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findQuantityAll() {
        return depositBox.findQuantityAll(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findDepositInventory() {
        return depositBox.findDepositInventory(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findDepositWorn() {
        return depositBox.findDepositWorn(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findDepositLoot() {
        return depositBox.findDepositLoot(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findCloseDepositBox() {
        return depositBox.findCloseDepositBox(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findSetCustomQuantity() {
        return depositBox.findSetCustomQuantity(scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle[] buildDepositBoxGrid() {
        return depositBox.buildDepositBoxGrid(scriptInfo.getCurrentEmulatorId());
    }
}
