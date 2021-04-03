package heatmaps;

import project.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static heatmaps.draw_and_fill_grid.Draw_and_Fill_Grid;

public class rssi_heatmap {
    public static class RSSI_HeatMap extends JPanel { //class for RSSI heatmap
        BufferedImage img;
        private double[][] rssi_values;
        public RSSI_HeatMap(double[][] rssi_values) { //class constructor to import image for the background of the heatmap
            try {
                this.rssi_values=rssi_values; //intitialization of rssi average values
                img = ImageIO.read(new FileInputStream("InputData/Map.png"));
            } catch (IOException ex) { //
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        protected void paintComponent(Graphics g) { //edit paintcomponent for the RSSI heatmap
            super.paintComponent(g);
            if (img != null) {
                g.drawImage(img, 0, 0, this);
                //take height and width of the image
                int height=getHeight();
                int width= getWidth();
                //call function with parameters the graphic type g variable, width, height and the average values of rssi
                Draw_and_Fill_Grid(g,height,width,rssi_values,"RSSI");
            }else{
                System.out.println("Wrong file path!");
            }
        }

        @Override
        public Dimension getPreferredSize() { //function to take picture's dimensions
            if (img == null){
                System.out.println("Wrong file path!");
                return new Dimension(300, 300);
            }else{
                return new Dimension(img.getWidth(), img.getHeight());
            }
        }
    }
}
