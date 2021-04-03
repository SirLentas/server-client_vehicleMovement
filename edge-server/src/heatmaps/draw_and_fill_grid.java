package heatmaps;

import java.awt.*;

import static heatmaps.create_gradient.createGradient;

public class draw_and_fill_grid {
    public static void Draw_and_Fill_Grid(Graphics g, int height, int width, double[][] average_values, String mode) { //function to draw the grid and fill it with the right color
        Color[] color_gradient = createGradient(Color.RED, Color.GREEN, 10); //return color gradient with 10 different colors
        Color c;
        int x_position;
        int y_position;
        int color_gradient_column;
        int reverse_y;
        width=width-155;
        int cellHeight = (height / 4); //take image's height and divide it by 4 to take height of every cell, in our image is 68
        int cellWidth = (width / 10); //take image's width and divide it by 10 to take width of every cell, in our image is 133
        for (int y = 0; y < height; y += cellHeight) {
            y_position = y / cellHeight; //cell position in y axes
            if (y_position == 4) { //if some of them are out of range we put them on the last y cell
                y_position = 3;
            }
            for (int x = 0; x < cellWidth*10; x += cellWidth) {
                if(x<cellWidth*9){
                    g.drawRect(x, y, cellWidth, cellHeight); //draw grid lines
                }else{
                    g.drawRect(x, y, width-x, cellHeight); //draw grid lines
                }
                x_position = x / cellWidth; //cell position in x axes
                if (x_position == 10) { //if some of them are out of range we put them on the last x cell
                    x_position = 9;
                }
                reverse_y = Math.abs(y_position - 3); //we reverse the y axes because times of y axes are reversed
                //we choose the right color cell on the color array depends on th average value of the array(rssi or throughput)
                if(mode=="RSSI"){
                    color_gradient_column = ((int) (Math.round((average_values[reverse_y][x_position]-20)*1.25) * 10) / 100);
                }else if(mode=="THR"){
                    color_gradient_column = ((int) (Math.round((average_values[reverse_y][x_position]-10)*2.5) * 10) / 100);
                }else{
                    return;
                }
                // we put the values of the color on the temp variable
                c = new Color(color_gradient[color_gradient_column].getRed(), color_gradient[color_gradient_column].getGreen(), color_gradient[color_gradient_column].getBlue(), 200);
                g.setColor(c);
                if(x<cellWidth*9){
                    g.fillRect(x, y, cellWidth, cellHeight); //fill the grid with color
                }else{
                    g.fillRect(x, y, width-x, cellHeight); //fill the grid with color
                }
            }
        }
        g.drawRect(width, 0, 156, height); //draw grid lines
        g.setColor(Color.white);
        g.fillRect(width, 0, 156, height); //fill the grid with color
        width=width+8;
        height=height-20;
        for(int i=0;i<10;i++){
//            g.drawRect(width, i*(height/10)+10, 140, height/10); //draw grid lines
            c = new Color(color_gradient[9-i].getRed(), color_gradient[9-i].getGreen(), color_gradient[9-i].getBlue(), 200);
            g.setColor(c);
            g.fillRect(width, i*(height/10)+10, 140, height/10); //fill the grid with color
            int f=90-i*10;
            int t=f+9;
            if(t==99){
                t=100;
            }
            String text=f+" - "+t+" %";
            g.setColor(Color.WHITE);
            g.drawString(text,width+20, (i+1)*(height/10)+5);
        }

    }
}
