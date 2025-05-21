package osr.utils;

import helpers.CacheManager;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import scripts.ScriptInfo;
import scripts.ScriptResourceManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    private final ScriptInfo scriptInfo;
    private final ScriptResourceManager scriptResourceManager;
    private final CacheManager cacheManager;

    public ImageUtils(ScriptInfo scriptInfo, ScriptResourceManager scriptResourceManager, CacheManager cacheManager) {
        this.scriptInfo = scriptInfo;
        this.scriptResourceManager = scriptResourceManager;
        this.cacheManager = cacheManager;
    }

    public Mat pathToMat(String path) {
        // Check the cache first
        Mat cachedMat = cacheManager.getImageStringMat(path);
        if (cachedMat != null) {
            return cachedMat;
        }
    
        try {
            // Check if the file exists on the local file system
            File file = new File("/" + path);
            if (file.exists()) {
                // Read the image from file system and return as Mat
                Mat image = Imgcodecs.imread(path, Imgcodecs.IMREAD_UNCHANGED);
                if (!image.empty()) {
                    // Cache the loaded image
                    cacheManager.setImageStringMat(path, image);
                    return image;
                } else {
                    System.err.println("Absolute image found but unable to read: " + path);
                }
            }
    
            // Remove the leading slash if there is one in the path, needed for the below methods
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
    
            // Try loading the image as a resource from the project
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {

                if (in != null) { // Check if the image is an internal client image
                    byte[] buffer = in.readAllBytes();
                    Mat encoded = new Mat(1, buffer.length, CvType.CV_8U);
                    encoded.put(0, 0, buffer);
                    Mat image = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_UNCHANGED);

                    // Cache the loaded image
                    cacheManager.setImageStringMat(path, image);

                    return image;
                } else { // Check if the image is within our script
                    Image image = scriptResourceManager.getResourceImageFromCurrentScript(scriptInfo.getCurrentEmulatorId(), path);
                    if (image != null) {
                        int width = (int) image.getWidth();
                        int height = (int) image.getHeight();
                        byte[] buffer = new byte[4 * width * height];
                        PixelReader reader = image.getPixelReader();
                        WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraInstance();
                        reader.getPixels(0, 0, width, height, format, buffer, 0, width * 4);

                        Mat mat = new Mat(height, width, CvType.CV_8UC4);
                        mat.put(0, 0, buffer);

                        // Cache the loaded image
                        cacheManager.setImageStringMat(path, mat);

                        return mat;
                    } else {
                        throw new IllegalArgumentException("Image not found: " + path);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            return new Mat();
        }
    }

    public BufferedImage pathToBuffered(String path) {
        try {
            // Check if the file exists on the local file system
            File file = new File("/" + path);
            if (file.exists()) {
                // Read the image from file system and return as BufferedImage
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    return image;
                } else {
                    System.err.println("Absolute image found but unable to read: " + path);
                }
            }

            // Remove the leading slash if there is one in the path, needed for the below methods
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // Try loading the image as a resource from the project
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
                if (in != null) { // Check if the image is an internal client image
                    BufferedImage image = ImageIO.read(in);
                    if (image != null) {
                        return image;
                    }
                } else { // Check if the image is within our script
                    Image fxImage = scriptResourceManager.getResourceImageFromCurrentScript(scriptInfo.getCurrentEmulatorId(), path);
                    if (fxImage != null) {
                        int width = (int) fxImage.getWidth();
                        int height = (int) fxImage.getHeight();
                        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                        PixelReader reader = fxImage.getPixelReader();
                        WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraInstance();
                        byte[] buffer = new byte[4 * width * height];
                        reader.getPixels(0, 0, width, height, format, buffer, 0, width * 4);

                        int[] pixels = new int[width * height];
                        for (int i = 0; i < pixels.length; i++) {
                            int b = buffer[i * 4] & 0xFF;
                            int g = buffer[i * 4 + 1] & 0xFF;
                            int r = buffer[i * 4 + 2] & 0xFF;
                            int a = buffer[i * 4 + 3] & 0xFF;
                            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                        }

                        bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);
                        return bufferedImage;
                    } else {
                        throw new IllegalArgumentException("Image not found: " + path);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
        }

        return null;
    }

    public Image pathToJavaFXImage(String path) {
        // Check the cache first
        Image cachedImage = cacheManager.getImageStringFX(path);
        if (cachedImage != null) {
            return cachedImage;
        }

        try {
            // Check if the file exists on the local file system
            File file = new File("/" + path);
            if (file.exists()) {
                // Load the image from the file system
                Image image = new Image(file.toURI().toString());
                if (image.getWidth() > 0 && image.getHeight() > 0) {
                    // Cache the loaded image
                    cacheManager.setImageStringFX(path, image);
                    return image;
                } else {
                    System.err.println("Absolute image found but unable to load: " + path);
                }
            }

            // Remove the leading slash if there is one in the path
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // Try loading the image as a resource from the project
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
                if (in != null) {
                    // Load the image from the input stream
                    Image image = new Image(in);
                    if (image.getWidth() > 0 && image.getHeight() > 0) {
                        // Cache the loaded image
                        cacheManager.setImageStringFX(path, image);
                        return image;
                    } else {
                        System.err.println("Resource image found but unable to load: " + path);
                    }
                } else {
                    // Check if the image is within our script resources
                    Image scriptImage = scriptResourceManager.getResourceImageFromCurrentScript(
                            scriptInfo.getCurrentEmulatorId(), path
                    );
                    if (scriptImage != null) {
                        // Cache the loaded image
                        cacheManager.setImageStringFX(path, scriptImage);
                        return scriptImage;
                    } else {
                        throw new IllegalArgumentException("Image not found: " + path);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            return null; // Return null to signify the failure
        }
        return null;
    }

    public Mat convertFXImageToMat(Image fxImage) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

        if (bufferedImage == null) {
            return new Mat();
        }

        BufferedImage convertedImg = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(bufferedImage, 0, 0, null);

        byte[] pixels = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);

        return mat;
    }

    public AbstractMap.SimpleEntry<Mat, Mat> convertToColorWithAlpha(Mat image) {
        if (image.channels() != 4) {
            return new AbstractMap.SimpleEntry<>(image, null); // Return the image as-is if it doesn't have an alpha channel
        }

        List<Mat> channels = new ArrayList<>();
        Core.split(image, channels);

        Mat bgr = new Mat();
        Mat alphaChannel = channels.get(3); // Extract the alpha channel

        Core.merge(channels.subList(0, 3), bgr); // Merge the BGR channels

        return new AbstractMap.SimpleEntry<>(bgr, alphaChannel);
    }

    public Mat bufferedImageToMat(BufferedImage bufferedImage) {
        // If the image is already in TYPE_3BYTE_BGR format, no conversion is necessary
        BufferedImage imageToProcess = bufferedImage;
        if (bufferedImage.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            // Only convert if the type is not TYPE_3BYTE_BGR
            imageToProcess = new BufferedImage(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = imageToProcess.createGraphics();
            g.drawImage(bufferedImage, 0, 0, null);
            g.dispose();
        }

        // Extract byte array from the BufferedImage
        byte[] imageData = ((DataBufferByte) imageToProcess.getRaster().getDataBuffer()).getData();

        // Create an OpenCV Mat and copy image data into it
        Mat mat = new Mat(imageToProcess.getHeight(), imageToProcess.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, imageData);

        return mat;
    }

    // Convert OpenCV Mat to JavaFX Image
    public Image matToImage(Mat mat) {
        if (mat == null) {
            return null;
        }

        BufferedImage bImage;
        int cols = mat.cols();
        int rows = mat.rows();
        int elemSize = (int) mat.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        mat.get(0, 0, data);
        int type;

        switch (mat.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // Convert BGR to RGB
                byte b;
                for (int i = 0; i < data.length; i = i + 3) {
                    b = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = b;
                }
                break;
            case 4:
                type = BufferedImage.TYPE_4BYTE_ABGR; // Notice the type to handle alpha
                // Convert BGRA to RGBA
                byte tmp;
                for (int i = 0; i < data.length; i = i + 4) {
                    tmp = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = tmp;
                }
                break;
            default:
                return null; // Unsupported format
        }

        bImage = new BufferedImage(cols, rows, type);
        bImage.getRaster().setDataElements(0, 0, cols, rows, data);
        return SwingFXUtils.toFXImage(bImage, null);
    }
}