package DataSet;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;
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
import java.io.*;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-29.
 */
public class SavePDFActionListener implements ActionListener {
    ApplicationFrame applicationFrame;
    ChartPanel chartPanel;

    public static final String EXIT_COMMAND = "EXIT";


    public SavePDFActionListener(ApplicationFrame applicationFrame, ChartPanel chartPanel) {
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
        if (command.equals("EXPORT_TO_PDF")) {
            exportToPDF();
        } else if (command.equals(EXIT_COMMAND)) {
            attemptExit();
        }
    }

    /**
     * Exits the application, but only if the user agrees.
     */
    private void attemptExit() {

        String title = "Confirm";
        String message = "Are you sure you want to exit the HML Data Viewer?";
        int result = JOptionPane.showConfirmDialog(
                applicationFrame, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
            applicationFrame.dispose();
            System.exit(0);
        }
    }

    /**
     * Opens a "Save As..." dialog, inviting the user to save the selected
     * chart to a file in PDF format.
     */
    private void exportToPDF() {
        Component c = chartPanel;
        if (c instanceof ChartPanel) {
            JFileChooser fc = new JFileChooser();
            fc.setName("untitled.pdf");
            fc.setFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(".pdf");
                }

                public String getDescription() {
                    return "Portable Document Format (PDF)";
                }});
            int result = fc.showSaveDialog(applicationFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                ChartPanel cp = (ChartPanel) c;
                try {
                    JFreeChart chart = (JFreeChart) cp.getChart().clone();
                    PDFExportTask t = new PDFExportTask(chart, cp.getWidth(),
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
            JOptionPane.showMessageDialog(applicationFrame, message, "PDF Export",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }


    static class PDFExportTask implements Runnable {

        JFreeChart chart;

        int width;

        int height;

        File file;

        /**
         * A task that exports a chart to a file in PDF format using iText.
         *
         * @param chart  the chart.
         * @param width  the width.
         * @param height  the height.
         * @param file  the file.
         */
        public PDFExportTask(JFreeChart chart, int width, int height,
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
                saveChartAsPDF(this.file, chart, width, height,
                        new DefaultFontMapper());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Saves a chart to a file in PDF format using iText.
     *
     * @param file  the file.
     * @param chart  the chart.
     * @param width  the chart width.
     * @param height  the chart height.
     * @param mapper  the font mapper.
     *
     * @throws IOException if there is an I/O problem.
     */
    public static void saveChartAsPDF(File file,
                                      JFreeChart chart,
                                      int width,
                                      int height,
                                      FontMapper mapper) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        writeChartAsPDF(out, chart, width, height, mapper);
        out.close();
    }

    /**
     * Writes a chart to an output stream in PDF format using iText.
     *
     * @param out  the output stream.
     * @param chart  the chart.
     * @param width  the chart width.
     * @param height  the chart height.
     * @param mapper  the font mapper.
     *
     * @throws IOException if there is an I/O problem.
     */
    public static void writeChartAsPDF(OutputStream out,
                                       JFreeChart chart,
                                       int width,
                                       int height,
                                       FontMapper mapper) throws IOException {
        com.lowagie.text.Rectangle pagesize = new com.lowagie.text.Rectangle(width, height);
        Document document = new Document(pagesize, 50, 50, 50, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.addAuthor("Huixing Fang");
            document.addSubject("HML");
            document.addCreator("Huixing Fang");
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height);
            Graphics2D g2 = tp.createGraphics(width, height, mapper);
            Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
            chart.draw(g2, r2D);
            g2.dispose();
            cb.addTemplate(tp, 0, 0);
        }
        catch (DocumentException de) {
            System.err.println(de.getMessage());
        }
        document.close();
    }


}


