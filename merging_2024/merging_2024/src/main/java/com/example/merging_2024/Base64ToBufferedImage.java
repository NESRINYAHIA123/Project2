package com.example.merging_2024;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
public class Base64ToBufferedImage {


    public static BufferedImage base64StringToImage(String base64String) {
        BufferedImage image = null;
        try {
            // Decode the Base64 string into a byte array
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            // Create an InputStream from the byte array
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);

            // Read the image data from the InputStream and create a BufferedImage
            image = ImageIO.read(bis);

            // Close the InputStream
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    /*public static void main(String[] args) {
        // Example Base64 encoded image string
        String base64Image = "your_base64_encoded_image_string_here";

        // Convert Base64 string to BufferedImage
        BufferedImage bufferedImage = base64StringToImage(base64Image);

        // Now you can use the BufferedImage as needed
        // For example, display it:
        if (bufferedImage != null) {
            // Display the image
            // Example: DisplayImage(bufferedImage);
        } else {
            System.out.println("Failed to decode Base64 string.");
        }
    }*/

}
