package helpers.OCR.utils;

/**
 * Enum for available font names.
 *
 * <p>The enum values map to the folder names that contain the BMP font templates.
 * For example, the folder "Bold 12" corresponds to the enum value <code>BOLD_12</code>.</p>
 */
public enum FontName {
    BOLD_12("Bold 12"),
    PLAIN_11("Plain 11"),
    PLAIN_12("Plain 12"),
    QUILL("Quill"),
    QUILL_8("Quill 8"),
    ANY("Any"),
    NONE("None");

    private final String folderName;

    FontName(String folderName) {
        this.folderName = folderName;
    }

    /**
     * Returns the folder name associated with this font.
     *
     * @return the folder name as a String.
     */
    public String getFolderName() {
        return folderName;
    }

    @Override
    public String toString() {
        return folderName;
    }
}
