package org.example.duplicateimagechecker;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageComparison {

    private final BufferedImage image1;
    private final BufferedImage image2;

    public ImageComparison(String imagePath1, String imagePath2) {
        // Load the images
        image1 = loadImage(imagePath1);
        image2 = loadImage(imagePath2);
    }

    public boolean compareImages() {
        // Check if the images have the same dimensions
        if (image1 == null || image2 == null) {
            return false;
        }
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return false;
        }

        // Calculate perceptual hashes for images
        long hash1 = calculatePerceptualHash(image1);
        long hash2 = calculatePerceptualHash(image2);

        // Compare hash values
        return hash1 == hash2;
    }

    private BufferedImage loadImage(String imagePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(imagePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

    private long calculatePerceptualHash(BufferedImage image) {
        // Convert image to grayscale and resize
        image = resizeImage(image);

        // Calculate hash value
        long hash = 0;
        int averagePixelValue = calculateAveragePixelValue(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                hash <<= 1;
                if ((image.getRGB(x, y) & 0xFF) >= averagePixelValue) {
                    hash |= 1;
                }
            }
        }
        return hash;
    }

    private BufferedImage resizeImage(BufferedImage originalImage) {
        BufferedImage resizedImage = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        resizedImage.getGraphics().drawImage(originalImage.getScaledInstance(8, 8, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return resizedImage;
    }

    private int calculateAveragePixelValue(BufferedImage image) {
        long totalPixelValue = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                totalPixelValue += image.getRGB(x, y) & 0xFF;
            }
        }
        return (int) (totalPixelValue / (image.getWidth() * image.getHeight()));
    }
}
