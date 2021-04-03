package rssi_throughput_values;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static rssi_throughput_values.num_of_samples_init.num_of_samples_initialization;

public class average_throughput {
    public static double[][] average_throughput(String filename, double lat_min_y, double lat_max_y, double long_min_x, double long_max_x){
        double[][] thr = new double[4][10];
        int[][] num_of_samples =num_of_samples_initialization();

        for (double[] i : thr)
            Arrays.fill(i, 0.0);

        //calculates grid's lat & long
        BigDecimal lat_distance = BigDecimal.valueOf(lat_max_y).subtract(BigDecimal.valueOf(lat_min_y));
        BigDecimal long_distance = BigDecimal.valueOf(long_max_x).subtract(BigDecimal.valueOf(long_min_x));
        BigDecimal lat_grid_size = lat_distance.divide(BigDecimal.valueOf(4), 7, RoundingMode.CEILING);
        BigDecimal long_grid_size = long_distance.divide(BigDecimal.valueOf(10), 7, RoundingMode.CEILING);

        BufferedReader fileReader;

        //delimiter used in CSV file
        final String DELIMITER = ",";
        try {
            String line;
            //create the file reader
            fileReader = new BufferedReader(new FileReader(filename));
            //read the file line by line
            while ((line = fileReader.readLine()) != null) {
                //Get all tokens available in line
                String[] tokens = line.split(DELIMITER);
                double lat = Double.parseDouble(tokens[2]);
                double lng = Double.parseDouble(tokens[3]);
                double throughput = Double.parseDouble(tokens[7]);

                //find in which cell coordinates match
                BigDecimal y = BigDecimal.valueOf(lat).subtract(BigDecimal.valueOf(lat_min_y)).divide(lat_grid_size, 0, RoundingMode.FLOOR);//0to4
                BigDecimal x = BigDecimal.valueOf(lng).subtract(BigDecimal.valueOf(long_min_x)).divide(long_grid_size, 0, RoundingMode.FLOOR);//0to10

                //check if x or y is off limits to reject them
                if (y.compareTo(BigDecimal.valueOf(0.0)) < 0 || y.compareTo(BigDecimal.valueOf(4.0)) >= 0)
                    continue;
                if (x.compareTo(BigDecimal.valueOf(0.0)) < 0 || x.compareTo(BigDecimal.valueOf(10.0)) >= 0)
                    continue;

                //add data on the throughput array
                thr[y.intValue()][x.intValue()] = thr[y.intValue()][x.intValue()] + throughput;
                num_of_samples[y.intValue()][x.intValue()] = num_of_samples[y.intValue()][x.intValue()] + 1;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //fill every cell of the throughput array with its average value
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                thr[i][j] = thr[i][j] / num_of_samples[i][j];
            }
        }
        return thr; //return 2d array
    }
}
