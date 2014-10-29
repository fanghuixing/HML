package DataSet;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.freehep.graphicsio.ps.EPSGraphics2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.w3c.dom.DOMImplementation;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Properties;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-29.
 */
public  class UnifiedActionListener implements ActionListener {
    public static enum ExportType{
        PDF, EPS, SVG
    }

    private static ExportType exportType;
    ApplicationFrame applicationFrame;
    ChartPanel chartPanel;

    public static final String EXIT_COMMAND = "EXIT";


    protected UnifiedActionListener(ApplicationFrame applicationFrame, ChartPanel chartPanel) {
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
            exportType = ExportType.PDF;
            exportToFile(ExportType.PDF);
        } else if (command.equals(EXIT_COMMAND)) {
            attemptExit();
        } else if (command.equals("EXPORT_TO_EPS")){
            exportType = ExportType.EPS;
            exportToFile(ExportType.EPS);
        } else if (command.equals("EXPORT_TO_SVG")) {
            exportType = ExportType.SVG;
            exportToFile(ExportType.SVG);
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
    private void exportToFile(final ExportType type) {
        Component c = chartPanel;
        final String suffix = "."+type.toString().toLowerCase();
        if (c instanceof ChartPanel) {
            JFileChooser fc = new JFileChooser();
            fc.setName("untitled"+suffix);
            fc.setFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(suffix);
                }

                public String getDescription() {
                    return type.toString();
                }});
            int result = fc.showSaveDialog(applicationFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                ChartPanel cp = (ChartPanel) c;
                try {
                    JFreeChart chart = (JFreeChart) cp.getChart().clone();
                    ExportTask t = new ExportTask(chart, cp.getWidth(),
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
            JOptionPane.showMessageDialog(applicationFrame, message, type + " Export",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void saveChartAsPDF(File file, JFreeChart chart, int width, int height, FontMapper mapper) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        writeChartAsPDF(out, chart, width, height, mapper);
        out.close();
    }

    public static void writeChartAsPDF(OutputStream out,JFreeChart chart,int width,int height,FontMapper mapper)
            throws IOException {
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


    public static void saveChartAsSVG(File file, JFreeChart chart, int width, int height) throws IOException {


        DOMImplementation domImpl
                = GenericDOMImplementation.getDOMImplementation();
        // Create an instance of org.w3c.dom.Document
        org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        // set the precision to avoid a null pointer exception in Batik 1.5
        svgGenerator.getGeneratorContext().setPrecision(6);
        // Ask the chart to render into the SVG Graphics2D implementation
        svgGenerator.setSVGCanvasSize(new Dimension(width, height));
        Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);

        //Graphics2D graphics2D = (Graphics2D) svgGenerator.create();
        chart.draw(svgGenerator, r2D);
        // Finally, stream out SVG to a file using UTF-8 character to
        // byte encoding
        boolean useCSS = true;
        Writer out = new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8");
        svgGenerator.stream(out, useCSS);

    }


    private static int resize(int size ) {
        return (int) (size*0.8);
    }

    public static void saveChartAsEPS(File outfile, JFreeChart chart, int width, int height) throws IOException{
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

    static class ExportTask implements Runnable {

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
        public ExportTask(JFreeChart chart, int width, int height,
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
                if (exportType==ExportType.EPS) {
                    saveChartAsEPS(this.file, chart, width, height);
                }
                else if (exportType==ExportType.PDF) {
                    saveChartAsPDF(this.file, chart, width, height, new DefaultFontMapper());
                }
                else {
                    saveChartAsSVG(this.file, chart, width, height);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
