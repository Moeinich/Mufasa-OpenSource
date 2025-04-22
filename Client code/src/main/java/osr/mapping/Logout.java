package osr.mapping;

import helpers.Logger;
import helpers.Color.TemplateMatcher;
import osr.utils.ImageUtils;
import scripts.APIClasses.ClientAPI;
import scripts.APIClasses.ConditionAPI;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Logout {
    private final Logger logger;
    private final GameTabs gameTabs;
    private final ClientAPI clientAPI;
    private final ConditionAPI conditionAPI;
    private final Login login;
    private final TemplateMatcher templateMatcher;
    private final int threshold = 10;

    private static BufferedImage logoutButtonPng;
    private static BufferedImage exitButtonPng;

    private static final Rectangle oneStartLogoutRatingRect = new Rectangle(609, 291, 23, 26);
    private static final Rectangle twoStartLogoutRatingRect = new Rectangle(645, 290, 24, 27);
    private static final Rectangle threeStartLogoutRatingRect = new Rectangle(682, 291, 24, 27);
    private static final Rectangle fourStartLogoutRatingRect = new Rectangle(719, 290, 23, 28);
    private static final Rectangle fiveStartLogoutRatingRect = new Rectangle(757, 291, 22, 27);

    private static final Rectangle[] ratingRects = {
            oneStartLogoutRatingRect,
            twoStartLogoutRatingRect,
            threeStartLogoutRatingRect,
            fourStartLogoutRatingRect,
            fiveStartLogoutRatingRect
    };

    private static final String[] ratings = {
            "one-star rating",
            "two-star rating",
            "three-star rating",
            "four-star rating",
            "five-star rating"
    };

    public Logout(TemplateMatcher templateMatcher, Logger logger, GameTabs gameTabs, ClientAPI clientAPI, ConditionAPI conditionAPI, Login login, ImageUtils imageUtils) {
        this.logger = logger;
        this.gameTabs = gameTabs;
        this.clientAPI = clientAPI;
        this.conditionAPI = conditionAPI;
        this.login = login;
        this.templateMatcher = templateMatcher;

        logoutButtonPng = imageUtils.pathToBuffered("/osrsAssets/Loginout/logoutbutton.png");
        exitButtonPng = imageUtils.pathToBuffered("/osrsAssets/Loginout/exitworldswitcher.png");
    }

    public void logout(String device, boolean fromHop) {
        if (!login.isLoggedIn(device)) {
            logger.devLog("User is already logged out.");
        } else {
            logger.devLog("Running logout sequence");

            // Check and interact with logout tab if needed
            gameTabs.openTab(device, "Logout");

            Rectangle logoutOption = findLogoutOption(device);

            if (logoutOption != null) {
                // Add a 5% chance to tap a random logout rating rectangle if fromHop is false
                if (!fromHop && Math.random() < 0.05) {
                    // Pick a random index
                    int randomIndex = (int) (Math.random() * ratingRects.length);
                    Rectangle randomRect = ratingRects[randomIndex];
                    String rating = ratings[randomIndex];

                    logger.devLog("[AntiBan]: Giving rating: " + rating);
                    clientAPI.tap(randomRect);
                    conditionAPI.sleep(300, 500);
                }
                // Tap within the logout option with margin
                clientAPI.tap(logoutOption);
                conditionAPI.wait(() -> isLoggedOut(device), 500, 50);
            } else {
                logger.devLog("Logout option not found, checking if we're on the world switching menu...");

                Rectangle exitWorldSwitcher = findExitWorldswitcherOption(device);

                if (exitWorldSwitcher != null) {
                    logger.devLog("We are on the world switching menu... Processing alternate logout!");
                    clientAPI.tap(exitWorldSwitcher);
                    conditionAPI.sleep(1200);

                    Rectangle logoutOption2 = findLogoutOption(device);

                    if (logoutOption2 != null) {
                        logger.devLog("Located the logout button after exiting the world switching menu!");
                        // Add a 5% chance to tap a random logout rating rectangle if fromHop is false
                        if (!fromHop && Math.random() < 0.05) {
                            // Pick a random index
                            int randomIndex = (int) (Math.random() * ratingRects.length);
                            Rectangle randomRect = ratingRects[randomIndex];
                            String rating = ratings[randomIndex];

                            logger.devLog("[AntiBan]: Giving rating: " + rating);
                            clientAPI.tap(randomRect);
                            conditionAPI.sleep(300, 500);
                        }
                        clientAPI.tap(logoutOption2);
                        conditionAPI.wait(() -> isLoggedOut(device), 500, 50);
                    }

                }
            }

            if (isLoggedOut(device)) {
                logger.devLog("Logged out successfully.");
            } else {
                logger.devLog("We couldn't locate both the logout button, and the world switcher menu logout button. Failed to run logout sequence.");
            }
        }
    }

    public Boolean isLoggedOut(String device) {
        return login.isLoggedOut(device);
    }

    public Rectangle findLogoutOption(String device) {
        Rectangle result = templateMatcher.match(device, logoutButtonPng, threshold);

        if (result == null) {
            logger.devLog("The logout button was not found");
        }

        return result; // Returns the found Rectangle or null if no image is found
    }

    public Rectangle findExitWorldswitcherOption(String device) {
        Rectangle result = templateMatcher.match(device, exitButtonPng, threshold);

        if (result == null) {
            logger.devLog("The exit world switcher button was not found");
        }

        return result; // Returns the found Rectangle or null if no image is found
    }
}
