package com.organizer.platform.service.JMS;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class MediaProcessor {
    private static final int MAX_LONG_EDGE = 1568;
    private static final int MIN_EDGE = 200;
    private static final double MAX_MEGAPIXELS = 1.15;

    public String processAndConvertImageFromBytes(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
            BufferedImage originalImage = ImageIO.read(bis);
            if (originalImage == null) {
                throw new IOException("Failed to read image from byte array");
            }
            return processImage(originalImage);
        }
    }

    private String processImage(BufferedImage originalImage) throws IOException {
        // Get dimensions
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions
        int[] newDimensions = calculateOptimalDimensions(originalWidth, originalHeight);
        int newWidth = newDimensions[0];
        int newHeight = newDimensions[1];

        // Skip resizing if image is already optimal
        if (newWidth == originalWidth && newHeight == originalHeight) {
            return convertToBase64(originalImage);
        }

        // Resize image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Use better quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return convertToBase64(resizedImage);
    }

    private int[] calculateOptimalDimensions(int width, int height) {
        double aspectRatio = (double) width / height;
        int newWidth = width;
        int newHeight = height;

        // Check if image exceeds maximum long edge
        if (width > MAX_LONG_EDGE || height > MAX_LONG_EDGE) {
            if (width > height) {
                newWidth = MAX_LONG_EDGE;
                newHeight = (int) (MAX_LONG_EDGE / aspectRatio);
            } else {
                newHeight = MAX_LONG_EDGE;
                newWidth = (int) (MAX_LONG_EDGE * aspectRatio);
            }
        }

        // Check if image exceeds maximum megapixels
        double megapixels = (newWidth * newHeight) / 1_000_000.0;
        if (megapixels > MAX_MEGAPIXELS) {
            double scale = Math.sqrt(MAX_MEGAPIXELS / megapixels);
            newWidth = (int) (newWidth * scale);
            newHeight = (int) (newHeight * scale);
        }

        // Ensure minimum dimensions
        if (newWidth < MIN_EDGE) {
            newWidth = MIN_EDGE;
            newHeight = (int) (MIN_EDGE / aspectRatio);
        }
        if (newHeight < MIN_EDGE) {
            newHeight = MIN_EDGE;
            newWidth = (int) (MIN_EDGE * aspectRatio);
        }

        return new int[]{newWidth, newHeight};
    }

    private String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
