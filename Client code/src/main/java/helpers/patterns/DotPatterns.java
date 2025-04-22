package helpers.patterns;

import java.util.*;

public class DotPatterns {
    // A single shared list of patterns
    private static final List<int[][]> sharedPatterns = new ArrayList<>();

    static {
        // Initialize shared patterns
        addPatterns(
                // Common patterns shared across all objects
                new int[][]{
                        {0, 1, 1, 0},
                        {1, 1, 1, 1},
                        {1, 1, 1, 1},
                        {0, 1, 1, 0}
                },
                new int[][]{
                        {0, 1, 1, 0},
                        {0, 1, 1, 1},
                        {0, 1, 1, 1},
                        {0, 1, 1, 0}
                },
                new int[][]{
                        {1, 1, 1, 1},
                        {1, 1, 1, 1},
                        {1, 1, 1, 1},
                        {1, 1, 1, 1}
                }
        );
    }

    /**
     * Gets all shared patterns.
     *
     * @return a list of all shared patterns.
     */
    public static List<int[][]> get() {
        return Collections.unmodifiableList(sharedPatterns);
    }

    /**
     * Adds multiple patterns to the shared list.
     *
     * @param patterns the patterns to add.
     */
    public static void addPatterns(int[][]... patterns) {
        sharedPatterns.addAll(Arrays.asList(patterns));
    }
}