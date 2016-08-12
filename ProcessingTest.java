import javax.swing.JFrame;

import processing.awt.PSurfaceAWT.SmoothCanvas;
import processing.core.PApplet;
import processing.core.PSurface;

public class ProcessingTest {
    public static void main(String... args){

        //create your JFrame
        JFrame frame = new JFrame("JFrame Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //create your sketch
        PMate pt = new PMate();

        //get the PSurface from the sketch
        PSurface ps = pt.getPSurface();

        //initialize the PSurface
        ps.setSize(800, 600);

        //get the SmoothCanvas that holds the PSurface
        SmoothCanvas smoothCanvas = (SmoothCanvas)ps.getNative();

        //SmoothCanvas can be used as a Component
        frame.add(smoothCanvas);

        //make your JFrame visible
        frame.setSize(800, 600);
        frame.setVisible(true);

        //start your sketch
        ps.startThread();
    }
}
