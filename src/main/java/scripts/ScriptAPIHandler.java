package scripts;

import scripts.APIClasses.*;

public class ScriptAPIHandler {
    private final GrandExchangeAPI grandExchangeAPI;
    private final InterfacesAPI interfacesAPI;
    private final BankAPI bankAPI;
    private final ConditionAPI conditionAPI;
    private final ClientAPI clientAPI;
    private final DepositBoxAPI depositBoxAPI;
    private final EquipmentAPI equipmentAPI;
    private final GameAPI gameAPI;
    private final GameTabsAPI gameTabsAPI;
    private final InventoryAPI inventoryAPI;
    private final LoginAPI loginAPI;
    private final LogoutAPI logoutAPI;
    private final MagicAPI magicAPI;
    private final OverlayAPI overlayAPI;
    private final PlayerAPI playerAPI;
    private final PrayerAPI prayerAPI;
    private final StatsAPI statsAPI;
    private final WalkerAPI walkerAPI;
    private final XPBarAPI xpBarAPI;
    private final ChatboxAPI chatboxAPI;
    private final LoggerAPI loggerAPI;
    private final ObjectsAPI objectsAPI;
    private final PaintAPI paintAPI;
    private final OcrAPI ocrAPI;

    public ScriptAPIHandler(GrandExchangeAPI grandExchangeAPI, InterfacesAPI interfacesAPI, BankAPI bankAPI, ClientAPI clientAPI, ConditionAPI conditionAPI, DepositBoxAPI depositBoxAPI, EquipmentAPI equipmentAPI, GameAPI gameAPI, GameTabsAPI gameTabsAPI, InventoryAPI inventoryAPI, LoginAPI loginAPI, LogoutAPI logoutAPI, MagicAPI magicAPI, OverlayAPI overlayAPI, PlayerAPI playerAPI, PrayerAPI prayerAPI,
                            StatsAPI statsAPI, WalkerAPI walkerAPI, XPBarAPI xpBarAPI, ChatboxAPI chatboxAPI, LoggerAPI loggerAPI, ObjectsAPI objectsAPI, PaintAPI paintAPI, OcrAPI ocrAPI) {
        this.grandExchangeAPI = grandExchangeAPI;
        this.interfacesAPI = interfacesAPI;
        this.bankAPI = bankAPI;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.depositBoxAPI = depositBoxAPI;
        this.equipmentAPI = equipmentAPI;
        this.gameAPI = gameAPI;
        this.gameTabsAPI = gameTabsAPI;
        this.inventoryAPI = inventoryAPI;
        this.loginAPI = loginAPI;
        this.logoutAPI = logoutAPI;
        this.magicAPI = magicAPI;
        this.overlayAPI = overlayAPI;
        this.playerAPI = playerAPI;
        this.prayerAPI = prayerAPI;
        this.statsAPI = statsAPI;
        this.walkerAPI = walkerAPI;
        this.xpBarAPI = xpBarAPI;
        this.chatboxAPI = chatboxAPI;
        this.loggerAPI = loggerAPI;
        this.objectsAPI = objectsAPI;
        this.paintAPI = paintAPI;
        this.ocrAPI = ocrAPI;
    }

    public BankAPI bankAPI() {
        return bankAPI;
    }

    public ClientAPI clientAPI() {
        return clientAPI;
    }

    public ConditionAPI getConditionAPI() {
        return conditionAPI;
    }

    public DepositBoxAPI depositBoxAPI() {
        return depositBoxAPI;
    }

    public EquipmentAPI equipmentAPI() {
        return equipmentAPI;
    }

    public GameAPI gameAPI() {
        return gameAPI;
    }

    public GameTabsAPI gameTabsAPI() {
        return gameTabsAPI;
    }

    public InventoryAPI inventoryAPI() {
        return inventoryAPI;
    }

    public LoginAPI loginAPI() {
        return loginAPI;
    }

    public LogoutAPI logoutAPI() {
        return logoutAPI;
    }

    public MagicAPI magicAPI() {
        return magicAPI;
    }

    public OverlayAPI overlayAPI() {
        return overlayAPI;
    }

    public PlayerAPI playerAPI() {
        return playerAPI;
    }

    public PrayerAPI prayerAPI() {
        return prayerAPI;
    }

    public StatsAPI statsAPI() {
        return statsAPI;
    }

    public WalkerAPI walkerAPI() {
        return walkerAPI;
    }

    public XPBarAPI xpBarAPI() {
        return xpBarAPI;
    }

    public ChatboxAPI chatboxAPI() {
        return chatboxAPI;
    }

    public LoggerAPI loggerAPI() {
        return loggerAPI;
    }

    public ObjectsAPI objectsAPI() {
        return objectsAPI;
    }

    public PaintAPI paintAPI() {
        return paintAPI;
    }

    public GrandExchangeAPI grandExchangeAPI() {return grandExchangeAPI;}

    public InterfacesAPI interfacesAPI() {return interfacesAPI;}

    public OcrAPI ocrAPI() {return ocrAPI;}
}
