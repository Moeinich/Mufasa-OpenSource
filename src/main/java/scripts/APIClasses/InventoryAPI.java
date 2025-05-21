package scripts.APIClasses;

import helpers.utils.ItemPair;
import interfaces.iInventory;
import javafx.util.Pair;
import osr.mapping.Inventory;
import scripts.ScriptInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InventoryAPI implements iInventory {
    private final Inventory inventory;
    private final ScriptInfo scriptInfo;

    public InventoryAPI(Inventory inventory, ScriptInfo scriptInfo) {
        this.inventory = inventory;
        this.scriptInfo = scriptInfo;
    }

    public void tapItem(Integer slotID) {
        inventory.tapItem(slotID, null, false, 1, scriptInfo.getCurrentEmulatorId());
    }

    public void tapItem(String itemId, double threshold) {
        inventory.tapItem(null, itemId, false, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public void tapItem(int itemId, double threshold) {
        inventory.tapItem(null, String.valueOf(itemId), false, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public void tapItem(int itemId, double threshold, Color targetColor) {
        inventory.tapItem(itemId, threshold, targetColor, scriptInfo.getCurrentEmulatorId());
    }

    public void tapItem(String itemId, boolean useCache, double threshold) {
        inventory.tapItem(null, itemId, useCache, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public void tapItem(int itemId, boolean useCache, double threshold) {
        inventory.tapItem(null, String.valueOf(itemId), useCache, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public void tapAllItems(int itemId, double threshold) {
        inventory.tapAllItems(itemId, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public void tapAllItems(List<ItemPair> itemPairs, double threshold) {
        inventory.tapAllItems(itemPairs, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public boolean slotContains(int slot, int itemID, double threshold) {
        if (slot < 1 || slot > 28) {
            System.out.println("invalid slot number.");
        }

        return inventory.slotContains(slot, itemID, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public void eat(String itemId, double threshold) {
        inventory.eat(itemId, threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public void eat(int itemId, double threshold) {
        inventory.eat(String.valueOf(itemId), threshold, scriptInfo.getCurrentEmulatorId(), null);
    }

    public void eat(String itemId, double threshold, Color searchColor) {
        inventory.eat(itemId, threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public void eat(int itemId, double threshold, Color searchColor) {
        inventory.eat(String.valueOf(itemId), threshold, scriptInfo.getCurrentEmulatorId(), searchColor);
    }

    public Point getItemCenterPoint(String itemId, double threshold) {
        return inventory.getItemCenterPoint(itemId, threshold, scriptInfo.getCurrentEmulatorId());
    }

    public Point getItemCenterPoint(int itemId, double threshold) {
        return inventory.getItemCenterPoint(String.valueOf(itemId), threshold, scriptInfo.getCurrentEmulatorId());
    }

    public Rectangle findItem(int itemId, double threshold, Color searchColor) {
        return inventory.findItem(itemId, threshold, searchColor, scriptInfo.getCurrentEmulatorId());
    }

    public boolean contains(String itemID, double threshold) {
        return inventory.contains(scriptInfo.getCurrentEmulatorId(), Integer.parseInt(itemID), threshold);
    }

    public boolean contains(int itemID, double threshold) {
        return contains(String.valueOf(itemID), threshold);
    }

    public boolean contains(int itemID, double threshold, Color targetColor) {
        return inventory.contains(scriptInfo.getCurrentEmulatorId(), itemID, threshold, targetColor);
    }

    public boolean contains(int itemID, double threshold, Color targetColor, Color exclusionColor) {
        return inventory.contains(scriptInfo.getCurrentEmulatorId(), itemID, threshold, targetColor, exclusionColor);
    }

    public boolean contains(String[] itemIDs, double threshold) {
        int[] intItemIDs = Arrays.stream(itemIDs)
                .mapToInt(Integer::parseInt)
                .toArray();
        return inventory.contains(scriptInfo.getCurrentEmulatorId(), intItemIDs, threshold);
    }

    public boolean contains(int[] itemIDs, double threshold, Color targetColor) {
        return inventory.contains(scriptInfo.getCurrentEmulatorId(), itemIDs, threshold, targetColor);
    }

    public boolean contains(int[] itemIDs, double threshold, Color targetColor, Color exclusionColor) {
        return inventory.contains(scriptInfo.getCurrentEmulatorId(), itemIDs, threshold, targetColor, exclusionColor);
    }

    public boolean containsAny(int[] itemIDs, double threshold) {
        for (int itemID : itemIDs) {
            if (contains(String.valueOf(itemID), threshold)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAny(int[] itemIDs, double threshold, Color color) {
        for (int itemID : itemIDs) {
            if (contains(itemID, threshold, color)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(int[] itemIDs, double threshold) {
        for (int itemID : itemIDs) {
            if (!contains(itemID, threshold)) {
                return false;
            }
        }
        return true;
    }

    public boolean containsAll(int[] itemIDs, double threshold, Color color) {
        for (int itemID : itemIDs) {
            if (!contains(itemID, threshold, color)) {
                return false;
            }
        }
        return true;
    }

    public int count(String itemID, double threshold) {
        return inventory.count(scriptInfo.getCurrentEmulatorId(), Integer.parseInt(itemID), threshold, null);
    }

    public int count(int itemID, double threshold) {
        return inventory.count(scriptInfo.getCurrentEmulatorId(), itemID, threshold, null);
    }

    public int count(int itemID, double threshold, Color color) {
        return inventory.count(scriptInfo.getCurrentEmulatorId(), itemID, threshold, color);
    }

    public int stackSize(int itemID) {
        return inventory.getItemStack(scriptInfo.getCurrentEmulatorId(), itemID);
    }

    public Rectangle itemPosition(String itemID, double threshold) {
        return inventory.itemPosition(scriptInfo.getCurrentEmulatorId(), itemID, threshold);
    }

    public Rectangle itemPosition(int itemID, double threshold) {
        return inventory.itemPosition(scriptInfo.getCurrentEmulatorId(), String.valueOf(itemID), threshold);
    }

    public Rectangle lastItemPosition(String itemID, double threshold) {
        return inventory.lastItemPosition(scriptInfo.getCurrentEmulatorId(), itemID, threshold);
    }

    public Rectangle lastItemPosition(int itemID, double threshold) {
        return inventory.lastItemPosition(scriptInfo.getCurrentEmulatorId(), String.valueOf(itemID), threshold);
    }

    public Integer itemSlotPosition(int itemID, double threshold) {
        return inventory.itemSlotPosition(scriptInfo.getCurrentEmulatorId(), new int[]{itemID}, threshold);
    }

    public Integer itemSlotPosition(int[] ItemIDs, double threshold) {
        return inventory.itemSlotPosition(scriptInfo.getCurrentEmulatorId(), ItemIDs, threshold);
    }

    public void dropInventItems(List<Integer> exclusionSlotList, boolean useCache) {
        inventory.dropInventItems(exclusionSlotList, scriptInfo.getCurrentEmulatorId());
    }

    public void dropInventItems(Integer exclusionSlot, boolean useCache) {
        // Convert the single integer exclusion slot to a List and call the existing method
        dropInventItems(Collections.singletonList(exclusionSlot), useCache);
    }

    public void dropInventItems(List<Integer> slotDropList) {
        inventory.dropInventItemsBySlots(slotDropList, scriptInfo.getCurrentEmulatorId());
    }

    public boolean isFull() {
        return inventory.isInventoryFull(scriptInfo.getCurrentEmulatorId());
    }

    public int usedSlots() {
        return inventory.getNumberOfUsedInventorySlots(scriptInfo.getCurrentEmulatorId());
    }

    public int emptySlots() {
        return inventory.getNumberOfEmptyInventorySlots(scriptInfo.getCurrentEmulatorId());
    }
}
