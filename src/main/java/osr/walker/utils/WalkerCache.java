package osr.walker.utils;

import org.opencv.core.Core;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class WalkerCache {
    private final List<Core.MinMaxLocResult> lastTopMatches = new ArrayList<>();
    public Point lastFoundPosition;
    public double lastConfidence;

    public boolean isSameAsLastSearch(List<Core.MinMaxLocResult> newMatches) {
        if (lastTopMatches.size() != newMatches.size()) {
            return false;
        }
        for (int i = 0; i < newMatches.size(); i++) {
            if (lastTopMatches.get(i).maxLoc.x != newMatches.get(i).maxLoc.x ||
                    lastTopMatches.get(i).maxLoc.y != newMatches.get(i).maxLoc.y) {
                return false;
            }
        }
        return true;
    }

    public void updateCache(List<Core.MinMaxLocResult> newMatches, Point newPosition, double newConfidence) {
        lastTopMatches.clear();
        lastTopMatches.addAll(newMatches);
        lastFoundPosition = newPosition;
        lastConfidence = newConfidence;
    }
}
