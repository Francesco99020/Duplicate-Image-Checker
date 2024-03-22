package org.example.duplicateimagechecker;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageComparison {

    private BufferedImage image1;
    private BufferedImage image2;

    public ImageComparison(String imagePath1, String imagePath2) {
        // Load the images
        image1 = loadImage(imagePath1);
        image2 = loadImage(imagePath2);
    }

    public boolean compareImages() {
        // Check if the images have the same dimensions
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return false;
        }

        // Iterate over each pixel and compare
        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image1.getWidth(); x++) {
                // Compare pixel values
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    return false; // Images are different
                }
            }
        }

        return true; // Images are identical
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
}
