package rssi_throughput_values;

import java.util.Arrays;

public class num_of_samples_init {
    public static int[][] num_of_samples_initialization(){ // num_of_samples array initializing
        int[][] num_of_samples = new int[4][10];
        //initialize array of samples
        for (int[] i : num_of_samples)
            Arrays.fill(i, 0);
        return num_of_samples;
    }
}
