
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
import java.util.ArrayList;
import java.util.List;

public class pathTracer extends JFrame {
    JPanel renderPanel;

    final int BOARDWIDTH = 1920 / 4;
    final int BOARDHEIGHT = 1080 / 4;
    final int CELLSIZE = 1;
    final boolean ANTIALIASING = true;
    final int THREADAMT = Runtime.getRuntime().availableProcessors(); // the amount of available threads
    final int SKYBRIGHTNESS = 60;
    final int MAXBOUNCES = 50;
    final double AIRRI = 1; // index of refraction of air
    int toneMapType = 2; // only not a final because vs code hilights the alternative map as unused code

    double[][][] frameBuffer = new double[BOARDWIDTH][BOARDHEIGHT][3];;

    List<double[]> sphereData = new ArrayList<double[]>();
    // x, y, z, radius, r, g, b, emition r g b, material, extra property (12 wide)

    int camX = 0;
    int camY = -160;
    int camZ = -300;

    double fOV = 150;

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

        /*
         * addSphere(0, 5000, 0, 5000, 1, 1, 1, 0, 0, 0, 1, 0);
         * addSphere(0, 0, 0, 20, 0.4, 1, 0.5, 0, 0, 0, 1, 0);
         * addSphere(-60, -30, 40, 25, 0, 0, 0, 255, 255, 255, 1, 0);
         * addSphere(100, -40, 100, 40, 0.4, 1, 0.5, 0, 0, 0, 2, 0);
         * addSphere(110, -40, -100, 40, 1, 0.2, 0.5, 0, 0, 0, 2, 0);
         * addSphere(-200, -40, 120, 40, 0.85, 1, 0.85, 0, 0, 0, 2, 0);
         * addSphere(-280, -40, 80, 40, 1, 0.85, 0.85, 0, 0, 0, 2, 0);
         * addSphere(290, -400, 700, 600, 0.8, 0.8, 0.8, 0, 0, 0, 2, 0.1);
         * addSphere(100, -120, 0, 30, 0, 0, 0, 50, 1000, 50, 1, 0);
         * addSphere(50, -20, -35, 20, 1, 1, 1, 0, 0, 0, 3, 1.5);
         * addSphere(-70, -30, -35, 30, 1, 1, 1, 0, 0, 0, 3, 1.5);
         * addSphere(0, -5, -45, 5, 1, 1, 1, 0, 0, 0, 3, 1.5);
         */

        addSphere(0, 10000000, 0, 10000000, 1, 1, 1, 0, 0, 0, 1, 0);
        addSphere(0, -130, 0, 60, 1, 1, 1, 0, 0, 0, 3, 1.5);
        addSphere(80, -300, 80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 3, 1.5);
        addSphere(0, -300, 80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(-80, -300, 80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(80, -300, 0, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(0, -300, 0, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(-80, -300, 0, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(80, -300, -80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(0, -300, -80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1, 1.5);
        addSphere(-80, -300, -80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
                1000, Math.random() * 1000, 1,
                1.5);
        addSphere(500, -250, 400, 200, 0.85, 0.85, 0.85, 0, 0, 0, 2, 0);
        addSphere(-500, -250, 400, 200, 0.85, 0.85, 0.85, 0, 0, 0, 2, 0);

        /*
         * addSphere(0, 10000000, 0, 10000000, 1, 1, 1, 0, 0, 0, 1, 0);
         * addSphere(0, -130, 0, 80, 1, 1, 1, 0, 0, 0, 3, 1.5);
         * int divides = 8;
         * for (int i = 0; i < 360; i += 360 / divides) {
         * double x = Math.sin(Math.toRadians(i)) * 100;
         * double y = Math.cos(Math.toRadians(i)) * 100;
         * double z = Math.tan(Math.toRadians(i)) * 100;
         * addSphere(x, -300, y, 20, 0, 1, 1, Math.random() * 10000, Math.random() *
         * 10000, Math.random() * 10000, 1,
         * 1.5);
         * addSphere(x * 1.5, -20, y * 1.5, 20, 1, 1, 1, 0, 0, 0, 1,
         * 1.5);
         * addSphere(x * 2, -20, y * 2, 20, 1, 1, 1, 1000, 1000, 1000, 1,
         * 1.5);
         * }
         */
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
                g.drawString("Threads: " + THREADAMT, 20, 90);
            }
        };

        // Initialize renderPanel size and add to Frame
        renderPanel.setPreferredSize(new Dimension(BOARDHEIGHT * CELLSIZE, BOARDWIDTH * CELLSIZE));
        add(renderPanel, BorderLayout.CENTER);

        setVisible(true);

        tickLoop();
    }

    public void addSphere(double x, double y, double z, double radius, double r, double g, double b, double emmitR,
            double emmitG, double emmitB, int material, double extra) {
        double[] newSphere = new double[12];
        newSphere[0] = x; // position x
        newSphere[1] = y; // position y
        newSphere[2] = z; // position z
        newSphere[3] = radius; // radius
        newSphere[4] = r; // colour r
        newSphere[5] = g; // colour g
        newSphere[6] = b; // colour b
        newSphere[7] = emmitR; // emmited light r
        newSphere[8] = emmitG; // emmited light g
        newSphere[9] = emmitB; // emmited light b
        newSphere[10] = material; // material, 1 is matte, 2 is metal, 3 is glass
        newSphere[11] = extra; // extra property, fuzzieness for metal, refractive index for glass

        sphereData.add(newSphere);
    }

    public double[] toneMap(double[] colour) {
        double[] result = new double[3];
        switch (toneMapType) {
            case 1:
                // basic clamp
                for (int i = 0; i < 3; i++) {
                    result[i] = clamp(colour[i], 0, 255);
                    result[i] /= 255;
                }
                break;
            case 2:
                // adaptive tonemap
                double biggestValue = 0;
                for (int i = 0; i < 3; i++) {
                    // find the brightest of the R, G, and B
                    if (colour[i] > biggestValue)
                        biggestValue = colour[i];
                }
                for (int i = 0; i < 3; i++) {
                    result[i] = (colour[i] / 255) / ((biggestValue / 255) + 1);
                }
                break;
            case 3:
                // indipendent tonemap
                for (int i = 0; i < 3; i++) {
                    result[i] = (colour[i] / 255) / ((colour[i] / 255) + 1);
                }
                break;
            default:
                System.out.println("Chosen Tonemap is Doesn't Exist");
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
        double rand1 = 0;
        double rand2 = 0;
        if (ANTIALIASING) {
            rand1 = ThreadLocalRandom.current().nextDouble() - 0.5; // anti aliasing, offset the inital ray by a random
                                                                    // amount, half a pixel
            rand2 = ThreadLocalRandom.current().nextDouble() - 0.5; // in either direction
        }

        x += rand1;
        y += rand2;
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

        double[] rayPos = new double[] { startX, startY, startZ };
        double[] rayDir = new double[] { dirX, dirY, dirZ };

        float[] rayColor = new float[] { 1, 1, 1 };
        double[] incomingLight = new double[] { 0, 0, 0 };

        for (int i = 0; i < MAXBOUNCES; i++) { // bounces the ray multiple times

            double[] nextRay = sendIndividualRay(rayPos[0], rayPos[1], rayPos[2],
                    rayDir[0], rayDir[1], rayDir[2]);
            double distance = nextRay[0];

            if (distance > 0) { // if the ray hit a sphere, caculate the new ray position and direction
                rayPos = new double[] { rayDir[0] * distance + rayPos[0], rayDir[1] * distance + rayPos[1],
                        rayDir[2] * distance + rayPos[2] };

                switch ((int) sphereData.get((int) nextRay[4])[10]) {
                    case 1:
                        rayDir = defuseMaterial(new double[] { nextRay[1], nextRay[2], nextRay[3] });
                        break;
                    case 2:
                        rayDir = metalMaterial(new double[] { nextRay[1], nextRay[2], nextRay[3] }, rayDir,
                                (int) nextRay[4]);
                        break;
                    case 3:
                        rayDir = glassMaterial(new double[] { nextRay[1], nextRay[2], nextRay[3] }, rayDir,
                                (int) nextRay[5], (int) nextRay[4]);
                        break;
                    default:
                        System.out.println("No Sphere Material Found!");
                }

                incomingLight[0] += sphereData.get((int) nextRay[4])[7] * rayColor[0];
                incomingLight[1] += sphereData.get((int) nextRay[4])[8] * rayColor[1];
                incomingLight[2] += sphereData.get((int) nextRay[4])[9] * rayColor[2];

                rayColor[0] *= sphereData.get((int) nextRay[4])[4];
                rayColor[1] *= sphereData.get((int) nextRay[4])[5];
                rayColor[2] *= sphereData.get((int) nextRay[4])[6];

            } else {
                double[] bgColour = getBackGround(rayDir[1]);
                incomingLight[0] += bgColour[0] * rayColor[0];
                incomingLight[1] += bgColour[1] * rayColor[1];
                incomingLight[2] += bgColour[2] * rayColor[2];

                return incomingLight;
                // break out of the loop as the ray hit the background
            }
        }
        return incomingLight;
        // the ray has finished bouncing
    }

    public double[] defuseMaterial(double[] surfaceNormal) {
        double randVec[] = randomBounceVector();
        double bounceDir[] = surfaceNormal;
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

    public double[] glassMaterial(double[] surfaceNormal, double[] rayDir, int internalBounce, int collisionIndex) {
        if (internalBounce == 1)
            surfaceNormal = new double[] { -surfaceNormal[0], -surfaceNormal[1], -surfaceNormal[2] };

        double ballRafractiionIndex = sphereData.get(collisionIndex)[11];
        double[] newDir;
        double refractionIndex = internalBounce == 0 ? (AIRRI / ballRafractiionIndex) : ballRafractiionIndex;

        double cosTheta = dot(new double[] { -rayDir[0], -rayDir[1], -rayDir[2] }, surfaceNormal);
        cosTheta = cosTheta > 1 ? 1 : cosTheta; // prevent it from going over 1
        double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);
        boolean cannotRefract = refractionIndex * sinTheta > 1;

        if (cannotRefract || reflectance(cosTheta, refractionIndex) > ThreadLocalRandom.current().nextDouble()) {
            newDir = reflect(rayDir, surfaceNormal);
        } else {
            newDir = refract(rayDir, surfaceNormal, refractionIndex);
        }
        return newDir;
    }

    public double[] metalMaterial(double[] surfaceNormal, double[] rayDir, int collisionIndex) {
        double randVec[] = randomBounceVector();

        double[] reflectDir = reflect(rayDir, surfaceNormal);

        double fuzziness = sphereData.get(collisionIndex)[11];

        reflectDir[0] += randVec[0] * fuzziness;
        reflectDir[1] += randVec[1] * fuzziness;
        reflectDir[2] += randVec[2] * fuzziness;

        double normalLength = Math.sqrt((reflectDir[0] * reflectDir[0]) + (reflectDir[1] * reflectDir[1])
                + (reflectDir[2] * reflectDir[2]));
        reflectDir[0] /= normalLength;
        reflectDir[1] /= normalLength;
        reflectDir[2] /= normalLength;
        return reflectDir;
    }

    public double[] reflect(double[] uv, double[] normal) {
        double[] reflect = new double[3];
        double dot = dot(uv, normal);
        reflect[0] = uv[0] - 2 * dot * normal[0];
        reflect[1] = uv[1] - 2 * dot * normal[1];
        reflect[2] = uv[2] - 2 * dot * normal[2];
        return reflect;
    }

    public double reflectance(double cosine, double refractionIndex) {
        double r0 = (1 - refractionIndex) / (1 + refractionIndex);
        r0 *= r0; // square the value
        return r0 + (1 - r0) * Math.pow((1 - cosine), 5);
    }

    public double[] refract(double[] uv, double[] normal, double etaiOverEtat) {
        double cosTheta = dot(new double[] { -uv[0], -uv[1], -uv[2] }, normal);
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
        for (int i = 0; i < sphereData.size(); i++) {
            double sphereX = sphereData.get(i)[0];
            double sphereY = sphereData.get(i)[1];
            double sphereZ = sphereData.get(i)[2];
            double sphereRadius = sphereData.get(i)[3];

            double deltaX = sphereX - startX; // minus xyz of the sphere
            double deltaY = sphereY - startY;
            double deltaZ = sphereZ - startZ;

            double length = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));

            double tC = dot(new double[] { deltaX, deltaY, deltaZ }, new double[] { dirX, dirY, dirZ });
            if (!(tC < 0)) { // the ray isn't goint to hit the sphere, wrong dir
                double d = Math.sqrt((length * length) - (tC * tC));
                if (!(d > sphereRadius)) { // if the ray will hit the sphere
                    // compute the ray intersect points
                    double t1C = Math.sqrt((sphereRadius * sphereRadius) - (d * d));
                    double t1 = tC - t1C;
                    double t2 = tC + t1C;

                    double nearestCollision;
                    int innerCollision = 0; // if the ray is inside the sphere
                    if (t1 < t2 && t1 > 0.00000001) { // prioritise the closer collision, for glass inner collisions
                        nearestCollision = t1;
                        innerCollision = 0;
                        // System.out.println("collide Outside");
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

    public double dot(double[] a, double[] b) {
        return (a[0] * b[0]) + (a[1] * b[1]) + (a[2] * b[2]);
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

    public double[] getBackGround(double yIn) {
        // returns a colour based on the y value of the ray, used to make a sky gradient
        double blend = (yIn / 4) + 0.75;

        return new double[] { blend * SKYBRIGHTNESS, blend * SKYBRIGHTNESS, SKYBRIGHTNESS };
    }
}