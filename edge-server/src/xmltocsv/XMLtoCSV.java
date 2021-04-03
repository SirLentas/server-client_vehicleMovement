package xmltocsv;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

public class XMLtoCSV {
    public static void XMLtoCSV(String fname) { //convert .xml to .csv
        //fname is the name of the file without extension
        //create path for .xml and .csv
        String XMLname = "InputData/" + fname + ".xml";
        String CSVname = fname + ".csv";
        try {
            File file = new File(XMLname);

            FileWriter csvWriter = new FileWriter(CSVname);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("timestep"); //check every timestep on the xml file

            //info for th normal distribution
            double dev = 23.380903889; //diaspora
            double mean = 60; //average value

            for (int itr = 0; itr < nodeList.getLength(); itr++) { //for every timestep
                Element tElement = (Element) nodeList.item(itr);

                Node node = nodeList.item(itr);
                NodeList vList = node.getChildNodes();

                for (int j = 0; j < vList.getLength(); j++) {   //for every record per timestep
                    if (!(vList.item(j) instanceof Element))    //check for opening/closing tag
                        continue;
                    Element docElement = (Element) vList.item(j);

                    //double sDev = dev / 2;    //96% of sampling between 20-100
                    //double sDev=  dev/3;      //99.7% of sampling between 20-100

                    Random r = new Random();
                    double rssi = mean + (r.nextGaussian() * dev); //calculate rssi by normal distribution
                    if (rssi < 20) { //keep values inside [20,100]
                        rssi = 20;
                    } else if (rssi > 100) {
                        rssi = 100;
                    }
                    double throughput = ((rssi / 100) * 50); //calculate throughput for 50 Mbps

                    //write data to csv
                    csvWriter.append(tElement.getAttribute("time")).append(",").append(docElement.getAttribute("id")).append(",").append(docElement.getAttribute("y")).append(",").append(docElement.getAttribute("x")).append(",").append(docElement.getAttribute("angle")).append(",").append(docElement.getAttribute("speed")).append(",").append(String.valueOf(rssi)).append(",").append(String.valueOf(throughput));
                    csvWriter.append("\n");
                }
            }
            //close files
            csvWriter.flush();
            csvWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
