
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

    final int BOARDWIDTH = 1920 / 4;
    final int BOARDHEIGHT = 1080 / 4;
    final int CELLSIZE = 1;
    final boolean ANTIALIASING = true;
    final int THREADAMT = 8;
    final int SKYBRIGHTNESS = 255;
    final int MAXBOUNCES = 10;
    int toneMapType = 2; // only not a final because vs code hilights the alternative map as unused code

    double[][][] frameBuffer = new double[BOARDWIDTH][BOARDHEIGHT][3];

    final int SPHEREAMT = 10;
    double[][] sphereData = new double[11][SPHEREAMT]; // x, y, z, radius, r, g, b, emition r g b, material (10 wide,
                                                       // spheres)

    int camX = 0;
    int camY = -50;
    int camZ = -250;

    double fOV = 400;

    int frameNumber = 1;

    boolean[] threadSync = new boolean[3];

    double fPS;
    long lastTime;
    double timeTo500 = 0;

    final double startTime = System.nanoTime();

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
        // for (int i = 0; i < SPHEREAMT; i++) {
        // if (i == 0) {
        // brightness = 500;
        // } else {
        // brightness = 0;
        // }
        /*
         * addSphere((Math.random() * 400) - 200,
         * (Math.random() * 400) - 200,
         * (Math.random() * 400) - 200,
         * (Math.random() * 100),
         * (Math.random() * 1),
         * (Math.random() * 1),
         * (Math.random() * 1),
         * brightness,
         * brightness,
         * brightness,
         * i);
         */
        addSphere(0, 5000, 0, 5000, 1, 1, 1, 0, 0, 0, -1, 0);
        addSphere(0, 0, 0, 20, 0.4, 1, 0.5, 0, 0, 0, -1, 1);
        addSphere(-60, -30, 40, 25, 0, 0, 0, 255, 255, 255, -1, 2);
        addSphere(100, -40, 100, 40, 0.4, 1, 0.5, 0, 0, 0, 0, 3);
        addSphere(110, -40, -100, 40, 1, 0.2, 0.5, 0, 0, 0, 0, 4);
        addSphere(-200, -40, 120, 40, 0.85, 1, 0.85, 0, 0, 0, 0, 5);
        addSphere(-280, -40, 80, 40, 1, 0.85, 0.85, 0, 0, 0, 0, 6);
        addSphere(290, -400, 700, 600, 0.8, 0.8, 0.8, 0, 0, 0, 0.1, 7);
        //addSphere(0, -50, -100, 40, 1, 1, 1, 0, 0, 0, -2, 8);
        // }

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
                g.setColor(Color.RED);
                g.setFont(customFont);
                g.drawString("Frame: " + frameNumber, 20, 30);
                g.drawString("FPS: " + Math.round(fPS * 10) / 10, 20, 50);
                g.drawString("Time To 500: " + Math.round(timeTo500 * 100) / 100.0, 20, 70);
            }
        };

        // Initialize renderPanel size and add to Frame
        renderPanel.setPreferredSize(new Dimension(BOARDHEIGHT * CELLSIZE, BOARDWIDTH * CELLSIZE));
        add(renderPanel, BorderLayout.CENTER);

        setVisible(true);

        tickLoop();
    }

    public void addSphere(double x, double y, double z, double radius, double r, double g, double b, double emmitR,
            double emmitG, double emmitB, double material, int index) {
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
        sphereData[10][index] = material; // material, -1 is matte. 0 is reflective, > 0 is reflection fuzz
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

            if (frameNumber == 500) {
                timeTo500 = (System.nanoTime() - startTime) / 1000000000.0;
            }

            long currentTime = System.nanoTime();
            fPS = 1 / ((currentTime - lastTime) / 1000000000.0);
            lastTime = System.nanoTime();

        }
    }

    public void renderChunk(int startArea) {
        int chunkSize = (int) Math.ceil((double) BOARDHEIGHT / THREADAMT);
        // round up so all pixels are rendered, even if the height is not divisable by
        // the thread amount
        int startY = chunkSize * startArea;
        for (int y = startY; y < clamp(startY + chunkSize, 0, BOARDHEIGHT); y++) {
            // clamp to prevent the last thread overshooting
            for (int x = 0; x < BOARDWIDTH; x++) {
                double[] rawPixelColour = getPixelColour(x, y);
                // if (startY == 0) {
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
                // }

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
            rand1 = ThreadLocalRandom.current().nextDouble() - 0.5; // anti aliasing, offset the inital ray by a random
                                                                    // amount, half a pixel
            rand2 = ThreadLocalRandom.current().nextDouble() - 0.5; // in either direction
        }
        double[] rayPos = new double[] { startX + rand1, startY + rand2, startZ };
        double[] rayDir = new double[] { dirX, dirY, dirZ };

        float[] rayColor = new float[] { 1, 1, 1 };
        float[] incomingLight = new float[] { 0, 0, 0 };

        for (int i = 0; i < MAXBOUNCES; i++) { // bounces the ray multiple times

            double[] nextRay = sendIndividualRay(rayPos[0], rayPos[1], rayPos[2],
                    rayDir[0], rayDir[1], rayDir[2]);
            double distance = nextRay[0];

            if (distance > 0) { // if the ray hit a sphere, caculate the new ray position and direction
                double randVec[] = randomBounceVector();

                rayPos = new double[] { rayDir[0] * distance + rayPos[0], rayDir[1] * distance + rayPos[1],
                        rayDir[2] * distance + rayPos[2] };

                if (sphereData[10][(int) nextRay[4]] == -1) {
                    rayDir = defuseMaterial(new double[] { nextRay[1], nextRay[2], nextRay[3] });
                } else if (sphereData[10][(int) nextRay[4]] == -2) {
                    rayDir = glassMaterial(new double[] { nextRay[1], nextRay[2], nextRay[3] },
                            new double[] { rayDir[0], rayDir[1], rayDir[2] }, (int) nextRay[5]);
                } else {
                    rayDir = metalMaterial(new double[] { nextRay[1], nextRay[2], nextRay[3] },
                            new double[] { rayDir[0], rayDir[1], rayDir[2] }, (int) nextRay[4]);
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
                // break out of the loop as the ray hit the background
            }
        }
        return new double[] { incomingLight[0], incomingLight[1], incomingLight[2] };
        // the ray has finished bouncing
    }

    public double[] defuseMaterial(double[] surfaceNormal) {
        double randVec[] = randomBounceVector();
        double bounceDir[] = new double[] { surfaceNormal[0], surfaceNormal[1], surfaceNormal[2] };
        bounceDir[0] += randVec[0];
        bounceDir[1] += randVec[1];
        bounceDir[2] += randVec[2];
        // normalise the scatter direction
        double normalLength = Math.sqrt((bounceDir[0] * bounceDir[0]) + (bounceDir[1] * bounceDir[1])
                + (bounceDir[2] * bounceDir[2]));

        bounceDir[0] /= normalLength;
        bounceDir[1] /= normalLength;
        bounceDir[2] /= normalLength;

        return bounceDir;
    }

    public double[] glassMaterial(double[] surfaceNormal, double[] rayDir, int internalBounce) {
        double[] refractDir;
        if (internalBounce == 0) {
            refractDir = refract(new double[] { rayDir[0], rayDir[1], rayDir[2] },
                    new double[] { surfaceNormal[0], surfaceNormal[1], surfaceNormal[2] }, -1.333);
            // System.out.println("refract1");
        } else {
            refractDir = refract(new double[] { rayDir[0], rayDir[1], rayDir[2] },
                    new double[] { -surfaceNormal[0], -surfaceNormal[1], -surfaceNormal[2] }, -1);
            // System.out.println("refract2");
        }
        rayDir = new double[] { refractDir[0], refractDir[1], refractDir[2] };
        return refractDir;
    }

    public double[] metalMaterial(double[] surfaceNormal, double[] rayDir, int collisionIndex) {
        double randVec[] = randomBounceVector();
        double dot = dot(rayDir[0], rayDir[1], rayDir[2], surfaceNormal[0], surfaceNormal[1],
                surfaceNormal[2]);
        double[] reflectDir = new double[3];
        reflectDir[0] = rayDir[0] - 2 * dot * surfaceNormal[0];
        reflectDir[1] = rayDir[1] - 2 * dot * surfaceNormal[1];
        reflectDir[2] = rayDir[2] - 2 * dot * surfaceNormal[2];

        reflectDir[0] += randVec[0] * sphereData[10][collisionIndex];
        reflectDir[1] += randVec[1] * sphereData[10][collisionIndex];
        reflectDir[2] += randVec[2] * sphereData[10][collisionIndex];

        double normalLength = Math.sqrt((rayDir[0] * rayDir[0]) + (rayDir[1] * rayDir[1])
                + (rayDir[2] * rayDir[2]));
        reflectDir[0] /= normalLength;
        reflectDir[1] /= normalLength;
        reflectDir[2] /= normalLength;
        return reflectDir;
    }

    public double[] refract(double[] uv, double[] normal, double etaiOverEtat) {
        double cosTheta = dot(-uv[0], -uv[1], -uv[2], normal[0], normal[1], normal[2]);
        if (cosTheta > 1)
            cosTheta = 1; // prevent it from going over 1
        double[] rOutPerp = new double[] { etaiOverEtat, etaiOverEtat, etaiOverEtat };
        rOutPerp[0] *= (uv[0] + cosTheta * normal[0]);
        rOutPerp[1] *= (uv[1] + cosTheta * normal[1]);
        rOutPerp[2] *= (uv[2] + cosTheta * normal[2]);
        double rOutPerpLengthSquared = (rOutPerp[0] * rOutPerp[0]) + (rOutPerp[1] * rOutPerp[1])
                + (rOutPerp[2] * rOutPerp[2]);
        double[] rOutParallel = new double[3];
        rOutParallel[0] = -Math.sqrt(Math.abs(1 - rOutPerpLengthSquared)) * normal[0];
        rOutParallel[1] = -Math.sqrt(Math.abs(1 - rOutPerpLengthSquared)) * normal[1];
        rOutParallel[2] = -Math.sqrt(Math.abs(1 - rOutPerpLengthSquared)) * normal[2];

        return new double[] { rOutPerp[0] + rOutParallel[0], rOutPerp[1] + rOutParallel[1],
                rOutPerp[2] + rOutParallel[2] };
    }

    public double[] sendIndividualRay(double startX, double startY, double startZ, double dirX, double dirY,
            double dirZ) {
        // returns an array of doubles, the first is the distance of the collision, the
        // normal, then the index of the sphere that was hit
        double[] collisionData = new double[6]; // stored as t1, normalx, normaly, normalz, collision index, inner
                                                // collision flag
        double clostestDistance = 999999999; // a big number, any collision distancewill be favoured over it
        collisionData[0] = -1; // distance to collision, -1 is ignored
        collisionData[1] = 0; // surface normal
        collisionData[2] = 0; // surface normal
        collisionData[3] = 0; // surface normal
        collisionData[4] = -1; // index of the collided sphere, -1 is ignored
        collisionData[5] = 0; // inner collision flag
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
                    double t2 = tC + t1C;
                    /*
                     * if (t1 < 0)
                     * t1 = 0;
                     * if (t2 < 0)
                     * t2 = 0;
                     */

                    double nearestCollision;
                    int innerCollision = 0; // if the ray is inside the sphere
                    if (t1 < t2 && t1 > 0) { // prioritise the closer collision, for glass inner collisions
                        nearestCollision = t1;
                        innerCollision = 0;
                    } else {
                        nearestCollision = t2;
                        innerCollision = 1;
                        // System.out.println("collide Inside");
                    }

                    if (nearestCollision < clostestDistance && nearestCollision > 0.00000001) {
                        // this code runs if the new collision is better (closer) than all
                        // previous hits. if the new collision is too close (caused by floating point
                        // errors) it is ignored
                        // at the end of the loop the clostest collision is left occupying the caribles
                        clostestDistance = nearestCollision;

                        // compute the normal of the hit
                        double hitX = startX + (dirX * nearestCollision) - sphereX;
                        double hitY = startY + (dirY * nearestCollision) - sphereY;
                        double hitZ = startZ + (dirZ * nearestCollision) - sphereZ;

                        // can devide by sphere radius
                        double normalLength = Math.sqrt((hitX * hitX) + (hitY * hitY) + (hitZ * hitZ)); // normalise the
                                                                                                        // normal vector
                        hitX /= normalLength;
                        hitY /= normalLength;
                        hitZ /= normalLength;

                        collisionData[0] = nearestCollision;
                        collisionData[1] = hitX; // the surface normal
                        collisionData[2] = hitY;
                        collisionData[3] = hitZ;
                        collisionData[4] = i;
                        collisionData[5] = innerCollision; // if it is an inner surface collision
                    }
                }
            }
        }
        return collisionData;
    }

    public double dot(double a1, double a2, double a3, double b1, double b2, double b3) {
        return (a1 * b1) + (a2 * b2) + (a3 * b3);
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

        return new double[] { blend * SKYBRIGHTNESS, blend * SKYBRIGHTNESS, SKYBRIGHTNESS };
    }
}
