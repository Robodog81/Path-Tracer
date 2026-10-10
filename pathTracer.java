
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

public class pathTracer extends JFrame {
    JPanel renderPanel;

    final int BOARDWIDTH = 500;
    final int BOARDHEIGHT = 500;
    final int CELLSIZE = 1;

    final int MAXRAYDISTANCE = 100;

    // Grid array: false = dead, true = alive
    Color[][] accmulationBuffer = new Color[BOARDWIDTH][BOARDHEIGHT];
    Color[][] currentFrameBuffer = new Color[BOARDWIDTH][BOARDHEIGHT];

    final int SPHEREAMT = 10;
    int[][] sphereData = new int[7][SPHEREAMT]; // x, y, z, radius, r, g, b. (7 wide, spheres)

    int camX = 0;
    int camY = -20;
    int camZ = -250;

    double fOV = 200;

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

        for (int i = 0; i < SPHEREAMT; i++) {
            addSphere((int) (Math.random() * 400) - 200,
                    (int) (Math.random() * 400) - 200,
                    (int) (Math.random() * 400) - 200,
                    (int) (Math.random() * 100),
                    (int) (Math.random() * 255),
                    (int) (Math.random() * 255),
                    (int) (Math.random() * 255),
                    i);
        }
        // addSphere(0, -30, 0, 30, 0); // add a sphere at 0,0,0 with a radius of 10 at
        // the first slot
        // addSphere(0, 500, 0, 500, 1);

        // 2. Create the Custom Grid Panel
        renderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw cells
                super.paintComponent(g);

                if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
                    buffer = new BufferedImage(BOARDWIDTH, BOARDHEIGHT, BufferedImage.TYPE_INT_ARGB); // create the
                                                                                                      // image buffer,
                                                                                                      // it is the size
                                                                                                      // of the grid
                } // every cell is rendered as one pixel then upscaled

                Graphics2D g2d = buffer.createGraphics();
                // Draw cells
                for (int y = 0; y < BOARDHEIGHT; y++) { // display the grid
                    for (int x = 0; x < BOARDWIDTH; x++) {
                        buffer.setRGB(x, y, accmulationBuffer[x][y].getRGB()); // render the cell as a single pixel
                    }
                }

                g2d.dispose(); // dispose the buffer, prevents to much garbage

                Graphics2D gScreen = (Graphics2D) g; // scale the low res buffer th the desired screen resoloutiuon
                gScreen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int screenWidth = getWidth();
                int screenHeight = getHeight();
                g.drawImage(buffer, 0, 0, screenWidth, screenHeight, null); // put the buffer onto the screen
            }
        };

        // Initialize renderPanel size and add to Frame
        renderPanel.setPreferredSize(new Dimension(BOARDHEIGHT * CELLSIZE, BOARDWIDTH * CELLSIZE));
        add(renderPanel, BorderLayout.CENTER);

        setVisible(true);

        tickLoop();
    }

    public double[] sendIndividualRay(int startX, int startY, int startZ, double dirX, double dirY, double dirZ) {
        // returns an array of doubles, the first is the distance of the collision, the
        // normal, then the index of the sphere that was hit
        double[] collisionData = new double[5]; // stored as t1, normalx, normaly, normalz, collision index
        double clostestDistance = 99999;
        collisionData[0] = -1; // distance to collision, -1 is ignored
        collisionData[1] = 0; // surface normal
        collisionData[2] = 0;
        collisionData[3] = 0;
        collisionData[4] = -1; // index of the collided sphere, -1 is ignored
        for (int i = 0; i < SPHEREAMT; i++) {
            int sphereX = sphereData[0][i];
            int sphereY = sphereData[1][i];
            int sphereZ = sphereData[2][i];
            int sphereRadius = sphereData[3][i];

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

                        // reflection: v - 2*dot(v,n)*n
                    }
                }
            }
        }
        return collisionData;
    }

    public double dot(double a1, double a2, double a3, double b1, double b2, double b3) {
        return (a1 * b1) + (a2 * b2) + (a3 * b3);
    }

    public void addSphere(int x, int y, int z, int radius, int r, int g, int b, int index) {
        sphereData[0][index] = x;
        sphereData[1][index] = y;
        sphereData[2][index] = z;
        sphereData[3][index] = radius;
        sphereData[4][index] = r;
        sphereData[5][index] = g;
        sphereData[6][index] = b;
    }

    public void tickLoop() {
        while (true) {
            for (int y = 0; y < BOARDHEIGHT; y++) {
                for (int x = 0; x < BOARDWIDTH; x++) {
                    double sendX = x - BOARDWIDTH / 2;
                    // the ray is sent from the camera to the pixel on the screen, this is the
                    // direction of the ray
                    double sendY = y - BOARDHEIGHT / 2;
                    double sendZ = fOV;
                    double normalLength = Math.sqrt((sendX * sendX) + (sendY * sendY) + (sendZ * sendZ));
                    sendX /= normalLength;
                    sendY /= normalLength;
                    sendZ /= normalLength;

                    double[] rayInfo = tracePixel(camX, camY, camZ, sendX, sendY, sendZ);

                    if (rayInfo[0] > 0) { // if the ray actually collided with a sphere
                        int colourX = sphereData[4][(int) rayInfo[4]];
                        int colourY = sphereData[5][(int) rayInfo[4]];
                        int colourZ = sphereData[6][(int) rayInfo[4]];

                        Color testColour = new Color(colourX, colourY, colourZ);

                        accmulationBuffer[x][y] = testColour;
                    } else
                        accmulationBuffer[x][y] = getBackGround(sendY);
                }
            }
            renderPanel.repaint();
            // camZ--;
        }
    }

    public double[] tracePixel(int startX, int startY, int startZ, double dirX, double dirY, double dirZ) {
        // caculates the pixel colour by sending and bouncing rays
        double[] initalRay = sendIndividualRay(startX, startY, startZ, dirX, dirY, dirZ);
        double distance = initalRay[0];
        double newX = initalRay[1] * distance + startX;
        double newY = initalRay[2] * distance + startY;
        double newZ = initalRay[3] * distance + startZ;

        double[] finalRay = initalRay; // the vairible that will be returned
        // double[] accumulatedColour = new double[3]; // the colour that will be
        // returned

        /*
         * accumulatedColour[0] = sphereData[4][(int) initalRay[4]];
         * accumulatedColour[1] = sphereData[5][(int) initalRay[4]];
         * accumulatedColour[2] = sphereData[6][(int) initalRay[4]];
         */

        // make a random bounce normal off of the surface normal
        if (distance > 0) { // if the ray hit a sphere, send a new ray from the collision point in a random
                            // direction
            double randVec[] = randomBounceVector();

            if (dot(randVec[0], randVec[1], randVec[2], initalRay[1], initalRay[2], initalRay[3]) < 0) {
                // if the random vector is facing into the sphere invert it
                randVec[0] *= -1;
                randVec[1] *= -1;
                randVec[2] *= -1;
            }

            double[] secondRay = sendIndividualRay((int) newX, (int) newY, (int) newZ, randVec[0], randVec[1],
                    randVec[2]);
            finalRay = secondRay; // if the ray bounces return the new ray
        }
        return finalRay;
    }

    public double[] randomBounceVector() {
        while (true) {
            double randX = (double) (Math.random() * 2) - 1;
            double randY = (double) (Math.random() * 2) - 1;
            double randZ = (double) (Math.random() * 2) - 1;

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

    public Color getBackGround(double yIn) { // returns a colour based on the y value of the ray, this is used to make a
                                             // sky gradient
        double blend = (yIn / 4) + 0.75;

        return new Color((int) (255 * blend), (int) (255 * blend), 255);
    }
}
