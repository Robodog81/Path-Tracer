
/**
 * A Path Tracer
 *
 * @author Neo Hunt
 * @version 1 14/8/26
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;

public class PathTracer extends JFrame implements MouseListener, MouseMotionListener, ActionListener, KeyListener {
    JPanel renderPanel;

    JDialog settings = new JDialog();
    JDialog globalSettings = new JDialog();

    final int BOARDWIDTH = 1920 / 4;
    final int BOARDHEIGHT = 1080 / 4;
    final int CELLSIZE = 1;
    final boolean ANTIALIASING = false;
    final int THREADAMT = Runtime.getRuntime().availableProcessors(); // the amount of available threads
    final int MAXBOUNCES = 50;
    final double AIRRI = 1; // index of refraction of air
    int toneMapType = 2; // only not a final because vs code hilights the alternative map as unused code
    boolean previewRender = true;
    int skyBrightness = 150;
    boolean debugInfo = true;

    double[][][] frameBuffer = new double[BOARDWIDTH][BOARDHEIGHT][3];

    List<double[]> sphereData = new ArrayList<double[]>();
    // x, y, z, radius, r, g, b, emition r g b, material, extra property (12 wide)

    int camX = 0;
    int camY = -90;
    int camZ = -250;

    int selectedSphere = 0;

    double fOV = 150;

    int frameNumber = 1;

    double fPS;
    long lastTime;
    double timeTo500 = 0;

    boolean painting = false;

    int[] pressedKeys = new int[6];

    final double startTime = System.nanoTime();

    private BufferedImage buffer;

    JMenuBar menuBar = new JMenuBar();

    ExecutorService threadPool = Executors.newFixedThreadPool(8);

    public PathTracer() {
        setTitle("Path Tracer");
        setSize((BOARDWIDTH * CELLSIZE) + 20, (BOARDHEIGHT * CELLSIZE) + 60);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JMenu optionsMenu = new JMenu("Options");

        JMenuItem renderItem = new JMenuItem("Toggle Render Mode");
        renderItem.addActionListener(this);
        renderItem.setAccelerator(KeyStroke.getKeyStroke('r'));
        JMenuItem editPopupItem = new JMenuItem("Sphere Editor");
        editPopupItem.addActionListener(this);
        editPopupItem.setAccelerator(KeyStroke.getKeyStroke('t'));
        JMenuItem addItem = new JMenuItem("Add Sphere");
        addItem.addActionListener(this);
        addItem.setAccelerator(KeyStroke.getKeyStroke('+'));
        JMenuItem removeItem = new JMenuItem("Remove Sphere");
        removeItem.addActionListener(this);
        removeItem.setAccelerator(KeyStroke.getKeyStroke('-'));
        JMenuItem extraSettings = new JMenuItem("Extra Settings");
        extraSettings.addActionListener(this);
        extraSettings.setAccelerator(KeyStroke.getKeyStroke('f'));

        optionsMenu.add(renderItem);
        optionsMenu.add(editPopupItem);
        optionsMenu.addSeparator();
        optionsMenu.add(addItem);
        optionsMenu.add(removeItem);
        menuBar.add(optionsMenu);
        optionsMenu.addSeparator();
        optionsMenu.add(extraSettings);

        setJMenuBar(menuBar);

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        addSphere(0, 5000, 0, 5000, 1, 1, 1, 0, 0, 0, 1, 0);
        addSphere(0, 0, 0, 20, 0.4, 1, 0.5, 0, 0, 0, 1, 0);
        addSphere(-60, -30, 40, 25, 0, 0, 0, 255, 255, 255, 1, 0);
        addSphere(100, -40, 100, 40, 0.4, 1, 0.5, 0, 0, 0, 2, 0);
        addSphere(110, -40, -100, 40, 1, 0.2, 0.5, 0, 0, 0, 2, 0);
        addSphere(-200, -40, 120, 40, 0.85, 1, 0.85, 0, 0, 0, 2, 0);
        addSphere(-280, -40, 80, 40, 1, 0.85, 0.85, 0, 0, 0, 2, 0);
        addSphere(290, -400, 700, 600, 0.8, 0.8, 0.8, 0, 0, 0, 2, 0.1);
        addSphere(100, -120, 0, 30, 0, 0, 0, 50, 1000, 50, 1, 0);
        addSphere(50, -20, -35, 20, 1, 1, 1, 0, 0, 0, 3, 1.5);
        addSphere(-70, -30, -35, 30, 1, 1, 1, 0, 0, 0, 3, 1.5);
        addSphere(0, -5, -45, 5, 1, 1, 1, 0, 0, 0, 3, 1.5);

        /*
         * addSphere(0, 100000000, 0, 100000000, 1, 1, 1, 0, 0, 0, 1, 0);
         * addSphere(0, -130, 0, 60, 1, 1, 1, 0, 0, 0, 3, 1.5);
         * addSphere(80, -300, 80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 3, 1.5);
         * addSphere(0, -300, 80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(-80, -300, 80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(80, -300, 0, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(0, -300, 0, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(-80, -300, 0, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(80, -300, -80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(0, -300, -80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1, 1.5);
         * addSphere(-80, -300, -80, 20, 1, 1, 1, Math.random() * 1000, Math.random() *
         * 1000, Math.random() * 1000, 1,
         * 1.5);
         * addSphere(500, -250, 400, 200, 0.85, 0.85, 0.85, 0, 0, 0, 2, 0);
         * addSphere(-500, -250, 400, 200, 0.85, 0.85, 0.85, 0, 0, 0, 2, 0);
         */

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

        /*
         * addSphere(0, 1000000000, 0, 1000000000, 1, 1, 1, 0, 0, 0, 1, 0);
         * // addSphere(0, -100, 0, 90, 1, 0.8, 0.3, 0, 0, 0, 2, 0);
         * addSphere(120, -40, 0, 30, 0.3, 1, 0, 0, 0, 0, 1, 0);
         * addSphere(120, -30, -100, 30, 1, 1, 1, 0, 0, 0, 3, 1.5);
         * addSphere(-170, -50, 0, 50, 1, 1, 1, 255, 200, 200, 1, 1);
         * addSphere(120, -50, 0, 50, 1, 1, 1, 200, 255, 200, 1, 1);
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

                Graphics2D gScreen = (Graphics2D) g; // scale the low res buffer to the desired screen resoloutiuon
                gScreen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int screenWidth = getWidth();
                int screenHeight = getHeight();
                g.drawImage(buffer, 0, 0, screenWidth, screenHeight, null); // put the buffer onto the screen
                if (debugInfo) {
                    Font customFont = new Font("Arial", Font.BOLD, 18);
                    g.setColor(Color.RED);
                    g.setFont(customFont);
                    g.drawString("Frame: " + frameNumber, 20, 30);
                    g.drawString("FPS: " + Math.round(fPS * 10) / 10, 20, 50);
                    g.drawString("Time To 500: " + Math.round(timeTo500 * 100) / 100.0, 20, 70);
                    g.drawString("Threads: " + THREADAMT, 20, 90);
                    g.drawString("Run Time: " + Math.round((System.nanoTime() - startTime) / 100000000) / 10.0, 20,
                            110);
                }
            }
        };

        // Initialize renderPanel size and add to Frame
        renderPanel.setPreferredSize(new Dimension(BOARDHEIGHT * CELLSIZE, BOARDWIDTH * CELLSIZE));
        add(renderPanel, BorderLayout.CENTER);

        setVisible(true);

        tickLoop();
    }

    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "Toggle Render Mode":
                previewRender = !previewRender;
                settings.setVisible(false);
                globalSettings.setVisible(false);
                reset(previewRender);
                break;
            case "Sphere Editor":
                previewRender = true;
                reset(previewRender);
                launchSettings();
                reset(previewRender);
                break;
            case "Add Sphere":
                addSphere(0, 0, 0, 30, 1, 1, 1, 0, 0, 0, 1, 0);
                selectedSphere = sphereData.size() - 1;
                // update the settings menu
                if (settings.isVisible())
                    launchSettings();
                break;
            case "Remove Sphere":
                sphereData.remove(selectedSphere);
                // prevent the selected sphere acessing an invalid index
                if (selectedSphere > sphereData.size() - 1) {
                    selectedSphere = sphereData.size() - 1;
                    if (sphereData.size() == 0)
                        selectedSphere = 0;
                }
                // update the settings menu
                if (settings.isVisible())
                    launchSettings();
                break;
            case "Extra Settings":
                if (globalSettings == null) // for the first time (not openned yet) it is just null
                    globalSettingsPopup();
                else if (!globalSettings.isVisible()) { // if it is hidden (closed)
                    globalSettingsPopup();
                }
                break;
            default:
                break;
        }
    }

    public void reset(boolean preview) {
        // at the moment it just sets frame number to 1
        // this causes the rendered data to overite the old data
        if (!preview)
            frameBuffer = new double[BOARDWIDTH][BOARDHEIGHT][3];
        frameNumber = 1;
    }

    public void launchSettings() {
        settings.setVisible(false);
        if (settings == null) // for the first time (not openned yet) it is just null
            settingsPopup();
        else if (!settings.isVisible()) { // if it is hidden (closed)
            settingsPopup();
        }
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
            // if it is a preview use the basic clamp
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
        while (true) {
            if (previewRender)
                editSpheres();

            List<Callable<Void>> threadTasks = new ArrayList<>();

            for (int i = 0; i < THREADAMT; i++) {
                int input = i;
                // add all of the tasks for every thread to an array
                threadTasks.add(() -> {
                    renderChunk(input);
                    return null;
                });
            }

            try {
                // start all of the tasks and wait for them to dinish
                threadPool.invokeAll(threadTasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            frameNumber++;

            if (frameNumber == 500) {
                timeTo500 = (System.nanoTime() - startTime) / 1000000000.0;
            }

            // fps caculation
            long currentTime = System.nanoTime();
            fPS = 1 / ((currentTime - lastTime) / 1000000000.0);
            lastTime = System.nanoTime();

            renderPanel.repaint();

        }
    }

    public void editSpheres() {
        if (sphereData.size() > 0) {
            double[] currentSphere = sphereData.get(selectedSphere);

            // move the selected sphere using the current pressed keys
            currentSphere[0] += (pressedKeys[3] - pressedKeys[1]) * 2;
            currentSphere[1] += (pressedKeys[4] - pressedKeys[5]) * 2;
            currentSphere[2] += (pressedKeys[0] - pressedKeys[2]) * 2;

            reset(true);

            sphereData.set(selectedSphere, currentSphere);
        }
    }

    public void renderChunk(int startArea) {
        int startY = startArea;
        for (int y = startY; y < BOARDHEIGHT; y += THREADAMT) {
            // render every few coloums in an interlaced pattern between threads
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
    }

    public double[] getPixelColour(int x, int y) {
        double rand1 = 0;
        double rand2 = 0;
        if (ANTIALIASING) {
            rand1 = ThreadLocalRandom.current().nextDouble() - 0.5; // anti aliasing, offset the inital ray by a random
            rand2 = ThreadLocalRandom.current().nextDouble() - 0.5; // amount, half a pixel in either direction
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

        if (!previewRender) {
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
        } else {
            incomingLight = previewRender(rayPos[0], rayPos[1], rayPos[2], rayDir[0], rayDir[1], rayDir[2]);
        }
        return incomingLight;
        // the ray has finished bouncing
    }

    public double[] previewRender(double startX, double startY, double startZ, double dirX, double dirY,
            double dirZ) { // a quick single cast render for placing spheres
        // gives a preveiw that only needs a single layer to render.

        double[] rayPos = new double[] { startX, startY, startZ };
        double[] rayDir = new double[] { dirX, dirY, dirZ };

        double[] colourModifier = new double[] { 1, 1, 1 };

        double[] nextRay = new double[3];

        boolean onSelectedSphere = false;
        for (int i = 0; i < 5; i++) {

            nextRay = sendIndividualRay(rayPos[0], rayPos[1], rayPos[2],
                    rayDir[0], rayDir[1], rayDir[2]);
            if (i == 0 && nextRay[4] == selectedSphere) {
                onSelectedSphere = true;
            }
            double distance = nextRay[0];

            if (distance > 0) { // if the ray hit a sphere, caculate the new ray position and direction
                rayPos = new double[] { rayDir[0] * distance + rayPos[0], rayDir[1] * distance + rayPos[1],
                        rayDir[2] * distance + rayPos[2] };

                switch ((int) sphereData.get((int) nextRay[4])[10]) {
                    case 1:
                        double[] sphereColour = new double[] { sphereData.get((int) nextRay[4])[4] * colourModifier[0],
                                sphereData.get((int) nextRay[4])[5] * colourModifier[1],
                                sphereData.get((int) nextRay[4])[6] * colourModifier[2] };
                        double[] sphereNormal = new double[] { nextRay[1], nextRay[2], nextRay[3] };
                        double[] renderColour = quickDefuseMaterial(sphereNormal, sphereColour,
                                new double[] { rayPos[0], rayPos[1], rayPos[2] });
                        double[] finalColour = new double[3];

                        double transition = onSelectedSphere ? (Math.sin(System.nanoTime() / 400000000.0) + 1) / 2 : 0;
                        finalColour[0] = mix(renderColour[0], 1000, transition);
                        finalColour[1] = mix(renderColour[1], 1000, transition);
                        finalColour[2] = mix(renderColour[2], 1000, transition);

                        return finalColour;

                    case 2:
                        colourModifier[0] *= sphereData.get((int) nextRay[4])[4];
                        colourModifier[1] *= sphereData.get((int) nextRay[4])[5];
                        colourModifier[2] *= sphereData.get((int) nextRay[4])[6];
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
            } else {
                double[] skyColour = getBackGround(rayDir[1]);
                skyColour[0] *= colourModifier[0];
                skyColour[1] *= colourModifier[1];
                skyColour[2] *= colourModifier[2];

                double transition = onSelectedSphere ? (Math.sin(System.nanoTime() / 400000000.0) + 1) / 2 : 0;

                double[] finalColour = new double[3];
                finalColour[0] = mix(skyColour[0], 1000, transition);
                finalColour[1] = mix(skyColour[1], 1000, transition);
                finalColour[2] = mix(skyColour[2], 1000, transition);

                return finalColour;
            }
        }
        double[] sphereColour = new double[] { sphereData.get((int) nextRay[4])[4],
                sphereData.get((int) nextRay[4])[5],
                sphereData.get((int) nextRay[4])[6] };
        double[] sphereNormal = new double[] { nextRay[1], nextRay[2], nextRay[3] };
        double[] renderColour = quickDefuseMaterial(sphereNormal, sphereColour,
                new double[] { rayPos[0], rayPos[1], rayPos[2] });
        double transition = onSelectedSphere ? (Math.sin(System.nanoTime() / 400000000.0) + 1) / 2 : 0;

        double[] finalColour = new double[3];
        finalColour[0] = mix(renderColour[0], 1000, transition);
        finalColour[1] = mix(renderColour[1], 1000, transition);
        finalColour[2] = mix(renderColour[2], 1000, transition);

        return finalColour;
    }

    public double[] quickDefuseMaterial(double[] surfaceNormal, double[] sphereColour, double[] collisionPos) {
        // used for the preveiw render, no further rays are cast from this material
        double[] background = getBackGround(surfaceNormal[1] * 0.9);
        double shading = ((surfaceNormal[1] - 1) / -3) + 0.3333;

        double[] light = new double[3];
        double irradience = 0;
        for (int i = 0; i < sphereData.size(); i++) {
            double distanceFromLight = Math.sqrt(Math.pow(collisionPos[0] - sphereData.get(i)[0], 2) +
                    Math.pow(collisionPos[1] - sphereData.get(i)[1], 2) +
                    Math.pow(collisionPos[2] - sphereData.get(i)[2], 2))
                    + sphereData.get(i)[3];

            double[] emission = new double[] { sphereData.get(i)[7], sphereData.get(i)[8],
                    sphereData.get(i)[9] };

            double[] direction = new double[] {
                    sphereData.get(i)[0] - collisionPos[0],
                    sphereData.get(i)[1] - collisionPos[1],
                    sphereData.get(i)[2] - collisionPos[2] };

            double normalLength = Math.sqrt((direction[0] * direction[0]) + (direction[1] * direction[1])
                    + (direction[2] * direction[2]));

            direction[0] /= normalLength;
            direction[1] /= normalLength;
            direction[2] /= normalLength;

            irradience = (Math.pow(sphereData.get(i)[3] / distanceFromLight, 2) * dot(surfaceNormal, direction)) * 3;
            // if the point is on the light it must be light
            // may cause a slight bug on the side of a sphere to the light
            if (dot(surfaceNormal, direction) < -0.9999)
                irradience = 3;

            light[0] += irradience * emission[0];
            light[1] += irradience * emission[1];
            light[2] += irradience * emission[2];
        }

        double[] colour = new double[] {
                sphereColour[0] * 255 * shading * (background[0] / 255) + max((light[0]), 0),
                sphereColour[1] * 255 * shading * (background[1] / 255) + max((light[1]), 0),
                sphereColour[2] * 255 * shading * (background[2] / 255) + max((light[2]), 0) };

        return colour;
    }

    public double mix(double a, double b, double x) {
        double difference = b - a;
        return a + (difference * x);
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

        if (cannotRefract || (reflectance(cosTheta, refractionIndex) > ThreadLocalRandom.current().nextDouble()
                && !previewRender)) {
            newDir = reflect(rayDir, surfaceNormal);
        } else {
            newDir = refract(rayDir, surfaceNormal, refractionIndex);
        }
        return newDir;
    }

    public double[] metalMaterial(double[] surfaceNormal, double[] rayDir, int collisionIndex) {
        double fuzziness = sphereData.get(collisionIndex)[11];

        double[] reflectDir = reflect(rayDir, surfaceNormal);

        double[] newDir = reflectDir;

        if (fuzziness > 0) {
            boolean continueLoop = true;
            while (continueLoop) {
                double[] randVec = new double[3];
                if (!previewRender)
                    randVec = randomBounceVector();

                newDir = reflectDir;

                newDir[0] += randVec[0] * fuzziness;
                newDir[1] += randVec[1] * fuzziness;
                newDir[2] += randVec[2] * fuzziness;

                // normalise
                double normalLength = Math.sqrt((newDir[0] * newDir[0]) + (newDir[1] * newDir[1])
                        + (newDir[2] * newDir[2]));
                newDir[0] /= normalLength;
                newDir[1] /= normalLength;
                newDir[2] /= normalLength;

                if (dot(newDir, surfaceNormal) > 0) {
                    continueLoop = false;
                }
            }
        } else {
            // if there is no fuzz just reflect the ray
            newDir = reflectDir;
        }
        return newDir;
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
                if (d < sphereRadius) { // if the ray will hit the sphere
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

    public double max(double a, double b) {
        return a > b ? a : b;
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
        return new double[] { blend * skyBrightness, blend * skyBrightness, skyBrightness };
    }

    public void settingsPopup() { // slider layout based on a google ai overveiw example
        settings = new JDialog();

        settings.setLayout(new BorderLayout());
        settings.setResizable(false);
        settings.setTitle("Sphere Editor");

        JPanel contentPanelLeft = new JPanel();
        contentPanelLeft.setLayout(new BoxLayout(contentPanelLeft, BoxLayout.Y_AXIS));
        contentPanelLeft.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // add padding on the border

        JPanel contentPanelRight = new JPanel();
        contentPanelRight.setLayout(new BoxLayout(contentPanelRight, BoxLayout.Y_AXIS));
        contentPanelRight.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        double[] data = sphereData.get(selectedSphere);

        JLabel radius = new JLabel("Radius: " + data[3], JLabel.CENTER);
        JLabel r = new JLabel("Red: " + data[4], JLabel.CENTER);
        JLabel g = new JLabel("Green: " + data[5], JLabel.CENTER);
        JLabel b = new JLabel("Blue: " + data[6], JLabel.CENTER);
        JLabel eR = new JLabel("Emmision Red: " + data[7], JLabel.CENTER);
        JLabel eG = new JLabel("Emmision Green: " + data[8], JLabel.CENTER);
        JLabel eB = new JLabel("Emmision Blue: " + data[9], JLabel.CENTER);
        JLabel material = new JLabel("Material: " + (int) data[10], JLabel.CENTER);
        JLabel mExtra = new JLabel("Material Property: " + data[11], JLabel.CENTER);

        JSlider s1 = new JSlider(0, 1000, (int) clamp(data[3], 0, 1000.0));
        JSlider s2 = new JSlider(0, 1000, (int) clamp(data[4] * 1000, 0, 1000.0));
        JSlider s3 = new JSlider(0, 1000, (int) clamp(data[5] * 1000, 0, 1000.0));
        JSlider s4 = new JSlider(0, 1000, (int) clamp(data[6] * 1000, 0, 1000.0));
        JSlider s5 = new JSlider(0, 1000, (int) clamp(data[7], 0, 1000.0));
        JSlider s6 = new JSlider(0, 1000, (int) clamp(data[8], 0, 1000.0));
        JSlider s7 = new JSlider(0, 1000, (int) clamp(data[9], 0, 1000.0));
        JSlider s8 = new JSlider(1, 3, (int) clamp(data[10], 1, 3));
        JSlider s9 = new JSlider(0, 1000, (int) clamp(data[11] * 200, 0, 1000.0));

        JLabel[] labels = { radius, r, g, b, eR, eG, eB, material, mExtra };
        JSlider[] sliders = { s1, s2, s3, s4, s5, s6, s7, s8, s9 };
        String[] titles = { "Radius: ", "Red: ", "Green: ", "Blue: ", "Emmsion Red: ", "Emmision Green: ",
                "Emmision Blue: ", "Material: ", "Material Property: " };

        // Loop to configure and add components
        for (int i = 0; i < labels.length; i++) {
            sliders[i].setPaintTicks(true);

            int index = i; // so the listner can read i

            if (i == 7) {
                sliders[i].setMajorTickSpacing(1);
            } else {
                sliders[i].setMajorTickSpacing(30);
            }

            sliders[i].addChangeListener(e -> {
                // change back into edit mode if rendering
                previewRender = true;
                reset(previewRender);

                // Read the current integer value
                int value = sliders[index].getValue();

                if (index > 0 && index < 4) {
                    labels[index].setText(titles[index] + value / 1000.0);
                } else if (index == 8) {
                    labels[index].setText(titles[index] + value / 200.0);
                } else {
                    labels[index].setText(titles[index] + value);
                }

                switch (index) {
                    case 0:
                        sphereData.get(selectedSphere)[3] = value;
                        break;
                    case 1:
                        sphereData.get(selectedSphere)[4] = value / 1000.0;
                        break;
                    case 2:
                        sphereData.get(selectedSphere)[5] = value / 1000.0;
                        break;
                    case 3:
                        sphereData.get(selectedSphere)[6] = value / 1000.0;
                        break;
                    case 4:
                        sphereData.get(selectedSphere)[7] = value;
                        break;
                    case 5:
                        sphereData.get(selectedSphere)[8] = value;
                        break;
                    case 6:
                        sphereData.get(selectedSphere)[9] = value;
                        break;
                    case 7:
                        sphereData.get(selectedSphere)[10] = value;
                        break;
                    case 8:
                        sphereData.get(selectedSphere)[11] = value / 200.0;
                        System.out.println(sphereData.get(selectedSphere)[11]);
                        break;
                    default:
                        break;
                }
            });

            // Add elements to the panel
            contentPanelLeft.add(labels[i]);
            contentPanelLeft.add(sliders[i]);
            contentPanelLeft.add(Box.createRigidArea(new Dimension(230, 20))); // Spacing
        }

        // Add the group to the dialog
        settings.add(contentPanelLeft, BorderLayout.WEST);
        settings.add(contentPanelRight, BorderLayout.EAST);

        settings.setDefaultCloseOperation(HIDE_ON_CLOSE);
        settings.revalidate(); // Updates the layout
        settings.repaint();
        settings.pack();
        settings.setAlwaysOnTop(true);
        settings.setAutoRequestFocus(false);
        settings.setVisible(true);
    }

    public void globalSettingsPopup() { // slider layout based on a google ai overveiw example
        globalSettings = new JDialog();
        globalSettings.setLayout(new BorderLayout());
        globalSettings.setResizable(false);
        globalSettings.setTitle("Extra Settings");

        JPanel contentPanelLeft = new JPanel();
        contentPanelLeft.setLayout(new BoxLayout(contentPanelLeft, BoxLayout.Y_AXIS));
        contentPanelLeft.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // add padding on the border

        JPanel contentPanelRight = new JPanel();
        contentPanelRight.setLayout(new BoxLayout(contentPanelRight, BoxLayout.Y_AXIS));
        contentPanelRight.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel skyBrightnessLabel = new JLabel("Sky Brightness: " + skyBrightness, JLabel.CENTER);
        JLabel toneMapLabel = new JLabel("Tone-Mapp Type: " + toneMapType, JLabel.CENTER);

        JSlider s1 = new JSlider(0, 1000, (int) max(skyBrightness * 4, 255));
        JSlider s2 = new JSlider(1, 3, (int) toneMapType);

        JLabel[] labels = { skyBrightnessLabel, toneMapLabel };
        JSlider[] sliders = { s1, s2 };
        String[] titles = { "Sky Brightness: ", "Tone-Mapp Type: " };

        JCheckBox debugBox = new JCheckBox("Debug Information", true);

        // Loop to configure and add components
        for (int i = 0; i < labels.length; i++) {
            int index = i; // so the listner can read i

            if (index == 0)
                sliders[i].setMajorTickSpacing(30);
            else
                sliders[i].setMajorTickSpacing(1);
            sliders[i].setPaintTicks(true);

            sliders[i].addChangeListener(e -> {
                // change back into edit mode if rendering
                previewRender = true;
                reset(previewRender);

                // Read the current integer value
                int value = sliders[index].getValue();

                if (index == 0)
                    labels[index].setText(titles[index] + value / 4.0);
                else
                    labels[index].setText(titles[index] + value);

                switch (index) {
                    case 0:
                        skyBrightness = value / 4;
                        break;
                    case 1:
                        toneMapType = value;
                        break;
                    default:
                        break;
                }
            });

            // Add elements to the panel
            contentPanelLeft.add(labels[i]);
            contentPanelLeft.add(sliders[i]);
            contentPanelLeft.add(Box.createRigidArea(new Dimension(230, 20))); // Spacing
        }
        debugBox.addChangeListener(e -> {
            debugInfo = debugBox.isSelected();
        });

        contentPanelLeft.add(debugBox);

        // Add the group to the dialog
        globalSettings.add(contentPanelLeft, BorderLayout.WEST);
        globalSettings.add(contentPanelRight, BorderLayout.EAST);

        globalSettings.setDefaultCloseOperation(HIDE_ON_CLOSE);
        globalSettings.revalidate(); // Updates the layout
        globalSettings.repaint();
        globalSettings.pack();
        globalSettings.setAlwaysOnTop(false);
        globalSettings.setAutoRequestFocus(false);
        globalSettings.setVisible(true);
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        int mouseY = e.getY();
        if (mouseY < 60) {
            menuBar.setPreferredSize(new Dimension(0, 24));
            menuBar.revalidate();
        } else {
            // make the menubar 0px tall so keybinds still work & it is hidden
            menuBar.setPreferredSize(new Dimension(0, 0));
            menuBar.revalidate();
        }
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case 87:
                pressedKeys[0] = 1;
                break;
            case 65:
                pressedKeys[1] = 1;
                break;
            case 83:
                pressedKeys[2] = 1;
                break;
            case 68:
                pressedKeys[3] = 1;
                break;
            case 81:
                pressedKeys[4] = 1;
                break;
            case 69:
                pressedKeys[5] = 1;
                break;
            case 37:
                if (selectedSphere > 0) {
                    selectedSphere--;
                    if (settings.isVisible()) {
                        launchSettings();
                    }
                }
                break;
            case 39:
                if (selectedSphere < sphereData.size() - 1) {
                    selectedSphere++;
                    if (settings.isVisible()) {
                        launchSettings();
                    }
                }
                break;
            default:
                break;
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case 87:
                pressedKeys[0] = 0;
                break;
            case 65:
                pressedKeys[1] = 0;
                break;
            case 83:
                pressedKeys[2] = 0;
                break;
            case 68:
                pressedKeys[3] = 0;
                break;
            case 81:
                pressedKeys[4] = 0;
                break;
            case 69:
                pressedKeys[5] = 0;
                break;
            default:
                break;
        }
    }

    public void keyTyped(KeyEvent e) {
    }
}