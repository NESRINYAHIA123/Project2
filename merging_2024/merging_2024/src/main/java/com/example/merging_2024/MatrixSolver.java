
package com.example.merging_2024;// A sparse matrix solver for the possion problem
//(c) Chris Tralie, 2012

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MatrixSolver {

    public static BufferedImage loadImageFromDisk(String filePath) {
        BufferedImage selectedImage = null;
        try {
            // Load the image from the file
            File file = new File(filePath);
            selectedImage = ImageIO.read(file);

            // Check if the image was loaded successfully
            if (selectedImage == null) {
                System.out.println("Failed to load image from: " + filePath);
            } else {
                System.out.println("Image loaded successfully from: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("Error loading image: " + e.getMessage());
        }
        return selectedImage;
    }

    //Variables passed along from the visual interface
    //used to display the image as it's iteratively updated
    public ArrayList<Cord> selectionArea;
    public int xMin, yMin;

    //Matrix variables
    int N;
    int[] D;//Diagonal
    int[][] R;//Off-Diagonal
    //NOTE: This R actually stores the negative of the real R matrix
    double[][] X;//Guess
    double[][] b;//Target of Ax = b

    int Width;
    int Height;

    public MatrixSolver(int[][] mask, ArrayList<Cord> selectionArea, BufferedImage image, BufferedImage selectedImage,
                        int xMin, int yMin) {

        this.selectionArea = selectionArea;
        this.xMin = xMin;
        this.yMin = yMin;

        this.Width = image.getWidth();
        this.Height = image.getHeight();

        //The following code is relevant only for flattening
        int selWidth = selectedImage.getWidth(), selHeight = selectedImage.getHeight();
        boolean[][] gradMask = new boolean[selWidth][selHeight];
        double energyScale = 1.0;//Try to preserve energy during flattening
        for (int i = 0; i < selWidth; i++) {
            for (int j = 0; j < selHeight; j++) {
                gradMask[i][j] = true;
            }
        }

        N = selectionArea.size();
        D = new int[N];
        R = new int[N][4];
        X = new double[N][3];//For the 3 color channels
        b = new double[N][3];
        //Initialize the matrix and make the initial guess the value
        //of the pixels in "selectedImage"
        int[][] dP = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int i = 0; i < N; i++) {
            int Np = 0;//Number of valid neighbors
            int x = selectionArea.get(i).x;
            int y = selectionArea.get(i).y;
            int selX = x - xMin;
            int selY = y - yMin;

            int RGB = selectedImage.getRGB(selX, selY);
            int pValueR = (RGB & 0xFF0000) >> 16;
            int pValueG = (RGB & 0xFF00) >> 8;
            int pValueB = RGB & 0xFF;

            b[i][0] = 0.0;
            b[i][1] = 0.0;
            b[i][2] = 0.0;
            double weight = gradMask[selX][selY] ? 1.0 : 0.0;

            for (int k = 0; k < dP.length; k++) {
                int x2 = x + dP[k][0];
                int y2 = y + dP[k][1];
                R[i][k] = -1;
                if (x2 < 1 || x2 >= Width - 1 || y2 < 1 || y2 >= Height - 1) {
                    continue;
                }
                Np++;
                int index = mask[x2][y2];
                if (index == -1) { //It's a border pixel
                    RGB = image.getRGB(x2, y2);
                    b[i][0] += (RGB & 0xFF0000) >> 16;
                    b[i][1] += (RGB & 0xFF00) >> 8;
                    b[i][2] += RGB & 0xFF;
                } else {
                    R[i][k] = index;
                    selX = x2 - xMin;
                    selY = y2 - yMin;
                    RGB = selectedImage.getRGB(selX, selY);
                    int qValueR = (RGB & 0xFF0000) >> 16;
                    int qValueG = (RGB & 0xFF00) >> 8;
                    int qValueB = RGB & 0xFF;
                    //vPQ = P - Q
                    b[i][0] += weight * (pValueR - qValueR);
                    b[i][1] += weight * (pValueG - qValueG);
                    b[i][2] += weight * (pValueB - qValueB);
                }
            }
            D[i] = Np;
        }
    }

    //Use the Jacobi Method
    public void nextIteration() {
        double[][] nextX = new double[N][3];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < 3; k++) {
                nextX[i][k] = b[i][k];
            }
            for (int n = 0; n < 4; n++) {
                if (R[i][n] >= 0) {
                    int index = R[i][n];
                    for (int k = 0; k < 3; k++) {
                        nextX[i][k] += X[index][k];
                    }
                }
            }
            for (int k = 0; k < 3; k++) {
                nextX[i][k] /= (double) D[i];
            }
        }
        for (int i = 0; i < N; i++) {
            X[i] = nextX[i];
        }
    }

    public double getError() {
        double total = 0.0;
        for (int i = 0; i < N; i++) {
            double[] error = {b[i][0], b[i][1], b[i][2]};
            for (int n = 0; n < 4; n++) {
                if (R[i][n] >= 0) {
                    int index = R[i][n];
                    for (int k = 0; k < 3; k++) {
                        error[k] += X[index][k];
                    }
                }
            }
            error[0] -= D[i] * X[i][0];
            error[1] -= D[i] * X[i][1];
            error[2] -= D[i] * X[i][2];
            total += (error[0] * error[0] + error[1] * error[1] + error[2] * error[2]);
        }

        //System.out.println(Math.sqrt(total));
        return Math.sqrt(total);
    }

    public void updateImage(BufferedImage selectedImage) {
        for (int i = 0; i < X.length; i++) {
            int x = selectionArea.get(i).x - xMin;
            int y = selectionArea.get(i).y - yMin;
            int R = (int) Math.round(X[i][0]);
            int G = (int) Math.round(X[i][1]);
            int B = (int) Math.round(X[i][2]);
            if (R > 255) {
                R = 255;
            }
            if (R < 0) {
                R = 0;
            }
            if (G > 255) {
                G = 255;
            }
            if (G < 0) {
                G = 0;
            }
            if (B > 255) {
                B = 255;
            }
            if (B < 0) {
                B = 0;
            }
            int RGB = 0xFF000000 | (R << 16) & 0xFF0000 | (G << 8) & 0xFF00 | B & 0xFF;
            selectedImage.setRGB(x, y, RGB);
        }
    }
}
