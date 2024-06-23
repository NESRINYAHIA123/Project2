package com.example.merging_2024;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class PoissonStandalone extends JFrame implements ActionListener, WindowListener, MouseListener, MouseMotionListener {

    BufferedImage newImage ;

    //Display parameters
    public static final int Width = 1200;
    public static final int Height = 800;

    //Program state
    public static final int NOTHING = 0;
    public static final int SELECTING = 1;
    public static final int DRAGGING = 2;
    public static final int BLENDING = 3;

    //Menu Options
    public static final String SELECT_REGION = "Select Region";
    public static final String BLEND_SELECTION = "Blend Selection";
    public static final String SAVE_IMAGE = "Save Image to File";
    public static final String STOP = "STOP";

    //GUI Widgets
    public static WelcomeScreen canvas;
    public JMenuBar menu;
    JMenu fileMenu;
    public static BufferedImage image;

    public BufferedImage targetImage;
    public BufferedImage sourceImage;

    //Variables for selected image
    public int[][] mask;//A 2D array that represents a selected region
    //It encodes the enclosed region and the border of that region
    public static ArrayList<Cord> selectionBorder;
    public static ArrayList<Cord> selectionArea;
    public static BufferedImage selectedImage;
    static int xMin;
    int xMax;
    static int yMin;
    int yMax;//Bounding box of selected area

    //GUI State Variables
    public static int state;
    public boolean dragValid;
    public int lastX, lastY;
    public int dx, dy;
    public boolean selectingLeft;
    public boolean doneAnything = false;

    //Matrix solver
    public static MatrixSolver solver;
    public Thread blendingThread;
    public static JProgressBar progressBar;

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    public PoissonStandalone() {


        addWindowListener(this);

        mask = new int[Width][Height];
        for (int x = 0; x < Width; x++) {
            for (int y = 0; y < Height; y++) {
                mask[x][y] = 0;
            }
        }
        selectionArea = new ArrayList<Cord>();
        selectionBorder = new ArrayList<Cord>();

        Container content = getContentPane();
        content.setLayout(null);

        menu = new JMenuBar();
        fileMenu = new JMenu("File");
        fileMenu.addActionListener(this);
        fileMenu.add(SELECT_REGION).addActionListener(this);
        fileMenu.add(BLEND_SELECTION).addActionListener(this);
        menu.add(fileMenu);

        menu.setBounds(0, 0, Width, 20);
        content.add(menu);

        canvas = new WelcomeScreen();
        canvas.setSize(Width, Height);
        canvas.addMouseMotionListener(this);
        canvas.addMouseListener(this);
        canvas.setBounds(0, 50, Width, 50 + Height);
        content.add(canvas);

        progressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
        progressBar.setBounds(0, 20, Width, 50);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        content.add(progressBar);

        state = NOTHING;
        image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, Width, Height);
        selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        dx = 0;
        dy = 0;
        setSize(Width, Height + 100);
        setVisible(true);


        String imagePath = "C:\\Users\\Majeed\\Desktop\\merging\\test\\sourceImage.jpg";
        File file = new File(imagePath);
        try {
            sourceImage = ImageIO.read(file);
        } catch (IOException e) {
            // Handle the exception, e.g., print an error message
            e.printStackTrace();
        }

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, Width/2 , Height);
        g.drawImage(sourceImage, 0, 0, this);


        System.out.println("sourceImage height:" + sourceImage.getHeight());
        System.out.println("sourceImage width:" + sourceImage.getWidth());

        imagePath = "C:\\Users\\Majeed\\Desktop\\merging\\test\\targetImage.jpg";
        file = new File(imagePath);
        try {
            targetImage  = ImageIO.read(file);
        } catch (IOException e) {
            // Handle the exception, e.g., print an error message
            e.printStackTrace();
        }

        System.out.println("targetImage height:" + targetImage.getHeight());
        System.out.println("targetImage width:" + targetImage.getWidth());

        g.setColor(Color.WHITE);
        g.fillRect(Width, 0,  Width / 2, Height);
        g.drawImage(targetImage, Width / 2, 0, this);


        // maybe this is useless
        canvas.repaint();



    }

    public static void main(String[] args) {
        PoissonStandalone program = new PoissonStandalone();

        program.addWindowListener(new WindowListener() {
            public void windowActivated(WindowEvent evt) {
            }

            public void windowClosed(WindowEvent evt) {
            }

            public void windowClosing(WindowEvent evt) {
                System.exit(0);
            }

            public void windowDeactivated(WindowEvent evt) {
            }

            public void windowDeiconified(WindowEvent evt) {
            }

            public void windowIconified(WindowEvent evt) {
            }

            public void windowOpened(WindowEvent evt) {
            }
        });


    }

    public class WelcomeScreen extends JPanel {

        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, this);
            g.setColor(Color.RED);
            if (state == SELECTING || state == DRAGGING) {
                for (int i = 0; i < selectionBorder.size(); i++) {
                    int x = selectionBorder.get(i).x + dx;
                    int y = selectionBorder.get(i).y + dy;
                    g.drawLine(x, y, x, y);
                }
            }

            g.drawImage(selectedImage, xMin + dx, yMin + dy, this);

        }
    }

   static class IterationBlender implements Runnable {

        public void run() {
            int iteration = 0;
            double error;
            double Norm = 1.0;
        //    progressBar.setValue(0);
            do {
                error = solver.getError();

                if (iteration == 1) {
                    Norm = Math.log(error);
                }
               /* if (iteration >= 1) {
                    //The Jacobi method converges exponentially
                    double progress = 1.0 - Math.log(error) / Norm;
                    progressBar.setValue((int) (progress * 100));
                    progressBar.repaint();
                }*/
                iteration++;
                nextIteration();
            } while (error > 1.0 && state == BLENDING);
            finalizeBlending();
            System.out.println("Did " + iteration + "iterations");

        }
    }

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    void updateMask() {
        //Clip the motion to the display window
        if (xMin + dx < 1) {
            dx = 1 - xMin;
        }
        if (xMax + dx > Width - 1) {
            dx = Width - 1 - xMax;
        }
        if (yMin + dy < 1) {
            dy = 1 - yMin;
        }
        if (yMax + dy > Height - 1) {
            dy = Height - 1 - yMax;
        }
        //Now update the mask
        for (int x = 0; x < Width; x++) {
            for (int y = 0; y < Height; y++) {
                mask[x][y] = -3;
            }
        }
        for (int i = 0; i < selectionBorder.size(); i++) {
            int x = selectionBorder.get(i).x + dx  ;
            int y = selectionBorder.get(i).y + dy ;
            selectionBorder.get(i).x = x;
            selectionBorder.get(i).y = y;
            mask[x][y] = -1;
        }
        for (int i = 0; i < selectionArea.size(); i++) {
            int x = selectionArea.get(i).x + dx;
            int y = selectionArea.get(i).y + dy;
            selectionArea.get(i).x = x;
            selectionArea.get(i).y = y;
            mask[x][y] = i;
        }
        xMin += dx;
        xMax += dx;
        yMin += dy;
        yMax += dy;
        dx = 0;
        dy = 0;
    }

    void fillOutside(int paramx, int paramy) {
        ArrayList<Cord> stack = new ArrayList<Cord>();
        stack.add(new Cord(paramx, paramy));
        while (stack.size() > 0) {
            Cord c = stack.remove(stack.size() - 1);
            int x = c.x, y = c.y;
            if (x < 0 || x >= Width || y < 0 || y >= Height) {
                continue;
            }
            if (mask[x][y] == -1) //Stop at border pixels
            {
                continue;
            }
            if (mask[x][y] == 0) //Don't repeat nodes that have already been visited
            {
                continue;
            }
            mask[x][y] = 0;
            stack.add(new Cord(x - 1, y));
            stack.add(new Cord(x + 1, y));
            stack.add(new Cord(x, y - 1));
            stack.add(new Cord(x, y + 1));
        }
    }

    public static void nextIteration() {
        for (int i = 0; i < 100; i++) {
            solver.nextIteration();
        }
        synchronized (selectedImage) {
            solver.updateImage(selectedImage);
        }
        canvas.repaint();
    }

    public static void finalizeBlending() {


        String imagePath = "C:\\Users\\Majeed\\Desktop\\merging\\saved\\image.jpg";



        Graphics g = image.getGraphics();


        // place the selectedImage that have benn changed it edges color, place it on top of image(the whole screen)
        g.drawImage(selectedImage, xMin, yMin, null);

       // ImageSaver.saveImage(image, imagePath, "jpg", 0, 0, sourceImage.getWidth(), sourceImage.getHeight());

        selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        selectionBorder.clear();
        selectionArea.clear();
        state = NOTHING;
    }

    public void getSelectionArea() {
        selectionArea.clear();
        // updateMask();
        //Find bounding box of selected region
        xMin = Width;
        xMax = 0;
        yMin = Height;
        yMax = 0;
        for (int i = 0; i < selectionBorder.size(); i++) {
            int x = selectionBorder.get(i).x;
            int y = selectionBorder.get(i).y;
            if (x < xMin) {
                xMin = x;
            }
            if (x > xMax) {
                xMax = x;
            }
            if (y < yMin) {
                yMin = y;
            }
            if (y > yMax) {
                yMax = y;
            }
        }
        int selWidth = xMax - xMin;
        int selHeight = yMax - yMin;
        selectedImage = new BufferedImage(selWidth, selHeight, BufferedImage.TYPE_INT_ARGB);
        //Find a pixel outside of the bounding box, which is guaranteed
        //to be outside of the selection
        boolean found = false;
        for (int x = 0; x < Width && !found; x++) {
            for (int y = 0; y < Height && !found; y++) {
                if ((x < xMin || x > xMax) && (y < yMin || y > yMax)) {
                    found = true;
                    fillOutside(x, y);
                }
            }
        }
        //Pixels in selection area have mask value of -2, outside have mask value of 0
        for (int x = 0; x < Width; x++) {
            for (int y = 0; y < Height; y++) {
                if (x - xMin >= 0 && y - yMin >= 0 && x - xMin < selWidth && y - yMin < selHeight) //selectedImage.setRGB(x-xMin, y-yMin, image.getRGB(x,y)&0x00FFFFFF);
                {
                    selectedImage.setRGB(x - xMin, y - yMin, image.getRGB(x, y) & 0x004a7704);
                }
                if (mask[x][y] == 0) {
                    mask[x][y] = -2;
                } else if (mask[x][y] != -1) {
                    mask[x][y] = selectionArea.size();//Make mask index of this coord
                    selectionArea.add(new Cord(x, y));
                    int color = (255 << 24) | image.getRGB(x, y);
                    if (x - xMin >= 0 && y - yMin >= 0) {
                        selectedImage.setRGB(x - xMin, y - yMin, color);
                    }
                }
            }
        }




        updateMask();
    }

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    @Override
    public void actionPerformed(ActionEvent evt) {
        doneAnything = true;
        String str = evt.getActionCommand();
        if (state == BLENDING) {
            return;
        }
        if (str.equals(SELECT_REGION)) {
            //Clear previous selection
            selectionBorder.clear();
            selectionArea.clear();
            state = SELECTING;
        } else if (str.equals(BLEND_SELECTION)) {

            state = BLENDING;
            updateMask();


            System.out.println("xMin: "+xMin);
            System.out.println("yMin: "+yMin);

            System.out.println("selectionArea x: "+ selectionArea.get(0).x +"selectionArea y:" + selectionArea.get(0).y);
            System.out.println(selectedImage.getWidth());




           /* Graphics2D g2d = selectedImage.createGraphics();
            g2d.setColor(Color.RED);
            g2d.drawString("some text", 0, 0);*/


            BufferedImage newSelectedImage = new BufferedImage(selectedImage.getWidth(), selectedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = newSelectedImage.createGraphics();
            g2d.drawImage(selectedImage, 0, 0, null);

            // Define the text to be drawn
            String text = "Hello World!";
            Font font = new Font("Arial", Font.BOLD, 36);
            Color color = Color.RED;

            // Set font and color
            g2d.setFont(font);
            g2d.setColor(color);

            // Calculate text position (centered)
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            int x = (selectedImage.getWidth() - textWidth) / 2;
            int y = (selectedImage.getHeight() - textHeight) / 2 + fm.getAscent();

            // Draw the text
            g2d.drawString(text, x, y);
            g2d.dispose();

            String myPath = "c:\\testSavedImages\\newSelectedImage.jpg";
           // ImageSaver.saveImage(mask, selectionArea, newSelectedImage, myPath, "jpg", 0, 0, newSelectedImage.getWidth(), newSelectedImage.getHeight());



           /* for(int i=0; i< mask.length; i++){
                for(int j=0; j< mask[i].length; j++){
                    System.out.println("mask["+i+"]["+j+"] = "+ mask[i][j]);
                }
            }*/


            //xMin and yMin is where to put the first pixel of intelselected image
            solver = new MatrixSolver(mask, selectionArea, image, selectedImage, xMin, yMin);

            //solver = new MatrixSolver(mask, selectionArea, image, selectedImage, xMin, yMin, Width, Height, false);
            IterationBlender blender = new IterationBlender();
            blendingThread = new Thread(blender);
            blendingThread.start();

        }


        canvas.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
        lastX = evt.getX();
        lastY = evt.getY();
    }

    @Override
    public void mouseDragged(MouseEvent evt) {
        int x = evt.getX();
        int y = evt.getY();



        if (state == SELECTING) {

            //System.out.println("start  SELECTING..................................");
            selectionBorder.add(new Cord(x, y));



        } else if (state == DRAGGING) {

            //     System.out.println("start  DRAGGING..................................");
            //Make sure the user is dragging within the bounds of the selection
            if (!dragValid) {
                if (mask[x][y] >= 0) {
                    dragValid = true;
                }
            }
            if (dragValid) {
                dx += (x - lastX);
                dy += (y - lastY);
            }
        }
        lastX = x;
        lastY = y;
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
        //Fill in pixels in between and connect the first to the last
        int N = selectionBorder.size();
        if (N == 0 || (state != SELECTING && state != DRAGGING)) {
            return;
        }

        if (state == SELECTING) {
            for (int n = 0; n < N; n++) {
                int startx = selectionBorder.get(n).x;
                int starty = selectionBorder.get(n).y;
                int totalDX = selectionBorder.get((n + 1) % N).x - startx;
                int totalDY = selectionBorder.get((n + 1) % N).y - starty;
                int numAdded = Math.abs(totalDX) + Math.abs(totalDY);
                for (int t = 0; t < numAdded; t++) {
                    double frac = (double) t / (double) numAdded;
                    int x = (int) Math.round(frac * totalDX) + startx;
                    int y = (int) Math.round(frac * totalDY) + starty;
                    selectionBorder.add(new Cord(x, y));
                }
            }

            updateMask();
            getSelectionArea();
            state = DRAGGING;
            dragValid = false;
            dx = 0;
            dy = 0;
        }
        else if (state == DRAGGING) {
            dragValid = false;
            updateMask();
        }
        canvas.repaint();
    }


    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////



    @Override
    public void mouseClicked(MouseEvent evt) {
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
    }

    @Override
    public void mouseExited(MouseEvent evt) {
    }

    @Override
    public void mousePressed(MouseEvent evt) {
    }




    @Override
    public void windowOpened(WindowEvent evt) {

    }
    @Override
    public void windowClosed(WindowEvent evt) {

    }

    @Override
    public void windowClosing(WindowEvent evt) {
    }

    @Override
    public void windowActivated(WindowEvent evt) {
    }

    @Override
    public void windowDeactivated(WindowEvent evt) {
    }

    @Override
    public void windowDeiconified(WindowEvent evt) {
    }

    @Override
    public void windowIconified(WindowEvent evt) {
    }


////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////// 
}
