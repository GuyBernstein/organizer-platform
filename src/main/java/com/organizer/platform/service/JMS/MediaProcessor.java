package com.organizer.platform.service.JMS;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * MediaProcessor handles image processing operations, specifically for optimizing images
 * before they are sent to AI services. It provides functionality to resize images while
 * maintaining aspect ratios and quality constraints.
 *
 * Key constraints:
 * - Maximum long edge: 1568 pixels
 * - Minimum edge: 200 pixels
 * - Maximum resolution: 1.15 megapixels
 */
public class MediaProcessor {
    // Maximum length allowed for the longer edge of the image
    private static final int MAX_LONG_EDGE = 1568;
    // Minimum length allowed for any edge of the image
    private static final int MIN_EDGE = 200;
    // Maximum allowed resolution in megapixels (width * height / 1,000,000)
    private static final double MAX_MEGAPIXELS = 1.15;

    /**
     * Processes an image from a byte array and converts it to a Base64 string.
     * This is the main entry point for image processing from external sources.
     *
     * @param imageBytes The raw image data as a byte array
     * @return Base64 encoded string of the processed image
     * @throws IOException If there are issues reading or processing the image
     */
    public String processAndConvertImageFromBytes(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
            BufferedImage originalImage = ImageIO.read(bis);
            if (originalImage == null) {
                throw new IOException("Failed to read image from byte array");
            }
            return processImage(originalImage);
        }
    }

    /**
     * Processes a BufferedImage by resizing it if necessary and converting to Base64.
     * The method applies high-quality rendering settings when resizing.
     *
     * @param originalImage The original BufferedImage to process
     * @return Base64 encoded string of the processed image
     * @throws IOException If there are issues processing or encoding the image
     */
    private String processImage(BufferedImage originalImage) throws IOException {
        // Get dimensions
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions based on constraints
        int[] newDimensions = calculateOptimalDimensions(originalWidth, originalHeight);
        int newWidth = newDimensions[0];
        int newHeight = newDimensions[1];

        // Skip resizing if image is already optimal
        if (newWidth == originalWidth && newHeight == originalHeight) {
            return convertToBase64(originalImage);
        }

        // Create new image with optimal dimensions
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Configure high-quality rendering settings
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Perform the resize operation
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return convertToBase64(resizedImage);
    }

    /**
     * Calculates optimal dimensions for the image based on defined constraints.
     * The method maintains aspect ratio while ensuring:
     * 1. Neither dimension exceeds MAX_LONG_EDGE
     * 2. Total resolution doesn't exceed MAX_MEGAPIXELS
     * 3. Neither dimension is smaller than MIN_EDGE
     *
     * @param width Original width of the image
     * @param height Original height of the image
     * @return int array containing [newWidth, newHeight]
     */
    private int[] calculateOptimalDimensions(int width, int height) {
        double aspectRatio = (double) width / height;
        int newWidth = width;
        int newHeight = height;

        // Step 1: Scale down if any dimension exceeds maximum allowed edge length
        if (width > MAX_LONG_EDGE || height > MAX_LONG_EDGE) {
            if (width > height) {
                newWidth = MAX_LONG_EDGE;
                newHeight = (int) (MAX_LONG_EDGE / aspectRatio);
            } else {
                newHeight = MAX_LONG_EDGE;
                newWidth = (int) (MAX_LONG_EDGE * aspectRatio);
            }
        }

        // Step 2: Scale down if total resolution exceeds maximum allowed megapixels
        double megapixels = (newWidth * newHeight) / 1_000_000.0;
        if (megapixels > MAX_MEGAPIXELS) {
            double scale = Math.sqrt(MAX_MEGAPIXELS / megapixels);
            newWidth = (int) (newWidth * scale);
            newHeight = (int) (newHeight * scale);
        }

        // Step 3: Ensure dimensions don't fall below minimum edge length
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

    /**
     * Converts a BufferedImage to a Base64 encoded string.
     * The image is encoded as a JPG format before conversion.
     *
     * @param image The BufferedImage to convert
     * @return Base64 encoded string representation of the image
     * @throws IOException If there are issues writing or encoding the image
     */
    private String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}