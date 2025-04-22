package scripts.APIClasses;

import helpers.utils.EquipmentSlot;
import interfaces.iEquipment;
import osr.mapping.Equipment;
import scripts.ScriptInfo;

import java.awt.*;

import static osr.mapping.Equipment.*;

public class EquipmentAPI implements iEquipment {
    private final Equipment equipment;
    private final ScriptInfo scriptInfo;

    public EquipmentAPI(Equipment equipment, ScriptInfo scriptInfo) {
        this.equipment = equipment;
        this.scriptInfo = scriptInfo;
    }

    public Boolean isOpen() {
        return equipment.isOpen(scriptInfo.getCurrentEmulatorId());
    }

    public Boolean open() {
        return equipment.open(scriptInfo.getCurrentEmulatorId());
    }

    public Boolean itemAt(EquipmentSlot equipmentSlot, int itemToCheck) {
        return equipment.itemAt(scriptInfo.getCurrentEmulatorId(), equipmentSlot, itemToCheck, null);
    }

    public Boolean itemAt(EquipmentSlot equipmentSlot, int itemToCheck, Color checkColor) {
        return equipment.itemAt(scriptInfo.getCurrentEmulatorId(), equipmentSlot, itemToCheck, checkColor);
    }

    public Rectangle findHelm() {
        return HELM_RECT;
    }

    public Rectangle findCape() {
        return CAPE_RECT;
    }

    public Rectangle findAmulet() {
        return AMULET_RECT;
    }

    public Rectangle findAmmo() {
        return AMMO_RECT;
    }

    public Rectangle findWeapon() {
        return WEAPON_RECT;
    }

    public Rectangle findBody() {
        return BODY_RECT;
    }

    public Rectangle findShield() {
        return SHIELD_RECT;
    }

    public Rectangle findLegs() {
        return LEGS_RECT;
    }

    public Rectangle findGloves() {
        return GLOVES_RECT;
    }

    public Rectangle findFeet() {
        return BOOTS_RECT;
    }

    public Rectangle findRing() {
        return RING_RECT;
    }
}
