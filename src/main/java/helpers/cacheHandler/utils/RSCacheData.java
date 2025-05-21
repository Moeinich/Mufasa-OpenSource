package helpers.cacheHandler.utils;

public class RSCacheData {
    private byte[] mainCacheDat2;  // .dat2 file
    private byte[] skeleton;       // .idx0
    private byte[] skin;           // .idx1
    private byte[] config;         // .idx2
    private byte[] interfaceData;  // .idx3
    private byte[] landscape;      // .idx5
    private byte[] models;         // .idx7
    private byte[] sprites;        // .idx8
    private byte[] texture;        // .idx9
    private byte[] clientScripts;  // .idx12
    private byte[] fonts;          // .idx13

    // Getters and setters
    public byte[] getMainCacheDat2() {
        return mainCacheDat2;
    }
    public void setMainCacheDat2(byte[] mainCacheDat2) {
        this.mainCacheDat2 = mainCacheDat2;
    }

    public byte[] getSkeleton() {
        return skeleton;
    }
    public void setSkeleton(byte[] skeleton) {
        this.skeleton = skeleton;
    }

    public byte[] getSkin() {
        return skin;
    }
    public void setSkin(byte[] skin) {
        this.skin = skin;
    }

    public byte[] getConfig() {
        return config;
    }
    public void setConfig(byte[] config) {
        this.config = config;
    }

    public byte[] getInterfaceData() {
        return interfaceData;
    }
    public void setInterfaceData(byte[] interfaceData) {
        this.interfaceData = interfaceData;
    }

    public byte[] getLandscape() {
        return landscape;
    }
    public void setLandscape(byte[] landscape) {
        this.landscape = landscape;
    }

    public byte[] getModels() {
        return models;
    }
    public void setModels(byte[] models) {
        this.models = models;
    }

    public byte[] getSprites() {
        return sprites;
    }
    public void setSprites(byte[] sprites) {
        this.sprites = sprites;
    }

    public byte[] getTexture() {
        return texture;
    }
    public void setTexture(byte[] texture) {
        this.texture = texture;
    }

    public byte[] getClientScripts() {
        return clientScripts;
    }
    public void setClientScripts(byte[] clientScripts) {
        this.clientScripts = clientScripts;
    }

    public byte[] getFonts() {
        return fonts;
    }
    public void setFonts(byte[] fonts) {
        this.fonts = fonts;
    }
}
