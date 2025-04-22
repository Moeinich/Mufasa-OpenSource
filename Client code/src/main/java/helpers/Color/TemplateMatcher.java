package helpers.Color;

import helpers.GetGameView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TemplateMatcher {
    private final GetGameView getGameView;

    public TemplateMatcher(GetGameView getGameView) {
        this.getGameView = getGameView;
    }

    // Match inputs without alpha values
    public Rectangle match(String device, BufferedImage imageToFind, Rectangle area, int colorTolerance) {
        List<Rectangle> matches = matchTemplate(imageToFind, getGameView.getSubBuffered(device, area), colorTolerance, false, false);
        return (matches.isEmpty()) ? null : matches.get(0);
    }

    public Rectangle match(String device, BufferedImage imageToFind, int colorTolerance) {
        List<Rectangle> matches = matchTemplate(imageToFind, getGameView.getBuffered(device), colorTolerance, false, false);
        return (matches.isEmpty()) ? null : matches.get(0);
    }

    public List<Rectangle> match(String device, BufferedImage imageToFind, Rectangle area, int colorTolerance, boolean findAllMatches) {
        return matchTemplate(imageToFind, getGameView.getSubBuffered(device, area), colorTolerance, findAllMatches, false);
    }

    public List<Rectangle> match(String device, BufferedImage imageToFind, int colorTolerance, boolean findAllMatches) {
        return matchTemplate(imageToFind, getGameView.getBuffered(device), colorTolerance, findAllMatches, false);
    }

    // Match inputs with alpha values
    public Rectangle matchAlpha(String device, BufferedImage imageToFind, Rectangle area, int colorTolerance) {
        List<Rectangle> matches = matchTemplate(imageToFind, getGameView.getSubBuffered(device, area), colorTolerance, false, true);
        return (matches.isEmpty()) ? null : matches.get(0);
    }

    public Rectangle matchAlpha(String device, BufferedImage imageToFind, int colorTolerance) {
        List<Rectangle> matches = matchTemplate(imageToFind, getGameView.getBuffered(device), colorTolerance, false, true);
        return (matches.isEmpty()) ? null : matches.get(0);
    }

    public List<Rectangle> matchAlpha(String device, BufferedImage imageToFind, Rectangle area, int colorTolerance, boolean findAllMatches) {
        return matchTemplate(imageToFind, getGameView.getSubBuffered(device, area), colorTolerance, findAllMatches, true);
    }

    public List<Rectangle> matchAlpha(String device, BufferedImage imageToFind, int colorTolerance, boolean findAllMatches) {
        return matchTemplate(imageToFind, getGameView.getBuffered(device), colorTolerance, findAllMatches, true);
    }

    private List<Rectangle> matchTemplate(BufferedImage imageToFind, BufferedImage imageToSearch, int tolerance, boolean findAllMatches, boolean useAlpha) {
        if (imageToFind == null || imageToSearch == null) {
            return new ArrayList<>();
        }

        imageToFind = useAlpha ? convertToARGB(imageToFind) : convertToRGB(imageToFind);
        imageToSearch = useAlpha ? convertToARGB(imageToSearch) : convertToRGB(imageToSearch);

        int imageWidth = imageToSearch.getWidth();
        int imageHeight = imageToSearch.getHeight();
        int templateWidth = imageToFind.getWidth();
        int templateHeight = imageToFind.getHeight();

        int[] imageData = new int[imageWidth * imageHeight];
        int[] templateData = new int[templateWidth * templateHeight];

        imageToSearch.getRGB(0, 0, imageWidth, imageHeight, imageData, 0, imageWidth);
        imageToFind.getRGB(0, 0, templateWidth, templateHeight, templateData, 0, templateWidth);

        List<Rectangle> matches = new ArrayList<>();

        for (int y = 0; y <= imageHeight - templateHeight; y++) {
            for (int x = 0; x <= imageWidth - templateWidth; x++) {
                if (isMatch(imageData, templateData, x, y, imageWidth, templateWidth, templateHeight, tolerance, useAlpha)) {
                    matches.add(new Rectangle(x, y, templateWidth, templateHeight));
                    if (!findAllMatches) return matches;
                }
            }
        }
        return matches;
    }

    private boolean isMatch(int[] imageData, int[] templateData, int x, int y, int imageWidth, int templateWidth, int templateHeight, int tolerance, boolean useAlpha) {
        for (int j = 0; j < templateHeight; j++) {
            for (int i = 0; i < templateWidth; i++) {
                int imagePixel = imageData[(y + j) * imageWidth + (x + i)];
                int templatePixel = templateData[j * templateWidth + i];

                int imageRed = (imagePixel >> 16) & 0xFF;
                int imageGreen = (imagePixel >> 8) & 0xFF;
                int imageBlue = imagePixel & 0xFF;

                int templateAlpha = (templatePixel >> 24) & 0xFF;
                int templateRed = (templatePixel >> 16) & 0xFF;
                int templateGreen = (templatePixel >> 8) & 0xFF;
                int templateBlue = templatePixel & 0xFF;

                if (useAlpha && templateAlpha == 0) continue;

                if (Math.abs(imageRed - templateRed) > tolerance ||
                        Math.abs(imageGreen - templateGreen) > tolerance ||
                        Math.abs(imageBlue - templateBlue) > tolerance) {
                    return false;
                }
            }
        }
        return true;
    }

    private BufferedImage convertToRGB(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.getGraphics().drawImage(image, 0, 0, null);
            return rgbImage;
        }
        return image;
    }

    private BufferedImage convertToARGB(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            BufferedImage argbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argbImage.getGraphics().drawImage(image, 0, 0, null);
            return argbImage;
        }
        return image;
    }
}
