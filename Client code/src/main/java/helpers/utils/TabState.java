package helpers.utils;

public class TabState {
    private volatile boolean inventoryTabOpen;
    private volatile boolean magicTabOpen;

    // Constructor, getters and setters
    public TabState(boolean inventoryTabOpen, boolean magicTabOpen) {
        this.inventoryTabOpen = inventoryTabOpen;
        this.magicTabOpen = magicTabOpen;
    }

    public boolean isInventoryTabOpen() {
        return inventoryTabOpen;
    }

    public void setInventoryTabOpen(boolean inventoryTabOpen) {
        this.inventoryTabOpen = inventoryTabOpen;
    }

    public boolean isMagicTabOpen() {
        return magicTabOpen;
    }

    public void setMagicTabOpen(boolean magicTabOpen) {
        this.magicTabOpen = magicTabOpen;
    }
}