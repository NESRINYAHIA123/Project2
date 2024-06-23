package com.example.merging_2024;

import java.awt.*;
import java.awt.image.*;
import java.io.IOException;

public class ImageWithText {

    public static BufferedImage addTextToImage(BufferedImage originalImage, String text) throws IOException {

        // Create a new BufferedImage with the same dimensions and type as originalImage
        BufferedImage newImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());

        // Get a Graphics2D object for drawing on the new image
        Graphics2D g2d = newImage.createGraphics();

        // Copy the pixels from originalImage to newImage
        g2d.drawImage(originalImage, 0, 0, null);

        // Set font properties (adjust as needed)
        Font font = new Font("Arial", Font.BOLD, 20); // Font family, style, size
        g2d.setFont(font);

        // Get color for the text
        Color textColor = Color.RED; // Adjust color as desired

        // Calculate text positioning to center it horizontally
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int textX = (newImage.getWidth() - textWidth) / 2;

        // Calculate text positioning for vertical alignment based on font size and baseline
        int ascent = g2d.getFontMetrics().getAscent();
        int textY = (newImage.getHeight() - ascent) / 2;

        // Draw the text on the new image
        g2d.setColor(textColor);
        g2d.drawString(text, textX, textY);

        // Release resources used by the Graphics2D object
        g2d.dispose();

        // Return the new image with text
        return newImage;
    }


}
