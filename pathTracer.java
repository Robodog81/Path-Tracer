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

    final int BOARDWIDTH = 400;
    final int BOARDHEIGHT = 400;
    final int CELLSIZE = 1;

    final int MAXRAYDISTANCE = 100;

    // Grid array: false = dead, true = alive
    Color[][] renderBuffer = new Color[BOARDWIDTH][BOARDHEIGHT];

    int[][] sphereData = new int[4][1]; // x, y, z, and radius. 3 wide

    int camX = 0;
    int camY = 0;
    int camZ = -60;
    
    float fOV = 50;

    private BufferedImage buffer;
    public pathTracer() {
        setTitle("Path Tracer");
        setSize((BOARDHEIGHT * CELLSIZE) + 20, (BOARDWIDTH * CELLSIZE) + 60);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. Create and Set the Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem startItem = new JMenuItem("Start");
        JMenuItem pauseItem = new JMenuItem("Pause");
        JMenuItem resetItem = new JMenuItem("Reset");

        fileMenu.add(startItem);
        fileMenu.add(pauseItem);
        fileMenu.addSeparator();
        fileMenu.add(resetItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);

        addSphere(0, 0, 0, 50, 0); // add a sphere at 0,0,0 with a radius of 10 at thr first slot

        // 2. Create the Custom Grid Panel
        renderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw cells
                super.paintComponent(g);

                if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
                    buffer = new BufferedImage(BOARDWIDTH, BOARDHEIGHT, BufferedImage.TYPE_INT_ARGB); // create the image buffer, it is the size of the grid
                } // every cell is rendered as one pixel then upscaled

                Graphics2D g2d = buffer.createGraphics();
                // Draw cells
                for (int y = 0; y < BOARDHEIGHT; y++){ // display the grid
                    for (int x = 0; x < BOARDWIDTH; x++){
                        buffer.setRGB(x , y, renderBuffer[x][y].getRGB()); // render the cell as a single pixel
                    }
                }

                g2d.dispose(); // dispose the buffer, prevents to much garbage

                Graphics2D gScreen = (Graphics2D) g; // scale the low res buffer th the desired screen resoloutiuon
                gScreen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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

    public double[] sendRay(int startX, int startY, int startZ, double dirX, double dirY, double dirZ){
        int sphereX = sphereData[0][0];
        int sphereY = sphereData[1][0];
        int sphereZ = sphereData[2][0];
        int sphereR = sphereData[3][0];

        double deltaX = sphereX - startX; // minus xyz of the sphere
        double deltaY = sphereY - startY;
        double deltaZ = sphereZ - startZ;
        
        double[] collisionData = new double[4]; // stored as t1, normalx, normaly, normalz
        
        double length = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));

        double tC = dot(deltaX, deltaY, deltaZ, dirX, dirY, dirZ);
        if (tC < 0){ // the ray isn't goint to hit the sphere, wrong dir
            collisionData[0] = -1;
            return collisionData;
        }
        
        double d = Math.sqrt((length * length) - (tC * tC));
        if (d > sphereR){ // the ray will miss the sphere
            collisionData[0] = -1;
            return collisionData;
        }
        
        double t1C = Math.sqrt((sphereR * sphereR) - (d * d));
        double t1 = tC - t1C;
        double t2 = tC + t1C;
        
        // compute the normal of the hit
        double hitX = startX + (dirX * t1);
        double hitY = startY + (dirY * t1);
        double hitZ = startZ + (dirZ * t1);
        
        double normalLength = Math.sqrt((hitX * hitX) + (hitY * hitY) + (hitZ * hitZ)); // normalise the normal vector
        hitX /= normalLength;
        hitY /= normalLength;
        hitZ /= normalLength;
        
        collisionData[0] = t1;
        collisionData[1] = hitX;
        collisionData[2] = hitY;
        collisionData[3] = hitZ;
        
        return collisionData;
    }
    
    public double dot(double a1, double a2, double a3, double b1, double b2, double b3){
        return (a1 * b1) + (a2 * b2) + (a3 * b3);
    }

    public void addSphere(int x, int y, int z, int radius, int index){
        sphereData[0][index] = x;
        sphereData[1][index] = y;
        sphereData[2][index] = z;
        sphereData[3][index] = radius;
    }

    public void tickLoop(){
        while (true){
            for (int y = 0; y < BOARDHEIGHT; y ++){
                for (int x = 0; x < BOARDWIDTH; x ++){
                    double sendX = x - BOARDWIDTH / 2;
                    double sendY = y - BOARDHEIGHT / 2;
                    double sendZ = fOV;
                    double normalLength = Math.sqrt((sendX * sendX) + (sendY * sendY) + (sendZ * sendZ));
                    sendX /= normalLength;
                    sendY /= normalLength;
                    sendZ /= normalLength;

                    double[] rayInfo = sendRay(camX, camY, camZ, sendX, sendY, sendZ);
                    
                    int colourX = (int)(((rayInfo[1] + 1) / 2) * 255);
                    int colourY = (int)(((rayInfo[2] + 1) / 2) * 255);
                    int colourZ = (int)(((rayInfo[3] + 1) / 2) * 255);
                    
                    Color testColour = new Color(colourX, colourY, colourZ);
                    if (rayInfo[0] > 0)
                        renderBuffer[x][y] = testColour;
                    else
                        renderBuffer[x][y] = getBackGround(sendY);
                }
            }
            renderPanel.repaint();
        }
    }
    
    public Color getBackGround(double yIn){
        double blend = (yIn / 4) + 0.75;
        
        return new Color((int)(255 * blend), (int)(255 * blend), 255);
    }
}
