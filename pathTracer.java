
/**
 * A Path Tracer
 *
 * @author Neo Hunt
 * @version 1 14/8/26
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class pathTracer extends JFrame {
    JPanel renderPanel;

    final int BOARDWIDTH = 1920 / 1;
    final int BOARDHEIGHT = 1080 / 1;
    final int CELLSIZE = 1;
    final boolean ANTIALIASING = false;
    final int THREADAMT = 8;
    int toneMapType = 2; // only not a final because vs code hilights the alternative map as unused code

    double[][][] frameBuffer = new double[BOARDWIDTH][BOARDHEIGHT][3];

    final int SPHEREAMT = 17;
    double[][] sphereData = new double[10][SPHEREAMT]; // x, y, z, radius, r, g, b, emition r g b (10 wide, spheres)

    int camX = 0;
    int camY = -20;
    int camZ = -250;

    double fOV = 200;

    int frameNumber = 1;

    boolean[] threadSync = new boolean[3];

    private BufferedImage buffer;

    public pathTracer() {
        setTitle("Path Tracer");
        setSize((BOARDWIDTH * CELLSIZE) + 20, (BOARDHEIGHT * CELLSIZE) + 60);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. Create and Set the Menu Bar
        /*
         * JMenuBar menuBar = new JMenuBar();
         * JMenu fileMenu = new JMenu("File");
         * JMenuItem startItem = new JMenuItem("Start");
         * JMenuItem pauseItem = new JMenuItem("Pause");
         * JMenuItem resetItem = new JMenuItem("Reset");
         * 
         * fileMenu.add(startItem);
         * fileMenu.add(pauseItem);
         * fileMenu.addSeparator();
         * fileMenu.add(resetItem);
         * menuBar.add(fileMenu);
         * 
         * setJMenuBar(menuBar);
         */
        int brightness = 0;
        for (int i = 0; i < SPHEREAMT; i++) {
            if (i == 0) {
                brightness = 255;
            } else {
                brightness = 0;
            }
            addSphere((Math.random() * 400) - 200,
                    (Math.random() * 400) - 200,
                    (Math.random() * 400) - 200,
                    (Math.random() * 100),
                    (Math.random() * 1),
                    (Math.random() * 1),
                    (Math.random() * 1),
                    brightness,
                    brightness,
                    brightness,
                    i);
        }

        // 2. Create the Custom Grid Panel
        renderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw cells
                super.paintComponent(g);

                if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
                    buffer = new BufferedImage(BOARDWIDTH, BOARDHEIGHT, BufferedImage.TYPE_INT_ARGB);
                    // create the image buffer, it is the size of the grid
                } // every cell is rendered as one pixel then upscaled

                Graphics2D g2d = buffer.createGraphics();
                // Draw cells
                for (int y = 0; y < BOARDHEIGHT; y++) { // display the grid
                    for (int x = 0; x < BOARDWIDTH; x++) {

                        double[] renderPixel = toneMap(frameBuffer[x][y]);

                        Color renderColor = new Color((float) renderPixel[0], (float) renderPixel[1],
                                (float) renderPixel[2]);
                        buffer.setRGB(x, y, renderColor.getRGB()); // render the cell as a single pixel
                    }
                }

                g2d.dispose(); // dispose the buffer, prevents to much garbage

                Graphics2D gScreen = (Graphics2D) g; // scale the low res buffer th the desired screen resoloutiuon
                gScreen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int screenWidth = getWidth();
                int screenHeight = getHeight();
                g.drawImage(buffer, 0, 0, screenWidth, screenHeight, null); // put the buffer onto the screen
                Font customFont = new Font("Arial", Font.BOLD, 18);
                g.setFont(customFont);
                g.drawString("Frame: " + frameNumber, 20, 30);
            }
        };

        // Initialize renderPanel size and add to Frame
        renderPanel.setPreferredSize(new Dimension(BOARDHEIGHT * CELLSIZE, BOARDWIDTH * CELLSIZE));
        add(renderPanel, BorderLayout.CENTER);

        setVisible(true);

        tickLoop();
    }

    public double[] toneMap(double[] colour) {
        double[] result = new double[3];
        if (toneMapType == 1) {
            for (int i = 0; i < 3; i++) {
                result[i] = (colour[i] / 255) / ((colour[i] / 255) + 1);
            }
        } else if (toneMapType == 2) {
            for (int i = 0; i < 3; i++) {
                result[i] = clamp(colour[i], 0, 255);
                result[i] /= 255;
            }
        }
        return result;

    }

    public void tickLoop() {
        Thread[] allThreads = new Thread[THREADAMT];
        while (true) {

            for (int i = 0; i < THREADAMT; i++) {
                int startArea = i;
                allThreads[i] = new Thread(() -> {
                    renderChunk(startArea);
                });
                allThreads[i].start();
            }

            try { // wait for the threads to finish runing
                for (int i = 0; i < allThreads.length; i++) {
                    allThreads[i].join();
                }
            } catch (InterruptedException e) {
                System.err.println("Thread Could Not Join: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore interrupted status
                break; // Exit loop if interrupted
            }
            frameNumber++;

        }
    }

    public void renderChunk(int startArea) {
        int chunkSize = BOARDHEIGHT / THREADAMT;
        int startY = chunkSize * startArea;
        for (int y = startY; y < startY + chunkSize; y++) {
            for (int x = 0; x < BOARDWIDTH; x++) {
                double[] rawPixelColour = getPixelColour(x, y);
                //if (startY == 0) {
                    if (frameNumber == 1) {
                        frameBuffer[x][y] = rawPixelColour;
                    } else {
                        frameBuffer[x][y][0] = frameBuffer[x][y][0]
                                + ((rawPixelColour[0] - frameBuffer[x][y][0]) / (frameNumber));
                        frameBuffer[x][y][1] = frameBuffer[x][y][1]
                                + ((rawPixelColour[1] - frameBuffer[x][y][1]) / (frameNumber));
                        frameBuffer[x][y][2] = frameBuffer[x][y][2]
                                + ((rawPixelColour[2] - frameBuffer[x][y][2]) / (frameNumber));
                    }
                //}

            }
        }
        renderPanel.repaint();
    }

    public double[] getPixelColour(int x, int y) {
        double sendX = x - BOARDWIDTH / 2;
        // the ray is sent from the camera to the pixel on the screen, this is the
        // direction of the ray
        double sendY = y - BOARDHEIGHT / 2;
        double sendZ = fOV;
        double normalLength = Math.sqrt((sendX * sendX) + (sendY * sendY) + (sendZ * sendZ));

        sendX /= normalLength;
        sendY /= normalLength;
        sendZ /= normalLength;

        return tracePixel(camX, camY, camZ, sendX, sendY, sendZ);
    }

    public double[] tracePixel(double startX, double startY, double startZ, double dirX, double dirY, double dirZ) {
        // caculates the pixel colour by sending and bouncing rays

        double rand1 = 0;
        double rand2 = 0;
        if (ANTIALIASING) {
            rand1 = ThreadLocalRandom.current().nextDouble() - 0.5; // anti aliasing, offset the inital ray by a random amount, half a pixel
            rand2 = ThreadLocalRandom.current().nextDouble() - 0.5; // in either direction
        }
        double[] rayPos = new double[] { startX + rand1, startY + rand2, startZ };
        double[] rayDir = new double[] { dirX, dirY, dirZ };

        float[] rayColor = new float[] { 1, 1, 1 };
        float[] incomingLight = new float[] { 0, 0, 0 };

        for (int i = 0; i < 5; i++) { // bounces the ray multiple times

            double[] nextRay = sendIndividualRay(rayPos[0], rayPos[1], rayPos[2],
                    rayDir[0], rayDir[1], rayDir[2]);
            double distance = nextRay[0];

            if (distance > 0) { // if the ray hit a sphere, caculate the new ray position and direction
                double randVec[] = randomBounceVector();

                if (dot(randVec[0], randVec[1], randVec[2], nextRay[1], nextRay[2], nextRay[3]) < 0) {
                    // if the random vector is facing into the sphere invert it
                    randVec[0] *= -1;
                    randVec[1] *= -1;
                    randVec[2] *= -1;
                }

                rayPos = new double[] { rayDir[0] * distance + rayPos[0], rayDir[1] * distance + rayPos[1],
                        rayDir[2] * distance + rayPos[2] };
                        
                if (ThreadLocalRandom.current().nextDouble() > 1) {
                    rayDir = new double[] { randVec[0], randVec[1], randVec[2] };
                } else {

                    // reflection

                    double dot = dot(rayDir[0], rayDir[1], rayDir[2], nextRay[1], nextRay[2],
                            nextRay[3]);
                    rayDir[0] = rayDir[0] - 2 * dot * nextRay[1];
                    rayDir[1] = rayDir[1] - 2 * dot * nextRay[2];
                    rayDir[2] = rayDir[2] - 2 * dot * nextRay[3];

                }

                incomingLight[0] += sphereData[7][(int) nextRay[4]] * rayColor[0];
                incomingLight[1] += sphereData[8][(int) nextRay[4]] * rayColor[1];
                incomingLight[2] += sphereData[9][(int) nextRay[4]] * rayColor[2];

                rayColor[0] *= sphereData[4][(int) nextRay[4]];
                rayColor[1] *= sphereData[5][(int) nextRay[4]];
                rayColor[2] *= sphereData[6][(int) nextRay[4]];

            } else {
                double[] bgColour = getBackGround(rayDir[1]);
                incomingLight[0] += bgColour[0] * rayColor[0];
                incomingLight[1] += bgColour[1] * rayColor[1];
                incomingLight[2] += bgColour[2] * rayColor[2];

                return new double[] { incomingLight[0], incomingLight[1], incomingLight[2] };
            }
        }
        return new double[] { incomingLight[0], incomingLight[1], incomingLight[2] };

    }

    public double[] sendIndividualRay(double startX, double startY, double startZ, double dirX, double dirY,
            double dirZ) {
        // returns an array of doubles, the first is the distance of the collision, the
        // normal, then the index of the sphere that was hit
        double[] collisionData = new double[5]; // stored as t1, normalx, normaly, normalz, collision index
        double clostestDistance = 999999999; // a big number, any collision distancewill be favoured over it
        collisionData[0] = -1; // distance to collision, -1 is ignored
        collisionData[1] = 0; // surface normal
        collisionData[2] = 0; // surface normal
        collisionData[3] = 0; // surface normal
        collisionData[4] = -1; // index of the collided sphere, -1 is ignored
        for (int i = 0; i < SPHEREAMT; i++) {
            double sphereX = sphereData[0][i];
            double sphereY = sphereData[1][i];
            double sphereZ = sphereData[2][i];
            double sphereRadius = sphereData[3][i];

            double deltaX = sphereX - startX; // minus xyz of the sphere
            double deltaY = sphereY - startY;
            double deltaZ = sphereZ - startZ;

            double length = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));

            double tC = dot(deltaX, deltaY, deltaZ, dirX, dirY, dirZ);
            if (!(tC < 0)) { // the ray isn't goint to hit the sphere, wrong dir
                double d = Math.sqrt((length * length) - (tC * tC));
                if (!(d > sphereRadius)) { // if the ray will hit the sphere
                    // compute the ray intersect points
                    double t1C = Math.sqrt((sphereRadius * sphereRadius) - (d * d));
                    double t1 = tC - t1C;
                    // double t2 = tC + t1C;
                    if (t1 < clostestDistance) { // this code runs if the new collision is better (closer) than all
                                                 // previous hits.
                        // at the end of the loop the clostest collision is left occupying the caribles
                        clostestDistance = t1;

                        // compute the normal of the hit
                        double hitX = startX + (dirX * t1) - sphereX;
                        double hitY = startY + (dirY * t1) - sphereY;
                        double hitZ = startZ + (dirZ * t1) - sphereZ;

                        // can devide by sphere radius
                        double normalLength = Math.sqrt((hitX * hitX) + (hitY * hitY) + (hitZ * hitZ)); // normalise the
                                                                                                        // normal vector
                        hitX /= normalLength;
                        hitY /= normalLength;
                        hitZ /= normalLength;

                        collisionData[0] = t1;
                        collisionData[1] = hitX;
                        collisionData[2] = hitY;
                        collisionData[3] = hitZ;
                        collisionData[4] = i;
                    }
                }
            }
        }
        return collisionData;
    }

    public double dot(double a1, double a2, double a3, double b1, double b2, double b3) {
        return (a1 * b1) + (a2 * b2) + (a3 * b3);
    }

    public void addSphere(double x, double y, double z, double radius, double r, double g, double b, double emmitR,
            double emmitG, double emmitB, int index) {
        sphereData[0][index] = x; // position x
        sphereData[1][index] = y; // position y
        sphereData[2][index] = z; // position z
        sphereData[3][index] = radius; // radius
        sphereData[4][index] = r; // colour r
        sphereData[5][index] = g; // colour g
        sphereData[6][index] = b; // colour b
        sphereData[7][index] = emmitR; // emmited light r
        sphereData[8][index] = emmitG; // emmited light g
        sphereData[9][index] = emmitB; // emmited light b
    }

    public double clamp(double input, double min, double max) {
        double result = input;
        if (result < min)
            result = min;
        if (result > max)
            result = max;
        return result;
    }

    public double[] randomBounceVector() {
        while (true) {
            double randX = (double) (ThreadLocalRandom.current().nextDouble() * 2) - 1;
            double randY = (double) (ThreadLocalRandom.current().nextDouble() * 2) - 1;
            double randZ = (double) (ThreadLocalRandom.current().nextDouble() * 2) - 1;

            double normalLength = (randX * randX) + (randY * randY) + (randZ * randZ);

            if (normalLength > 1e-160 && normalLength <= 1) {
                // if the vector is too small, floating point
                // errors will occur, if correct return the normalised vector
                // return the normalised vector
                normalLength = Math.sqrt(normalLength);
                return new double[] { randX / normalLength, randY / normalLength, randZ / normalLength };
            }
        }
    }

    public double[] getBackGround(double yIn) { // returns a colour based on the y value of the ray, this is used to
                                                // make a sky gradient
        double blend = (yIn / 4) + 0.75;

        return new double[] { blend * 255, blend * 255, 255 };
    }
}
