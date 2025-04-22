package helpers;

import UI.components.PaintBar;
import UI.components.utils.ObservableConcurrentHashMap;
import com.github.benmanes.caffeine.cache.*;
import com.sun.jna.platform.win32.WinDef;
import helpers.emulator.utils.EmulatorCaptureInfo;
import helpers.openCV.utils.MatchedRectangle;
import helpers.patterns.BankPinDigitPatterns;
import helpers.patterns.DigitPatterns;
import helpers.patterns.LetterPatterns;
import helpers.utils.HopProfile;
import helpers.utils.HopTimeInfo;
import helpers.utils.Skills;
import helpers.utils.TabState;
import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import osr.walker.MapInfo;
import osr.walker.utils.MapChunkHandler;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    // Define default expiration time
    private static final int DEFAULT_EXPIRE_AFTER_ACCESS_MINUTES = 30;

    // WORLDHOPPING
    private final Cache<String, HopProfile> hopProfileCache;
    private final Cache<String, String> hopProfilePerDeviceCache;
    private final Cache<String, HopTimeInfo> hopTimeCache;
    private final Cache<String, Long> hopSLLastUpdateTime;

    // ANTIBAN
    private final Cache<String, Long> antiBanTimeCache;
    private final Cache<String, Long> additionalAntiBanTimeCache;
    private final Cache<String, Integer> lastAntiBanActionCache;
    private final Cache<String, Boolean> optionalAntiBanGearEquipCache;
    private final Cache<String, Boolean> optionalAntiBanExtendedAFKCache;
    private final Cache<String, java.util.List<Skills>> optionalAntiBanSkillsCache;

    // TABSTATE
    private final Cache<String, TabState> tabStateCache;

    // GAMEVIEW
    private final Cache<String, Point> gameviewCenter;

    // MINIMAP
    private final Cache<String, Circle> minimapPosition;
    private final Cache<String, Point> minimapCenter;

    // BANK
    private final Cache<String, Rectangle> bankItemPositionCache;
    private final Cache<String, Rectangle> bankItemStackPositionCache;
    private final Cache<String, Rectangle> bankbuttonsCache;
    private final Cache<String, String> bankLocationCache;

    // INVENTORY
    private final Cache<Integer, Rectangle> slotBoxCache;
    private final Cache<String, Rectangle> tapItemLocationCache;

    // CHATBOX
    private final Cache<String, Rectangle> chatboxMenuRectCache;
    private final Cache<String, MatchedRectangle> makeOptionCache;
    private final Cache<String, MatchedRectangle> chatboxButtonCache;

    // INTERFACES
    private final Cache<String, MatchedRectangle> smithingCache;
    private final Cache<String, MatchedRectangle> craftingCache;
    private final Cache<String, MatchedRectangle> interfaceCache;

    // MAGIC
    private final Cache<String, MatchedRectangle> spellCache;

    // PLAYER
    private final Cache<String, Rect> orbCache;

    // XP
    private final Cache<String, String> deviceXpCache;

    // UTILS
    private final Cache<String, Mat> stringToMatCache;
    private final Cache<String, Image> stringToFXCache;
    private final Cache<String, Mat> itemMatCache;

    // Paint bar
    private final ObservableConcurrentHashMap<String, PaintBar> paintbars; // Retained for observable behavior

    // Direct capture
    private final Cache<String, WinDef.HWND> deviceHwndMap;
    private final Cache<Integer, WinDef.HWND> pidHwndMap;
    private final Cache<String, EmulatorCaptureInfo> devicePidMap;

    // Walker
    private final Cache<String, MapInfo> deviceMapInfo;

    /**
     * Constructor initializes all caches with Caffeine configurations.
     */
    public CacheManager() {
        // Common cache builder with default configurations (only time-based eviction)
        Caffeine<Object, Object> defaultCacheBuilder = Caffeine.newBuilder()
                .expireAfterAccess(DEFAULT_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES);

        // WORLDHOPPING
        hopProfileCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        hopProfilePerDeviceCache = defaultCacheBuilder.build();

        hopTimeCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        hopSLLastUpdateTime = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        // ANTIBAN
        antiBanTimeCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        additionalAntiBanTimeCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        lastAntiBanActionCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        optionalAntiBanGearEquipCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        optionalAntiBanExtendedAFKCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();

        optionalAntiBanSkillsCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES) // Example expiration time
                .build();

        // TABSTATE
        tabStateCache = defaultCacheBuilder.build();

        // GAMEVIEW
        gameviewCenter = defaultCacheBuilder.build();

        // MINIMAP
        minimapPosition = defaultCacheBuilder.build();
        minimapCenter = defaultCacheBuilder.build();

        // BANK
        bankItemPositionCache = defaultCacheBuilder.build();
        bankItemStackPositionCache = defaultCacheBuilder.build();
        bankbuttonsCache = defaultCacheBuilder.build();
        bankLocationCache = defaultCacheBuilder.build();

        // INVENTORY
        slotBoxCache = Caffeine.newBuilder()
                .expireAfterAccess(360, TimeUnit.MINUTES)
                .build();
        tapItemLocationCache = defaultCacheBuilder.build();

        // CHATBOX
        chatboxMenuRectCache = defaultCacheBuilder.build();
        makeOptionCache = defaultCacheBuilder.build();
        chatboxButtonCache = defaultCacheBuilder.build();

        // INTERFACES
        smithingCache = defaultCacheBuilder.build();
        craftingCache = defaultCacheBuilder.build();
        interfaceCache = defaultCacheBuilder.build();

        // MAGIC
        spellCache = defaultCacheBuilder.build();

        // PLAYER
        orbCache = defaultCacheBuilder.build();

        // XP
        deviceXpCache = defaultCacheBuilder.build();

        // UTILS
        stringToMatCache = defaultCacheBuilder.build();
        stringToFXCache = defaultCacheBuilder.build();
        itemMatCache = defaultCacheBuilder.build();

        // Paint bar (Retained as ObservableConcurrentHashMap for observable behavior)
        paintbars = new ObservableConcurrentHashMap<>();

        // Direct capture
        deviceHwndMap = defaultCacheBuilder.build();
        pidHwndMap = defaultCacheBuilder.build();
        devicePidMap = defaultCacheBuilder.build();

        // Walker
        deviceMapInfo = defaultCacheBuilder.build();
    }

    // -----------------------------------------------------------------------------------------------------------------//
    // Getters

    public MapInfo getMapInfo(String device) {
        return deviceMapInfo.getIfPresent(device);
    }

    public WinDef.HWND getHWNDForDevice(String device) {
        return deviceHwndMap.getIfPresent(device);
    }

    public WinDef.HWND getWindowHandle(int PID) {
        return pidHwndMap.getIfPresent(PID);
    }

    public Map<String, int[][]> getDigitPatterns() {
        return DigitPatterns.digitPatterns;
    }

    public Map<String, int[][]> getBankpinDigitPatterns() {
        return BankPinDigitPatterns.bankPinDigitPatterns;
    }

    public Map<String, int[][]> getLetterPatterns() {
        return LetterPatterns.letterPatterns;
    }

    public PaintBar getPaintBar(String device) {
        return paintbars.getOrDefault(device, null);
    }

    public ObservableConcurrentHashMap<String, PaintBar> getPaintbarInstance() {
        return paintbars;
    }

    public MatchedRectangle getSpell(String cacheKey) {
        return spellCache.getIfPresent(cacheKey);
    }

    public MatchedRectangle getSmithing(String cacheKey) {
        return smithingCache.getIfPresent(cacheKey);
    }

    public MatchedRectangle getCraftingCache(String cacheKey) {
        return craftingCache.getIfPresent(cacheKey);
    }

    public MatchedRectangle getInterface(String cacheKey) {
        return interfaceCache.getIfPresent(cacheKey);
    }

    public Rectangle getChatboxMenuRect(String cacheKey) {
        return chatboxMenuRectCache.getIfPresent(cacheKey);
    }

    public MatchedRectangle getMakeOption(String cacheKey) {
        return makeOptionCache.getIfPresent(cacheKey);
    }

    public MatchedRectangle getChatboxButton(String cacheKey) {
        return chatboxButtonCache.getIfPresent(cacheKey);
    }

    public Map<Integer, Rectangle> getSlotBox() {
        return Collections.unmodifiableMap(slotBoxCache.asMap());
    }

    public Rectangle getItemLocation(String location) {
        return tapItemLocationCache.getIfPresent(location);
    }

    public Rectangle getBankItemPositionCache(String cacheKey) {
        return bankItemPositionCache.getIfPresent(cacheKey);
    }

    public Rectangle getBankItemStackPositionCache(String cacheKey) {
        return bankItemStackPositionCache.getIfPresent(cacheKey);
    }

    public Rectangle getBankButton(String cacheKey) {
        return bankbuttonsCache.getIfPresent(cacheKey);
    }

    public String getBankLoc(String device) {
        return bankLocationCache.getIfPresent(device);
    }

    public String getDeviceXP(String device) {
        return deviceXpCache.getIfPresent(device);
    }


    public Mat getImageStringMat(String matName) {
        return stringToMatCache.getIfPresent(matName);
    }

    public Image getImageStringFX(String matName) {
        return stringToFXCache.getIfPresent(matName);
    }

    public Mat getItemMat(String matName) {
        return itemMatCache.getIfPresent(matName);
    }

    public Circle getMinimapPosition(String device) {
        return minimapPosition.getIfPresent(device);
    }

    public Point getMinimapCenter(String device) {
        return minimapCenter.getIfPresent(device);
    }

    public Point getGameviewCenter(String device) {
        return gameviewCenter.getIfPresent(device);
    }

    public HopProfile getHopProfile(String profileName) {
        return hopProfileCache.getIfPresent(profileName);
    }

    public String getHopProfilePerDevice(String device) {
        return hopProfilePerDeviceCache.getIfPresent(device);
    }

    public HopTimeInfo getHopTimeInfo(String device) {
        return hopTimeCache.getIfPresent(device);
    }

    public long getNextHopTime(String device) {
        HopTimeInfo hopTimeInfo = hopTimeCache.getIfPresent(device);
        return (hopTimeInfo != null) ? hopTimeInfo.nextHopTime : 0;
    }

    public Long getSLLastUpdateTime() {
        return hopSLLastUpdateTime.getIfPresent("lastUpdateTime");
    }

    public TabState getTabState(String device) {
        return tabStateCache.getIfPresent(device);
    }

    public Long getAntiBanTime(String device) {
        return antiBanTimeCache.getIfPresent(device);
    }

    public Long getAdditionalAntiBanTime(String device) {
        return additionalAntiBanTimeCache.getIfPresent(device);
    }

    public Integer getLastAntiBanAction(String device) {
        return lastAntiBanActionCache.getIfPresent(device);
    }

    public boolean isOptionalGearEquipEnabled(String device) {
        Boolean value = optionalAntiBanGearEquipCache.getIfPresent(device);
        return value != null ? value : false;
    }

    public boolean isOptionalExtendedAFKEnabled(String device) {
        Boolean value = optionalAntiBanExtendedAFKCache.getIfPresent(device);
        return value != null ? value : false;
    }

    public boolean isOptionalSkillListPresent(String device) {
        return optionalAntiBanSkillsCache.getIfPresent(device) != null;
    }

    public java.util.List<Skills> getOptionalSkillsList(String device) {
        return optionalAntiBanSkillsCache.getIfPresent(device);
    }

    // -----------------------------------------------------------------------------------------------------------------//
    // Setters

    public void setMapInfo(String device, MapInfo mapInfo) {
        deviceMapInfo.put(device, mapInfo);
    }

    public void setHWNDForDevice(String device, WinDef.HWND hwnd) {
        deviceHwndMap.put(device, hwnd);
    }

    /**
     * Caches the HWND for a given PID.
     *
     * @param pid  The process ID.
     * @param hwnd The HWND to cache.
     */
    public void setWindowHandle(int pid, WinDef.HWND hwnd) {
        pidHwndMap.put(pid, hwnd);
    }

    public void setPaintBar(String device, PaintBar paintBar) {
        paintbars.put(device, paintBar);
    }

    public void setSpell(String cacheKey, MatchedRectangle matchedRect) {
        spellCache.put(cacheKey, matchedRect);
    }

    public void setSmithing(String cacheKey, MatchedRectangle matchedRect) {
        smithingCache.put(cacheKey, matchedRect);
    }

    public void setCraftingCache(String cacheKey, MatchedRectangle matchedRect) {
        craftingCache.put(cacheKey, matchedRect);
    }

    public void setInterface(String cacheKey, MatchedRectangle matchedRect) {
        interfaceCache.put(cacheKey, matchedRect);
    }

    public void setChatboxMenuRect(String cacheKey, Rectangle rect) {
        chatboxMenuRectCache.put(cacheKey, rect);
    }

    public void setMakeOption(String cacheKey, MatchedRectangle matchedRect) {
        makeOptionCache.put(cacheKey, matchedRect);
    }

    public void setChatboxButton(String cacheKey, MatchedRectangle matchedRect) {
        chatboxButtonCache.put(cacheKey, matchedRect);
    }

    public void setSlotBox(int slotNumber, Rectangle slotRect) {
        slotBoxCache.put(slotNumber, slotRect);
    }

    public void setItemLocation(String location, Rectangle itemLoc) {
        tapItemLocationCache.put(location, itemLoc);
    }

    public void setBankItemPositionCache(String cacheKey, Rectangle rect) {
        bankItemPositionCache.put(cacheKey, rect);
    }

    public void setBankItemStackPositionCache(String cacheKey, Rectangle rect) {
        bankItemStackPositionCache.put(cacheKey, rect);
    }

    public void setBankButton(String cacheKey, Rectangle rect) {
        bankbuttonsCache.put(cacheKey, rect);
    }

    public void setBankLoc(String device, String bankLoc) {
        bankLocationCache.put(device, bankLoc);
    }

    public void setDeviceXP(String device, String XP) {
        deviceXpCache.put(device, XP);
    }

    public void setImageStringMat(String matName, Mat mat) {
        stringToMatCache.put(matName, mat);
    }

    public void setImageStringFX(String matName, Image mat) {
        stringToFXCache.put(matName, mat);
    }

    public void setImageMat(String matName, Mat mat) {
        itemMatCache.put(matName, mat);
    }

    public void setMinimapPosition(String device, Circle circle) {
        minimapPosition.put(device, circle);
    }

    public void setMinimapCenter(String device, Point point) {
        minimapCenter.put(device, point);
    }

    public void setGameviewCenter(String device, Point centerPoint) {
        gameviewCenter.put(device, centerPoint);
    }

    public void setHopProfile(String profileName, HopProfile profile) {
        hopProfileCache.put(profileName, profile);
    }

    public void setHopProfilePerDevice(String emulatorID, String profileName) {
        hopProfilePerDeviceCache.put(emulatorID, profileName);
    }

    public void setHopTime(String device, HopTimeInfo hopTimeInfo) {
        hopTimeCache.put(device, hopTimeInfo);
    }

    public void setSLLastUpdateTime(Long updateTime) {
        hopSLLastUpdateTime.put("lastUpdateTime", updateTime);
    }

    public void setTabState(String device, TabState tabState) {
        tabStateCache.put(device, tabState);
    }

    public void setAntiBanTime(String device, long nextAntiBanTime) {
        antiBanTimeCache.put(device, nextAntiBanTime);
    }

    public void setAdditionalAntiBanTime(String device, long nextAntiBanTime) {
        additionalAntiBanTimeCache.put(device, nextAntiBanTime);
    }

    public void setLastAntiBanAction(String device, int action) {
        lastAntiBanActionCache.put(device, action);
    }

    public void setOptionalGearEquipEnabled(String device, boolean enabled) {
        optionalAntiBanGearEquipCache.put(device, enabled);
    }

    public void setOptionalExtendedAFKEnabled(String device, boolean enabled) {
        optionalAntiBanExtendedAFKCache.put(device, enabled);
    }

    public void addOptionalAntiBanSkill(String device, Skills skill) {
        // Retrieve the current skill list from the cache
        java.util.List<Skills> currentSkills = optionalAntiBanSkillsCache.getIfPresent(device);

        if (currentSkills == null) {
            // If no list exists, create a new one with the given skill
            currentSkills = new ArrayList<>();
            currentSkills.add(skill);
        } else {
            // Add the skill to the existing list if it's not already present
            if (!currentSkills.contains(skill)) {
                currentSkills.add(skill);
            }
        }

        // Update the cache with the modified skill list
        optionalAntiBanSkillsCache.put(device, currentSkills);
    }

    // -----------------------------------------------------------------------------------------------------------------//
    // Individual Remove Methods

    public void removeMapInfo(String device) {
        deviceMapInfo.invalidate(device);
    }

    public void removeHWNDForDevice(String device) {
        deviceHwndMap.invalidate(device);
    }

    /**
     * Removes the PID associated with a device.
     *
     * @param device The device identifier.
     */
    public void removeEmulatorCaptureInfo(String device) {
        devicePidMap.invalidate(device);
    }

    /**
     * Retrieves the EmulatorCaptureInfo for a given device.
     *
     * @param device The device identifier.
     * @return The EmulatorCaptureInfo, or null if not present.
     */
    public EmulatorCaptureInfo getEmulatorCaptureInfo(String device) {
        return devicePidMap.getIfPresent(device);
    }

    /**
     * Caches the EmulatorCaptureInfo for a given device.
     *
     * @param device The device identifier.
     * @param info   The EmulatorCaptureInfo to cache.
     */
    public void setEmulatorCaptureInfo(String device, EmulatorCaptureInfo info) {
        devicePidMap.put(device, info);
    }

    public void removeOrb(String cacheKey) {
        orbCache.invalidate(cacheKey);
    }

    public void removeSpell(String cacheKey) {
        spellCache.invalidate(cacheKey);
    }

    public void removeSmithing(String cacheKey) {
        smithingCache.invalidate(cacheKey);
    }

    public void removeCraftingCache(String cacheKey) {
        craftingCache.invalidate(cacheKey);
    }

    public void removeInterface(String cacheKey) {
        interfaceCache.invalidate(cacheKey);
    }

    public void removeChatboxMenuRect(String cacheKey) {
        chatboxMenuRectCache.invalidate(cacheKey);
    }

    public void removeMakeOption(String cacheKey) {
        makeOptionCache.invalidate(cacheKey);
    }

    public void removeChatboxButton(String cacheKey) {
        chatboxButtonCache.invalidate(cacheKey);
    }

    public void removeSlotBox(int slotNumber) {
        slotBoxCache.invalidate(slotNumber);
    }

    public void removeItemLocation(String location) {
        tapItemLocationCache.invalidate(location);
    }

    public void removeBankItemPositionCache(String cacheKey) {
        bankItemPositionCache.invalidate(cacheKey);
    }

    public void removeBankItemStackPositionCache(String cacheKey) {
        bankItemStackPositionCache.invalidate(cacheKey);
    }

    public void removeBankButton(String cacheKey) {
        bankbuttonsCache.invalidate(cacheKey);
    }

    public void removeBankLoc(String device) {
        bankLocationCache.invalidate(device);
    }

    public void removeDeviceXP(String device) {
        deviceXpCache.invalidate(device);
    }

    public void removeImageStringMat(String matName) {
        stringToMatCache.invalidate(matName);
    }

    public void removeImageStringFX(String matName) {
        stringToFXCache.invalidate(matName);
    }

    public void removeItemMat(String matName) {
        itemMatCache.invalidate(matName);
    }

    public void removeMinimapPosition(String device) {
        minimapPosition.invalidate(device);
    }

    public void removeMinimapCenter(String device) {
        minimapCenter.invalidate(device);
    }

    public void removeGameviewCenter(String device) {
        gameviewCenter.invalidate(device);
    }

    public void removeHopProfile(String profileName) {
        hopProfileCache.invalidate(profileName);
    }

    public void removeHopProfilePerDevice(String device) {
        hopProfilePerDeviceCache.invalidate(device);
    }

    public void removeHopTimeInfo(String device) {
        hopTimeCache.invalidate(device);
    }

    public void removeSLLastUpdateTime() {
        hopSLLastUpdateTime.invalidate("lastUpdateTime");
    }

    public void removeTabState(String device) {
        tabStateCache.invalidate(device);
    }


    public void removePaintBar(String device) {
        PaintBar instance = paintbars.get(device);
        if (instance != null) {
            instance.disableAll();
        }
        paintbars.remove(device); // Retained for observable behavior
    }

    public void removeAntiBanTime(String device) {
        antiBanTimeCache.invalidate(device);
    }

    public void removeLastAntiBanAction(String device) {
        lastAntiBanActionCache.invalidate(device);
    }

    public void removeOptionalAntiBanSettings(String device) {
        optionalAntiBanGearEquipCache.invalidate(device);
        optionalAntiBanExtendedAFKCache.invalidate(device);
        optionalAntiBanSkillsCache.invalidate(device);
        additionalAntiBanTimeCache.invalidate(device);
    }

    // -----------------------------------------------------------------------------------------------------------------//
    // Combined Remove for All Caches Related to a Specific Device

    /**
     * Cleans all cache entries related to a specific device.
     *
     * @param device The device identifier.
     */
    public void cleanCaches(String device) {
        // Walker
        MapChunkHandler.removeDeviceChunkPosition(device);

        // Bank
        bankItemPositionCache.invalidate(device);
        bankItemStackPositionCache.invalidate(device);
        bankbuttonsCache.invalidate(device);
        removeBankLoc(device);

        // AntiBan
        removeAntiBanTime(device);
        removeLastAntiBanAction(device);
        removeOptionalAntiBanSettings(device);

        // Paint bar
        removePaintBar(device);

        // Worldhops
        hopProfileCache.invalidateAll();
        removeHopProfilePerDevice(device);
        removeHopTimeInfo(device);
        removeSLLastUpdateTime();

        // Gameview
        removeGameviewCenter(device);

        // Minimap
        removeMinimapPosition(device);
        removeMinimapCenter(device);

        // Tabstate
        removeTabState(device);

        // XP
        removeDeviceXP(device);

        // NONE DEVICE SPECIFIC
        // Player
        orbCache.invalidateAll();

        // Magic
        spellCache.invalidateAll();

        // Interfaces
        smithingCache.invalidateAll();
        craftingCache.invalidateAll();
        interfaceCache.invalidateAll();

        // Chatbox
        chatboxMenuRectCache.invalidateAll();
        makeOptionCache.invalidateAll();
        chatboxButtonCache.invalidateAll();

        // Inventory
        slotBoxCache.invalidateAll();
        tapItemLocationCache.invalidateAll();
    }
}