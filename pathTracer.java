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

    public int sendRay(int startX, int startY, int startZ, int dirX, int dirY, int dirZ){
        int rayX = startX;
        int rayY = startY;
        int rayZ = startZ;

        double deltaX;
        double deltaY;
        double deltaZ;

        deltaX = sphereData[0][0] - startX; // minus xyz of the sphere
        deltaY = sphereData[1][0] - startY;
        deltaZ = sphereData[2][0] - startZ;

        double tC = dot(deltaX, deltaY, deltaZ, dirX, dirY, dirZ);
        
        System.out.println(deltaX);
        System.out.println(deltaY);
        System.out.println(deltaZ);
        System.out.println(tC);
        return (int)tC;
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
                    //Color testColour = new Color(x / 2, y / 2, (int)Math.round(Math.random()*255));
                    int value = sendRay(x - BOARDWIDTH / 2, y - BOARDHEIGHT / 2, camZ, 0, 0, 1) + (int)(Math.random()*5);
                    Color testColour = new Color(value, value, value);
                    renderBuffer[x][y] = testColour;
                }
            }
            renderPanel.repaint();
        }
    }

    //public static void main(String[] args) {
    // Run on the Event Dispatch Thread for thread safety
    //     SwingUtilities.invokeLater(pathTracer::new);
    //}
}
