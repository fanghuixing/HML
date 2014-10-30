package DataSet;

import AntlrGen.JSONLexer;
import AntlrGen.JSONParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * HML DataSet, Show different color and line shape for different modes
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-27.
 */
public class ParseJSONData extends ApplicationFrame {
    private static Logger  logger = LogManager.getLogger(ParseJSONData.class);

    private static ParseTree tree;
    /** Exit action command. */
    public static final String EXIT_COMMAND = "EXIT";

    private  ChartPanel chartPanel;
    private ActionListener listener;
    private float[][] pattern = {{10.0f},{4.0f,1.0f},{7.0f,2.0f,3.0f,7.0f},{1.0f,2.0f}};
    // The HML model file path
    private static String modelPath = "./JSON/src/data.json";

    List<Double> globalTime = new ArrayList<Double>();
    int maxDepth;
    private NumberFormat numFormater = NumberFormat.getNumberInstance();



    List<YIntervalSeriesCollection> seriesCollectionList = new ArrayList<YIntervalSeriesCollection>();


    public ParseJSONData(String title, List<String> variables, HashMap<String, VariableWithData> dataHashMap) {
        super(title);
        XYDataset dataSet = createDataSet(variables, dataHashMap);

        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Global Time"));

        plot.setGap(5.0);

        for (YIntervalSeriesCollection data : seriesCollectionList) {
            String des = data.getSeries(0).getKey().toString();
            subplot(des, data, plot);
        }
        subplot("ALL", dataSet, plot);

        plot.setOrientation(PlotOrientation.VERTICAL);
        List<Color> colors = new ArrayList<Color>();
        colors.add(Color.black);
        colors.add(Color.blue);
        //colors.add(Color.green);


        List<XYPlot> subplots = (List<XYPlot>) plot.getSubplots();

        int index = 0;
        numFormater.setMinimumFractionDigits(10);
        for (XYPlot sp : subplots) {
            sp.getRenderer().setBaseToolTipGenerator(new StandardXYToolTipGenerator());
            //new StandardXYToolTipGenerator("{0} : [ {1},  {2}]", numFormater, numFormater);
            for (int i = 0; i<sp.getSeriesCount(); i++) {


                if (i % (maxDepth+1) == 0) index = 0;
                Paint color = colors.get(index);
                // From one mode to another, we will change the color
                sp.getRenderer().setSeriesPaint(i, color);
                // The shape of the line is solid or dashed

                if (i % 2 == 0 || i % (maxDepth+1) == 0)
                    sp.getRenderer().setSeriesStroke(i, getStroke(2.5F, "solid"));
                else
                    sp.getRenderer().setSeriesStroke(i, getStroke(2.5F, "dashed"));
                index = (index +1 ) % colors.size();
            }

        }

        JFreeChart chart = new JFreeChart("HML Data Chart",JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        //chart.removeLegend();


        chartPanel = new ChartPanel(chart, true,true,true,true, true);
        int width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int height = Toolkit.getDefaultToolkit().getScreenSize().height;
        chartPanel.setPreferredSize(new java.awt.Dimension(width, height));
        chartPanel.setMouseZoomable(true, false);
        chartPanel.setDisplayToolTips(true);


        Crosshair xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
        Crosshair yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
        chartPanel.addChartMouseListener(new HMLChartMouseListener(xCrosshair, yCrosshair, chartPanel));

        CrosshairOverlay crosshairOverlay = new HMLCrosshairOverlay();
        xCrosshair.setLabelVisible(true);
        yCrosshair.setLabelVisible(true);
        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);


        chartPanel.addOverlay(crosshairOverlay);





        //Set menu of exporting image to PDF, in the file menu
        listener = new UnifiedActionListener(this, chartPanel);
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        //Add the export menu (PDF) into right-click pop-menu
        JMenuItem exportToPDF = new JMenuItem("PDF...");
        exportToPDF.setActionCommand("EXPORT_TO_PDF");
        exportToPDF.addActionListener(listener);


        //Add the export menu (EPS) into right-click pop-menu
        JMenuItem exportToEPS = new JMenuItem("EPS...");
        exportToEPS.setActionCommand("EXPORT_TO_EPS");
        exportToEPS.addActionListener(listener);



        //Add the export menu (SVG) into right-click pop-menu
        JMenuItem exportToSVG = new JMenuItem("SVG...");
        exportToSVG.setActionCommand("EXPORT_TO_SVG");
        exportToSVG.addActionListener(listener);



        JPopupMenu popupMenu = chartPanel.getPopupMenu();
        JMenu subcom = (JMenu) popupMenu.getComponent(3);
        subcom.add(exportToPDF);
        subcom.add(exportToEPS);
        subcom.add(exportToSVG);







        setContentPane(chartPanel);



    }


    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // first the file menu
        JMenu fileMenu = new JMenu("File", true);
        fileMenu.setMnemonic('F');

        JMenuItem exportToPDF = new JMenuItem("Export to PDF...", 'p');
        exportToPDF.setActionCommand("EXPORT_TO_PDF");

        exportToPDF.addActionListener(listener);
        fileMenu.add(exportToPDF);

        fileMenu.addSeparator();


        JMenuItem exportToEPS = new JMenuItem("Export to EPS...", 'e');
        exportToEPS.setActionCommand("EXPORT_TO_EPS");
        exportToEPS.addActionListener(listener);
        fileMenu.add(exportToEPS);

        fileMenu.addSeparator();


        JMenuItem exportToSVG = new JMenuItem("Export to SVG...", 's');
        exportToSVG.setActionCommand("EXPORT_TO_SVG");
        exportToSVG.addActionListener(listener);
        fileMenu.add(exportToSVG);

        fileMenu.addSeparator();


        JMenuItem exitItem = new JMenuItem("Exit", 'x');
        exitItem.setActionCommand(EXIT_COMMAND);
        exitItem.addActionListener(listener);
        fileMenu.add(exitItem);

        // finally, glue together the menu and return it
        menuBar.add(fileMenu);

        return menuBar;
    }

    private void subplot(String des, XYDataset data, CombinedDomainXYPlot plot ){
        DeviationRenderer deviationrenderer = new DeviationRenderer(true, false);

        for (int i=0; i<data.getSeriesCount(); i++) {
            deviationrenderer.setSeriesFillPaint(i, new Color(100, 100, 255));
        }
        NumberAxis rangeAxis = new NumberAxis(des);
        XYPlot subplot = new XYPlot(data, null, rangeAxis, deviationrenderer);

        //subplot.setInsets(new RectangleInsets(5D, 5D, 5D, 5D));
        subplot.setInsets(new RectangleInsets(5D, 5D, 5D, 20D));
        subplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        plot.add(subplot, 1);
    }

    public XYDataset createDataSet(List<String> variables, HashMap<String, VariableWithData> dataHashMap) {
        //XYSeriesCollection dataSet = new XYSeriesCollection();
        YIntervalSeriesCollection dataSet = new YIntervalSeriesCollection();
        golobalTimeVar(dataSet, dataHashMap);
        variables.remove("global");
        for (String name : variables)
            createDataSetForOneVariable(name, dataSet, dataHashMap);
        return dataSet;
    }

    private void createDataSetForOneVariable(String name, YIntervalSeriesCollection dataSet, HashMap<String, VariableWithData> dataHashMap){
        int depth = 0;
        //XYSeries series = null;
        YIntervalSeriesCollection seriesCollection = new YIntervalSeriesCollection();
        seriesCollectionList.add(seriesCollection);
        YIntervalSeries series = null;
        double old_value = 0;
        while (true) {
            String key = name + "_" + depth + "_0";
            VariableWithData variableWithData = dataHashMap.get(key);

            if (variableWithData == null) {
                if (depth>maxDepth) break;
                else {
                    VariableWithData clock = dataHashMap.get("clock"+ "_" + depth + "_0");
                    int itemCount = clock.values.getItemCount();
                    series = new YIntervalSeries(name+depth, false, true);
                    dataSet.addSeries(series);
                    seriesCollection.addSeries(series);
                    for (int i=0; i<itemCount; i++) {
                        series.add(clock.values.getX(i).doubleValue() + globalTime.get(depth - 1), old_value, old_value, old_value);
                    }
                }
            }
            else {
                if (depth == 0) {
                    series = variableWithData.values;
                    series.setKey(name);
                    dataSet.addSeries(series);

                    seriesCollection.addSeries(series);
                } else {
                    series = new YIntervalSeries(name+depth, false, true);
                    dataSet.addSeries(series);
                    seriesCollection.addSeries(series);

                    int itemCount = variableWithData.values.getItemCount();
                    for (int i=0; i<itemCount; i++) {
                        addSeries(series, variableWithData, i, globalTime.get(depth - 1));
                    }

                }
            }
            int size = series.getItemCount();
            old_value = series.getYValue(size-1);
            depth++;

        }
    }

    private void addSeries(YIntervalSeries series, VariableWithData variableWithData, int i, double base){

        series.add(variableWithData.values.getX(i).doubleValue() + base,
                variableWithData.values.getYValue(i),
                variableWithData.values.getYLowValue(i),
                variableWithData.values.getYHighValue(i));
    }

    private void golobalTimeVar(YIntervalSeriesCollection dataSet, HashMap<String, VariableWithData> dataHashMap){
        int depth = 0;
        //XYSeries series = null;
        YIntervalSeriesCollection seriesCollection = new YIntervalSeriesCollection();
        seriesCollectionList.add(seriesCollection);
        YIntervalSeries series = null;
        while (true) {
            String key = "global" + "_" + depth + "_0";
            VariableWithData variableWithData = dataHashMap.get(key);

            if (variableWithData == null) {
                maxDepth = depth-1;
                break;
            }
            if (depth == 0) {
                series = variableWithData.values;
                series.setKey("global");
                dataSet.addSeries(series);

                seriesCollection.addSeries(series);
            } else {
                int itemCount = variableWithData.values.getItemCount();
                series = new YIntervalSeries("global"+depth, false, true);
                dataSet.addSeries(series);
                seriesCollection.addSeries(series);
                for (int i=0; i<itemCount; i++) {
                    addSeries(series, variableWithData, i, 0);
                }

            }

            globalTime.add(series.getYHighValue(series.getItemCount()-1));
            depth++;
        }
    }




    public static void showData(String dataPath) throws Exception {
        InputStream inputStream = new FileInputStream(dataPath);
        ANTLRInputStream input = new ANTLRInputStream(inputStream);
        JSONLexer lexer = new JSONLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);
        parser.setBuildParseTree(true);
        tree = parser.jsonData();

        CollectData collectData = new CollectData();
        collectData.visit(tree);

        List<String> variables = collectData.getVariables();
        HashMap<String, VariableWithData> dataHashMap = collectData.getDataHashMap();

        ParseJSONData demo = new ParseJSONData("Line Chart HML", variables, dataHashMap);

        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }

    public static void start(String path){
        DrawTask dt = new DrawTask(path);
        SwingUtilities.invokeLater(dt);
    }

    public static void main(String[] args) throws Exception{

        ParseJSONData.showData("./JSON/src/data.json");
    }

    private Stroke getStroke(float width, String type){
        Stroke[] strokes = new Stroke[] {
                new BasicStroke(width, BasicStroke.CAP_BUTT,  BasicStroke.JOIN_MITER, 10.0f                  ), // solid line
                new BasicStroke(width, BasicStroke.CAP_BUTT,  BasicStroke.JOIN_MITER, 10.0f, pattern[1], 0.0f), // dashed line
                new BasicStroke(width, BasicStroke.CAP_BUTT,  BasicStroke.JOIN_MITER, 10.0f, pattern[2], 0.0f), // dash-dotted line
                new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, pattern[3], 0.0f), // dotted line
        };
        if (type.equals("solid")) return strokes[0];
        else if (type.equals("dashed")) return strokes[1];
        else if (type.equals("dash-dotted")) return strokes[2];
        else if (type.equals("dotted")) return strokes[3];
        else return strokes[0];
    }


    static class DrawTask implements Runnable{
        private String path;

        public DrawTask(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                showData(path);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }


}
