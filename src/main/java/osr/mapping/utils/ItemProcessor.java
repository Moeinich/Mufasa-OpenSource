package osr.mapping.utils;

import helpers.CacheManager;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import utils.SystemUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ItemProcessor {
    private final String folderPath = SystemUtils.getItemsFolderPath() + File.separator + "item-images"; // Path to the folder
    private final CacheManager cacheManager;

    public ItemProcessor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Mat getItemImage(String itemID) {
        String itemImagePath = folderPath + File.separator + itemID + ".png"; // Full path to the image

        // Check if the image is in the cache
        Mat cachedMat = cacheManager.getItemMat(itemImagePath);
        if (cachedMat != null) {
            return cachedMat;
        }

        // If not in cache, load the image from the file system
        try {
            byte[] buffer = Files.readAllBytes(Paths.get(itemImagePath));
            MatOfByte matOfByte = new MatOfByte(buffer);
            Mat mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
    
            // Cache the loaded image
            cacheManager.setImageMat(itemImagePath, mat);
    
            return mat;
        } catch (IOException e) {
            System.err.println("Error accessing image file: " + e.getMessage());
            return new Mat();
        }
    }

    public Mat getItemImage(int itemID) {
        String itemImagePath = folderPath + File.separator + itemID + ".png"; // Full path to the image

        // Check if the image is in the cache
        Mat cachedMat = cacheManager.getItemMat(itemImagePath);
        if (cachedMat != null) {
            return cachedMat;
        }
    
        // If not in cache, load the image from the file system
        try {
            byte[] buffer = Files.readAllBytes(Paths.get(itemImagePath));
            MatOfByte matOfByte = new MatOfByte(buffer);
            Mat mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
    
            // Cache the loaded image
            cacheManager.setImageMat(itemImagePath, mat);
    
            return mat;
        } catch (IOException e) {
            System.err.println("Error accessing image file: " + e.getMessage());
            return new Mat();
        }
    }

    public Image getItemImageFX(String itemID) {
        String itemImagePath = folderPath + File.separator + itemID + ".png"; // Full path to the image file

        try {
            File imageFile = new File(itemImagePath);
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (IOException e) {
            System.err.println("Error accessing image file: " + e.getMessage());
            return null;
        }
    }

    public Image getItemImageFX(int itemID) {
        String itemImagePath = folderPath + File.separator + itemID + ".png"; // Full path to the image file

        try {
            File imageFile = new File(itemImagePath);
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (IOException e) {
            System.err.println("Error accessing image file: " + e.getMessage());
            return null;
        }
    }
}




