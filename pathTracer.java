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

    // Grid array: false = dead, true = alive
    private Color[][] renderBuffer = new Color[BOARDWIDTH][BOARDHEIGHT];

    private BufferedImage buffer;
    public pathTracer() {
        setTitle("Game of Life");
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

    public void tickLoop(){
        while (true){
            for (int y = 0; y < BOARDHEIGHT; y ++){
                for (int x = 0; x < BOARDWIDTH; x ++){ 
                    Color testColour = new Color(x / 2, y / 2, (int)Math.round(Math.random()*255));
                    renderBuffer[x][y] = testColour;
                }
            }
            renderPanel.repaint();
        }
    }

    public static void main(String[] args) {
        // Run on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(pathTracer::new);
    }
}
