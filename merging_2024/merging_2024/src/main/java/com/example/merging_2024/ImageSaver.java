package com.example.merging_2024;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ImageSaver {
    public static void saveImage(BufferedImage image, String filePath, String formatName) {

        try {
            File outputFile = new File(filePath);







            ImageIO.write(image, formatName, outputFile);
            System.out.println("Image saved successfully to: " + filePath);
        } catch (IOException e) {
            System.out.println("Error saving image: " + e.getMessage());
        }
    }


}