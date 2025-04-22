package osr.mapping;

import helpers.GetGameView;
import helpers.Logger;
import helpers.openCV.ImageRecognition;
import helpers.utils.EquipmentSlot;
import helpers.visualFeedback.FeedbackObservables;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.mapping.utils.ItemProcessor;

import java.awt.*;

public class Equipment {
    private final Logger logger;
    private final GetGameView getGameView;
    private final ImageRecognition imageRecognition;
    private final GameTabs gameTabs;
    private final ItemProcessor itemProcessor;

    // Static pre-defined rectangles
    public static final Rectangle HELM_RECT = new Rectangle(672, 228, 45, 42);
    public static final Rectangle CAPE_RECT = new Rectangle(631, 266, 43, 42);
    public static final Rectangle AMULET_RECT = new Rectangle(672, 267, 42, 42);
    public static final Rectangle AMMO_RECT = new Rectangle(715, 266, 42, 41);
    public static final Rectangle WEAPON_RECT = new Rectangle(617, 306, 41, 40);
    public static final Rectangle BODY_RECT = new Rectangle(672, 306, 45, 43);
    public static final Rectangle SHIELD_RECT = new Rectangle(727, 303, 46, 43);
    public static final Rectangle LEGS_RECT = new Rectangle(674, 346, 40, 40);
    public static final Rectangle GLOVES_RECT = new Rectangle(617, 387, 42, 40);
    public static final Rectangle BOOTS_RECT = new Rectangle(672, 386, 43, 42);
    public static final Rectangle RING_RECT = new Rectangle(729, 386, 42, 40);

    public Equipment(ItemProcessor itemProcessor, Logger logger, GetGameView getGameView, ImageRecognition imageRecognition, GameTabs gameTabs) {
        this.logger = logger;
        this.imageRecognition = imageRecognition;
        this.getGameView = getGameView;
        this.gameTabs = gameTabs;
        this.itemProcessor = itemProcessor;

    }

    public boolean isOpen(String device) {
        return gameTabs.isTabOpen(device, "Equip");
    }

    public boolean open(String device) {
        gameTabs.openTab(device, "Equip");
        return true;
    }

    public Boolean itemAt(String device, EquipmentSlot equipmentSlot, int itemToCheck, Color checkColor) {
        gameTabs.openTab(device, "Equip");

        Rectangle itemRect;  // Initialize itemRect to null
        switch (equipmentSlot) {
            case HEAD:
                itemRect = HELM_RECT;
                break;
            case CAPE:
                itemRect = CAPE_RECT;
                break;
            case NECK:
                itemRect = AMULET_RECT;
                break;
            case AMMUNITION:
                itemRect = AMMO_RECT;
                break;
            case WEAPON:
                itemRect = WEAPON_RECT;
                break;
            case SHIELD:
                itemRect = SHIELD_RECT;
                break;
            case BODY:
                itemRect = BODY_RECT;
                break;
            case LEGS:
                itemRect = LEGS_RECT;
                break;
            case HANDS:
                itemRect = GLOVES_RECT;
                break;
            case FEET:
                itemRect = BOOTS_RECT;
                break;
            case RING:
                itemRect = RING_RECT;
                break;
            default:
                logger.log("EquipmentSlot not recognized", device);
                return false;  // Return false if equipment slot is not recognized
        }

        FeedbackObservables.rectangleObservable.setValue(device, itemRect);
        Rect subMatRect = new Rect(itemRect.x, itemRect.y, itemRect.width, itemRect.height);
        Mat itemToFind = itemProcessor.getItemImage(itemToCheck);
        Mat gameView = getGameView.getSubmat(device, subMatRect);

        Rectangle result;
        if (checkColor != null) {
            result = imageRecognition.returnBestMatchWithColor(device, itemToFind, gameView, 0.75, checkColor, subMatRect.x, subMatRect.y, 3);
        } else {
            result = imageRecognition.returnBestMatchObject(itemToFind, gameView, 0.75);
        }

        if (result != null) {
            FeedbackObservables.rectangleObservable.setValue(device, result);
        }
        return result != null;
    }
}
