package DataSet;

import org.freehep.graphicsio.ps.EPSGraphics2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-29.
 */
public class SaveEPSActionListener  implements ActionListener {
    ApplicationFrame applicationFrame;
    ChartPanel chartPanel;


    public SaveEPSActionListener(ApplicationFrame applicationFrame, ChartPanel chartPanel) {
        this.applicationFrame = applicationFrame;
        this.chartPanel = chartPanel;
    }

    /**
     * Handles menu selections by passing control to an appropriate method.
     *
     * @param event the event.
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();
        if (command.equals("EXPORT_TO_EPS")) {
            exportToEPS();
        }
    }

    /**
     * Opens a "Save As..." dialog, inviting the user to save the selected
     * chart to a file in EPS format.
     */
    private void exportToEPS() {
        Component c = chartPanel;
        if (c instanceof ChartPanel) {
            JFileChooser fc = new JFileChooser();
            fc.setName("untitled.eps");
            fc.setFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(".eps");
                }

                public String getDescription() {
                    return "Encapsulated PostScript (EPS)";
                }});
            int result = fc.showSaveDialog(applicationFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                ChartPanel cp = (ChartPanel) c;
                try {
                    JFreeChart chart = (JFreeChart) cp.getChart().clone();
                    EPSExportTask t = new EPSExportTask(chart, cp.getWidth(),
                            cp.getHeight(), fc.getSelectedFile());
                    SwingUtilities.invokeLater(t);
                }
                catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            String message = "Unable to export the selected item.  There is ";
            message += "either no chart selected,\nor else the chart is not ";
            message += "at the expected location in the component hierarchy\n";
            message += "(future versions of the demo may include code to ";
            message += "handle these special cases).";
            JOptionPane.showMessageDialog(applicationFrame, message, "EPS Export",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }



    public static void saveChartAsEPS(File outfile, JFreeChart chart, int width,
                                      int height) throws IOException{

        if (width>1000) {
            width = (int) (width*0.8);
            height = (int) (height*0.8);
        }

        OutputStream out = new java.io.FileOutputStream(outfile);
        EPSGraphics2D g2d = new EPSGraphics2D(out, new Dimension(width, height));
        Properties p = new Properties();
        p.setProperty("PageSize","A4");
        g2d.setProperties(p);
        g2d.startExport();
        Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
        chart.draw(g2d,r2D);
        g2d.endExport();


        out.flush();
        out.close();

    }


    static class EPSExportTask implements Runnable {

        JFreeChart chart;

        int width;

        int height;

        File file;

        /**
         * A task that exports a chart to a file in EPS format using iText.
         *
         * @param chart  the chart.
         * @param width  the width.
         * @param height  the height.
         * @param file  the file.
         */
        public EPSExportTask(JFreeChart chart, int width, int height,
                             File file) {
            this.chart = chart;
            this.file = file;
            this.width = width;
            this.height = height;
            chart.setBorderVisible(true);
            chart.setPadding(new RectangleInsets(2, 2, 2, 2));
        }

        public void run() {
            try {
                saveChartAsEPS(this.file, chart, width, height);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
