package helpers;

import UI.components.PaintBar;
import helpers.services.BreakHandlerService;
import helpers.services.RuntimeService;
import helpers.services.XPService;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import scripts.ScriptAccountManager;
import scripts.ScriptInfo;
import utils.CredentialsManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordWebhook {
    private final Logger logger;
    private final GetGameView getGameView;
    private final HttpClient httpClient;
    private final ScriptInfo scriptInfo;
    private final ScriptAccountManager scriptAccountManager;
    private final RuntimeService runtimeService;
    private final CredentialsManager credMgr;
    private final XPService xpService;
    private final BreakHandlerService breakHandlerService;
    private final PaintBar paintBar;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> webhookTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = ThreadManager.getInstance().getScheduler();
    private final ConcurrentHashMap<String, AtomicBoolean> pauseFlags;

    public DiscordWebhook(Logger logger, GetGameView getGameView, ScriptInfo scriptInfo, ScriptAccountManager scriptAccountManager, RuntimeService runtimeService, CredentialsManager credMgr, XPService xpService, BreakHandlerService breakHandlerService, PaintBar paintBar) {
        this.logger = logger;
        this.getGameView = getGameView;
        this.scriptInfo = scriptInfo;
        this.scriptAccountManager = scriptAccountManager;
        this.runtimeService = runtimeService;
        this.credMgr = credMgr;
        this.xpService = xpService;
        this.breakHandlerService = breakHandlerService;
        this.paintBar = paintBar;

        this.httpClient = HttpClient.newHttpClient();
        this.pauseFlags = new ConcurrentHashMap<>();
    }

    public String formatDurationLong(long millis) {
        // Constants for time conversions
        long oneSecond = 1000;
        long oneMinute = oneSecond * 60;
        long oneHour = oneMinute * 60;
        long oneDay = oneHour * 24;

        // Calculate days, hours, minutes, and seconds
        long days = millis / oneDay;
        millis %= oneDay;
        long hours = millis / oneHour;
        millis %= oneHour;
        long minutes = millis / oneMinute;
        millis %= oneMinute;
        long seconds = millis / oneSecond;

        // Building the formatted time string
        StringBuilder sb = new StringBuilder();
        boolean previousComponent = false;

        if (days > 0) {
            sb.append(days).append("d");
            previousComponent = true;
        }
        if (hours > 0) {
            if (previousComponent) {
                sb.append(", ");
            }
            sb.append(hours).append("h");
            previousComponent = true;
        }
        if (minutes > 0) {
            if (previousComponent) {
                sb.append(", ");
            }
            sb.append(minutes).append("m");
            previousComponent = true;
        }

        if (seconds > 0 || (!previousComponent)) {
            if (previousComponent) {
                sb.append(" and ");
            }
            if (!previousComponent) {
                sb.append(seconds).append(" seconds");
            } else {
                sb.append(seconds).append("s");
            }
        }

        return sb.toString();
    }

    public void sendStartMessage(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            String jsonPayload = "{"
                    + "\"username\": \"Mufasa - " + scriptInfo.getCurrentEmulatorId() + "\","
                    + "\"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\","
                    + "\"embeds\": ["
                    + "  {"
                    + "    \"title\": \"Script started\","
                    + "    \"description\": \"A script is now running with the below information:\","
                    + "    \"color\": 65280,"  // Green color in decimal for 'start'
                    + "    \"fields\": ["
                    + "      {"
                    + "        \"name\": \"Emulator\","
                    + "        \"value\": \"" + device + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Script\","
                    + "        \"value\": \"" + scriptInfo.getScriptManifest(device).name() + "\","
                    + "        \"inline\": true"
                    + "      }"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.devLog("Start notification sent successfully");
            } catch (Exception e) {
                logger.devLog("Failed to send start notification: " + e.getMessage());
            }
        }
    }

    public void sendStopMessage(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            long elapsedTime = runtimeService.getExistingHandler(device).getElapsedTime();
            String formattedTime = formatDurationLong(elapsedTime);
            int xp = xpService.getXP(device);
            String formattedXP = formatXP(xp);  // Apply custom formatting for XP
            String jsonPayload = "{"
                    + "\"username\": \"Mufasa - " + device + "\","
                    + "\"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\","
                    + "\"embeds\": ["
                    + "  {"
                    + "    \"title\": \"Script stopped\","
                    + "    \"description\": \"The " + scriptInfo.getScriptManifest(device).name() + " script has stopped, final results:\","
                    + "    \"color\": 16711680,"
                    + "    \"fields\": ["
                    + "      {"
                    + "        \"name\": \"Emulator\","
                    + "        \"value\": \"" + device + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"\","
                    + "        \"value\": \"\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Runtime\","
                    + "        \"value\": \"" + formattedTime + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"XP Gain\","
                    + "        \"value\": \"" + formattedXP + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"\","
                    + "        \"value\": \"\","
                    + "        \"inline\": true"
                    + "      }"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.devLog("Stop notification sent successfully");
            } catch (Exception e) {
                logger.devLog("Failed to send stop notification: " + e.getMessage());
            }
        }
    }

    public void sendStartBreakMessage(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            long elapsedTime = runtimeService.getExistingHandler(device).getElapsedTime();
            long breakTime = breakHandlerService.getHandlerForEmulator(device).getTimeUntilNextEvent();
            breakTime = (breakTime * 1000); // Convert seconds to milliseconds
            String formattedTime = formatDurationLong(elapsedTime);
            String formattedBreakTime = formatDurationLong(breakTime);
            String jsonPayload = "{"
                    + "\"username\": \"Mufasa - " + device + "\","
                    + "\"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\","
                    + "\"embeds\": ["
                    + "  {"
                    + "    \"title\": \"Break started\","
                    + "    \"description\": \"A script has started a break with the following details:\","
                    + "    \"color\": 15258703,"
                    + "    \"fields\": ["
                    + "      {"
                    + "        \"name\": \"Emulator\","
                    + "        \"value\": \"" + device + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Script\","
                    + "        \"value\": \"" + scriptInfo.getScriptManifest(device).name() + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"\","
                    + "        \"value\": \"\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Run Duration\","
                    + "        \"value\": \"" + formattedTime + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Break Duration\","
                    + "        \"value\": \"" + formattedBreakTime + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"\","
                    + "        \"value\": \"\","
                    + "        \"inline\": true"
                    + "      }"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.devLog("Start break notification sent successfully");
            } catch (Exception e) {
                logger.devLog("Failed to send start break notification: " + e.getMessage());
            }
        }
    }

    public void sendResumeBreakMessage(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            long elapsedTime = runtimeService.getExistingHandler(device).getElapsedTime();
            String formattedTime = formatDurationLong(elapsedTime);
            String jsonPayload = "{"
                    + "\"username\": \"Mufasa - " + device + "\","
                    + "\"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\","
                    + "\"embeds\": ["
                    + "  {"
                    + "    \"title\": \"Break ended\","
                    + "    \"description\": \"A script has resumed from a break with the following details:\","
                    + "    \"color\": 15258703,"
                    + "    \"fields\": ["
                    + "      {"
                    + "        \"name\": \"Emulator\","
                    + "        \"value\": \"" + device + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Script\","
                    + "        \"value\": \"" + scriptInfo.getScriptManifest(device).name() + "\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Run Duration\","
                    + "        \"value\": \"" + formattedTime + "\","
                    + "        \"inline\": true"
                    + "      }"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.devLog("Resume break notification sent successfully");
            } catch (Exception e) {
                logger.devLog("Failed to send resume break notification: " + e.getMessage());
            }
        }
    }

    public void sendHopMessage(String device, int world) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            long elapsedTime = runtimeService.getExistingHandler(device).getElapsedTime();
            String formattedTime = formatDurationLong(elapsedTime);
            String jsonPayload = String.format("{"
                    + "\"username\": \"Mufasa - %s\","
                    + "\"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\","
                    + "\"embeds\": ["
                    + "  {"
                    + "    \"title\": \"Hopping world\","
                    + "    \"description\": \"A script is now hopping to world %d with the following details:\","
                    + "    \"color\": 15258703,"
                    + "    \"fields\": ["
                    + "      {"
                    + "        \"name\": \"Emulator\","
                    + "        \"value\": \"%s\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Script\","
                    + "        \"value\": \"%s\","
                    + "        \"inline\": true"
                    + "      },"
                    + "      {"
                    + "        \"name\": \"Run Duration\","
                    + "        \"value\": \"%s\","
                    + "        \"inline\": true"
                    + "      }"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}", device, world, device, scriptInfo.getScriptManifest(device).name(), formattedTime);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.devLog("Hop notification sent successfully");
            } catch (Exception e) {
                logger.devLog("Failed to send hop notification: " + e.getMessage());
            }
        }
    }

    public void sendLevelupMessage(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            File tempFile = null;
            try {
                tempFile = convertMatToTempFile(getGameView.getMat(device));
                HttpRequest.BodyPublisher bodyPublisher = ofLevelupMultipartData(device, tempFile, tempFile.getName());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookURL))
                        .header("Content-Type", "multipart/form-data; boundary=123456")
                        .POST(bodyPublisher)
                        .build();
                executeHttpRequest(request);
            } catch (IOException e) {
                logger.devLog("Error processing image for webhook: " + e.getMessage());
            } finally {
                if (tempFile != null) {
                    tempFile.delete();  // Ensure the file is deleted to avoid resource leakage
                }
            }
        }
    }

    public void sendScreenUpdate(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            File tempFile = null;
            try {
                // Get the buffered images
                BufferedImage gameViewImage = getGameView.getBuffered(device);

                BufferedImage paintBarImage = paintBar.getWebhookPaintImage(device);

                BufferedImage combinedImage;
                if (paintBarImage == null) {
                    // No paint bar image, use the game view image only
                    //combinedImage = blurImageAreas(gameViewImage, false);
                    combinedImage = gameViewImage;
                } else {
                    // Combine the images with adjustments for alignment
                    combinedImage = new BufferedImage(1094, 560, BufferedImage.TYPE_INT_RGB);
                    Graphics g = combinedImage.getGraphics();

                    // Set the background color to #100c0c
                    g.setColor(new Color(16, 12, 12)); // #100c0c color
                    g.fillRect(0, 0, 1094, 555); // Fill the entire background with the color

                    // Draw the paint bar image on the left side
                    g.drawImage(paintBarImage, 0, 0, null);

                    // Draw the game view image to the right of the paint bar
                    g.drawImage(gameViewImage, 100, 20, null);

                    // Clean up
                    g.dispose();

                    // Blur images
                    //combinedImage = blurImageAreas(combinedImage, false);
                }

                // Save the combined image to a temporary file
                tempFile = convertBufferedImageToTempFile(combinedImage);

                // Send the combined image as a message
                sendCombinedMessage(device, tempFile);

            } catch (IOException e) {
                e.printStackTrace(); // Print the stack trace for deeper investigation
            } finally {
                if (tempFile != null) {
                    boolean deleted = tempFile.delete();
                    if (!deleted) {
                        System.err.println("Failed to delete temporary file.");
                    }
                }
            }
        }
    }

    private void sendCombinedMessage(String device, File imageFile) throws IOException {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            HttpRequest.BodyPublisher bodyPublisher = ofMimeMultipartData(device, imageFile, imageFile.getName());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .header("Content-Type", "multipart/form-data; boundary=123456")
                    .POST(bodyPublisher)
                    .build();

            executeHttpRequest(request);
        }
    }

    public void startWebhookSchedule(String device) {
        String username = scriptAccountManager.getSelectedAccount(device);
        String webhookURL = credMgr.getWebhookURL(username);

        if (webhookURL != null) {
            AtomicBoolean pauseFlag = pauseFlags.computeIfAbsent(device, k -> new AtomicBoolean(false));

            // Start a new webhook task if one is not already running
            webhookTasks.computeIfAbsent(device, k -> {
                ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
                    if (!pauseFlag.get()) {
                        sendScreenUpdate(device);
                    }
                }, getFrequency(device), getFrequency(device), TimeUnit.SECONDS);

                logger.devLog("Webhook scheduler started for device: " + device);
                return task;
            });
        } else {
            logger.devLog("No webhook URL found for device: " + device);
        }
    }

    public boolean isWebhookTaskRunning(String device) {
        ScheduledFuture<?> task = webhookTasks.get(device);
        return task != null && !task.isCancelled() && !task.isDone();
    }

    public void stopWebhookSchedule(String device) {
        ScheduledFuture<?> task = webhookTasks.get(device);
        if (task != null) {
            task.cancel(false); // Stop the task gracefully
            webhookTasks.remove(device); // Remove from map
            pauseFlags.remove(device);
            logger.devLog("Webhook scheduler stopped for device: " + device);
        } else {
            logger.devLog("No webhook task found for device: " + device);
        }
    }

    public void pauseWebhookSchedule(String device) {
        AtomicBoolean pauseFlag = pauseFlags.get(device);
        if (pauseFlag != null) {
            pauseFlag.set(true);
            logger.devLog("Webhook scheduler paused for device: " + device);
        } else {
            logger.devLog("No scheduler found to pause for device: " + device);
        }
    }

    public void resumeWebhookSchedule(String device) {
        AtomicBoolean pauseFlag = pauseFlags.get(device);
        if (pauseFlag != null) {
            pauseFlag.set(false);
            logger.devLog("Webhook scheduler resumed for device: " + device);
        } else {
            logger.devLog("No scheduler found to resume for device: " + device);
        }
    }

    private HttpRequest.BodyPublisher ofMimeMultipartData(String device, File file, String filename) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        String boundary = "123456";

        String filePart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";
        byteArrays.add(filePart.getBytes());
        byteArrays.add(java.nio.file.Files.readAllBytes(file.toPath()));
        byteArrays.add("\r\n".getBytes());

        String jsonPart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"payload_json\"\r\n" +
                "Content-Type: application/json\r\n\r\n";
        long elapsedTime = runtimeService.getExistingHandler(device).getElapsedTime();
        String formattedTime = formatDurationLong(elapsedTime);

        int xp = xpService.getXP(device);
        String formattedXP = formatXP(xp);  // Apply custom formatting for XP

        String jsonPayload = String.format("{\"username\": \"Mufasa - %s\", \"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\", \"embeds\":[{\"title\": \"Script Update\", \"description\": \"%s\", \"color\": 65280, \"fields\": [{\"name\": \"Emulator\", \"value\": \"%s\", \"inline\": true}, {\"name\": \"Script\", \"value\": \"%s\", \"inline\": true}, {\"name\": \"\", \"value\": \"\", \"inline\": true}, {\"name\": \"XP Gain\", \"value\": \"%s\", \"inline\": true}, {\"name\": \"XP Rate\", \"value\": \"%s/hr\", \"inline\": true}, {\"name\": \"Run Duration\", \"value\": \"%s\", \"inline\": true}], \"image\": {\"url\": \"attachment://%s\"}}]}",
                device, "Current script status update", device, scriptInfo.getScriptManifest(device).name(), formattedXP, xpService.getXPHr(device), formattedTime, filename);
        jsonPart += jsonPayload + "\r\n--" + boundary + "--\r\n";

        byteArrays.add(jsonPart.getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private void executeHttpRequest(HttpRequest request) {
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.devLog("Status update webhook sent successfully");
        } catch (IOException | InterruptedException e) {
            logger.devLog("Failed to send status update webhook: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // set the interrupt flag
            }
        }
    }

    private HttpRequest.BodyPublisher ofLevelupMultipartData(String device, File file, String filename) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        String boundary = "123456";

        String filePart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";
        byteArrays.add(filePart.getBytes());
        byteArrays.add(java.nio.file.Files.readAllBytes(file.toPath()));
        byteArrays.add("\r\n".getBytes());

        String jsonPart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"payload_json\"\r\n" +
                "Content-Type: application/json\r\n\r\n";
        int color = getRandomFireworkColor();
        String accountName = scriptAccountManager.getSelectedAccount(device);
        String scriptName = scriptInfo.getScriptManifest(device).name();
        String jsonPayload = String.format("{\"username\": \"Mufasa - %s\", \"avatar_url\": \"https://wiki.mufasaclient.com/wp-content/uploads/2024/01/cropped-mufasa-transparent.png\", \"embeds\":[{\"title\": \"Levelup!\", \"description\": \"Congratulations on leveling up on %s while running %s on Mufasa.\", \"color\": %d, \"image\": {\"url\": \"attachment://%s\"}}]}",
                device, accountName, scriptName, color, filename);
        jsonPart += jsonPayload + "\r\n--" + boundary + "--\r\n";

        byteArrays.add(jsonPart.getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private File convertMatToTempFile(Mat imageMat) throws IOException {
        // Create a temporary file in the default temporary file directory
        File tempFile = File.createTempFile("tempfile_", ".png");

        // Attempt to write the Mat to the temporary file
        boolean result = Imgcodecs.imwrite(tempFile.getAbsolutePath(), imageMat);
        if (!result) {
            throw new IOException("Failed to save image file: " + tempFile.getAbsolutePath());
        }

        // Mark the file for deletion on exit; this is a fallback in case the manual deletion fails
        tempFile.deleteOnExit();

        return tempFile;
    }

    private File convertBufferedImageToTempFile(BufferedImage image) throws IOException {
        // Create a temporary file in the default temporary file directory
        File tempFile = File.createTempFile("tempfile_", ".png");

        try {
            // Write the BufferedImage to the temporary file
            ImageIO.write(image, "png", tempFile);
        } catch (IOException e) {
            // Ensure the temp file is deleted if writing fails
            tempFile.delete();
            throw new IOException("Failed to save image file: " + tempFile.getAbsolutePath(), e);
        }

        // Mark the file for deletion on exit; this is a fallback in case the manual deletion fails
        tempFile.deleteOnExit();

        return tempFile;
    }

    private String formatXP(int xp) {
        if (xp < 1000) {
            return String.format("%d", xp);  // Display as integer if less than 1000
        } else if (xp < 1000000) {
            return String.format("%.2fk", xp / 1000.0);  // Convert to 'K' format
        } else {
            return String.format("%.2fm", xp / 1000000.0);  // Convert to 'M' format
        }
    }

    public Long getFrequency(String device) {
        try {
            String frequencyStringValue = credMgr.getFrequency(scriptAccountManager.getSelectedAccount(device));
            if (frequencyStringValue != null && !frequencyStringValue.isEmpty()) {
                int frequencyValue = Integer.parseInt(frequencyStringValue.trim()); // Safely trim and parse the integer
                return frequencyValue * 60L; // Convert minutes to seconds and return as Long
            } else {
                return 60L; // Default frequency (1 minute) if not specified
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse frequency: " + e.getMessage());
            return 60L; // Default frequency (1 minute) in case of parsing error
        }
    }

    private int getRandomFireworkColor() {
        int[] colors = new int[]{0xFFFF00, // Yellow
                0x0000FF, // Blue
                0x008000, // Green
                0x800080}; // Purple
        return colors[new java.util.Random().nextInt(colors.length)];
    }

}
