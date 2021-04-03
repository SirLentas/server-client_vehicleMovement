package heatmaps;

import java.awt.*;

public class create_gradient {
    public static Color[] createGradient(final Color one, final Color two, int numsteps) //create array with numsteps columns (color gradient)
    {
        //get rgb values for the lowest color we use
        int r1 = one.getRed();
        int g1 = one.getGreen();
        int b1 = one.getBlue();

        //get rgb values for the highest color we use
        int r2 = two.getRed();
        int g2 = two.getGreen();
        int b2 = two.getBlue();

        // variables to save temporarily values from the new colors
        int newR ;
        int newG ;
        int newB ;

        Color[] gradient = new Color[numsteps];
        double iNorm;
        for (int i = 0; i < numsteps; i++)
        {
            //calculate rgb values for inbetween colors
            iNorm = i / (double)numsteps; //a normalized [0:1] variable
            newR = (int) (r1 + iNorm * (r2 - r1));
            newG = (int) (g1 + iNorm * (g2 - g1));
            newB = (int) (b1 + iNorm * (b2 - b1));
            gradient[i] = new Color(newR, newG, newB);
        }
        return gradient;
    }
}
