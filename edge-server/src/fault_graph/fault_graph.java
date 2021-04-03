package fault_graph;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.util.List;


public class fault_graph extends JFrame {
    public fault_graph(final String title, List<Double> faults, double last) { //gets a title, a list of faults and the timestamp from the last fault

        super(title);
        final XYSeries series = new XYSeries("Estimation fault");
        double first=last-faults.size()+1;
        double average=0; //average fault calculation
        for(int i=0;i<faults.size();i++){
            series.add(first+i,faults.get(i));
            average=average+faults.get(i);
        }
        average=average/faults.size();
        System.out.println("Average Fault = "+average+" m");
        final XYSeriesCollection data = new XYSeriesCollection(series);
        final JFreeChart chart = ChartFactory.createXYLineChart( //here we create a jfree chart for the fault per timestamp
                "Average Fault = "+average+" m",
                "Timestamp",
                "Fault in m",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        final ChartPanel chartPanel = new ChartPanel(chart);  //we create a panel to show the chart
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 400));
        setContentPane(chartPanel);

    }
}