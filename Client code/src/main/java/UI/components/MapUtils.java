package UI.components;


import helpers.utils.MapChunk;
import javafx.scene.control.TextField;

public class MapUtils {
    public static String toHexString(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static MapChunk parseChunksTF(TextField chunksToLoad, TextField planesToLoad) {
        // Get the text from the fields and trim whitespace
        String chunksText = chunksToLoad.getText().trim();
        String planesText = planesToLoad.getText().trim();

        if (chunksText.isEmpty() || planesText.isEmpty()) {
            // Return "50-50" as the chunk and "0" as the plane if any field is empty
            return new MapChunk(new String[]{"50-50"}, "0");
        }

        // Split by commas with optional spaces, and handle quoted chunks
        String[] chunks = chunksText.split("\\s*,\\s*");
        String[] planes = planesText.split("\\s*,\\s*");

        // Remove quotes around chunks if they exist
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = chunks[i].replaceAll("^\"|\"$", "");  // Remove leading and trailing quotes
        }

        // Create and return a MapChunk object
        return new MapChunk(chunks, planes);
    }
}
