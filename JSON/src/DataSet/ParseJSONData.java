package DataSet;

import AntlrGen.JSONLexer;
import AntlrGen.JSONParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import java.awt.*;

import java.io.FileInputStream;

import java.io.InputStream;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;


/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-27.
 */
public class ParseJSONData extends ApplicationFrame {


    private static ParseTree tree;
    // The HML model file path
    private static String modelPath = "./JSON/src/data.json";

    List<Double> globalTime = new ArrayList<Double>();
    int maxDepth;
    //List<XYSeries> seriesList = new ArrayList<XYSeries>();
    List<YIntervalSeries> seriesList = new ArrayList<YIntervalSeries>();

    public ParseJSONData(String title, List<String> variables, HashMap<String, VariableWithData> dataHashMap) {
        super(title);
        XYDataset dataSet = createDataSet(variables, dataHashMap);

        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Global Time"));
        plot.setGap(10.0);

        for (YIntervalSeries series : seriesList) {
            YIntervalSeriesCollection data = new YIntervalSeriesCollection();
            data.addSeries(series);

            String des = series.getKey().toString();
            subplot(des, data, plot);
        }
        subplot("ALL", dataSet, plot);

        plot.setOrientation(PlotOrientation.VERTICAL);
        List<Color> colors = new ArrayList<Color>();
        colors.add(Color.black);
        colors.add(Color.blue);
        colors.add(Color.green);
        colors.add(Color.red);
        colors.add(Color.darkGray);

        List<XYPlot> subplots = (List<XYPlot>) plot.getSubplots();
        int index = 0;
        for (XYPlot sp : subplots) {

            if(sp.getSeriesCount()>1) continue;
            sp.getRenderer().setSeriesPaint(0, colors.get(index));
            sp.getRenderer().setSeriesStroke(0, new BasicStroke( 1.5f ));

            index = (index +1 ) % colors.size();
        }

        JFreeChart chart = new JFreeChart("HML Data Chart",JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.setPreferredSize(new java.awt.Dimension(1024, 768));
        setContentPane(chartPanel);


    }

    private void subplot(String des, XYDataset data, CombinedDomainXYPlot plot ){
        //StandardXYItemRenderer renderer = new StandardXYItemRenderer();
        DeviationRenderer deviationrenderer = new DeviationRenderer(true, false);
        deviationrenderer.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
        //deviationrenderer.setSeriesStroke(1, new BasicStroke(3F, 1, 1));
        //deviationrenderer.setSeriesStroke(2, new BasicStroke(3F, 1, 1));
        deviationrenderer.setSeriesFillPaint(0, new Color(200, 100, 255));
        //deviationrenderer.setSeriesFillPaint(1, new Color(200, 200, 255));


        NumberAxis rangeAxis = new NumberAxis(des);
        XYPlot subplot = new XYPlot(data, null, rangeAxis, deviationrenderer);
        subplot.setInsets(new RectangleInsets(5D, 5D, 5D, 20D));
        subplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);


        //subplot.setWeight(10);


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
        YIntervalSeries series = null;
        double old_value = 0;
        while (true) {
            String key = name + "_" + depth + "_0";
            VariableWithData variableWithData = dataHashMap.get(key);

            if (variableWithData == null) {
                if (depth>maxDepth) break;
                else {
                    VariableWithData clock = dataHashMap.get("clock"+ "_" + depth + "_0");
                    /*
                    List<XYDataItem> items = (List<XYDataItem>) clock.values.getItems();

                    for (XYDataItem item : items) {
                        series.add(new XYDataItem(item.getX().doubleValue() + globalTime.get(depth - 1), old_value));
                    }
                    */
                    int itemCount = clock.values.getItemCount();
                    for (int i=0; i<itemCount; i++) {
                        series.add(clock.values.getX(i).doubleValue() + globalTime.get(depth - 1),
                                old_value, old_value, old_value);
                    }

                }
            } else {
                if (depth == 0) {
                    series = variableWithData.values;
                    dataSet.addSeries(series);
                    seriesList.add(series);
                } else {
                    /*
                    List<XYDataItem> items = (List<XYDataItem>) variableWithData.values.getItems();
                    for (XYDataItem item : items) {
                            series.add(new XYDataItem(item.getX().doubleValue() + globalTime.get(depth - 1), item.getY()));

                    }
                    */
                    int itemCount = variableWithData.values.getItemCount();
                    for (int i=0; i<itemCount; i++) {
                        series.add(variableWithData.values.getX(i).doubleValue() + globalTime.get(depth - 1),
                                variableWithData.values.getYValue(i),
                                variableWithData.values.getYLowValue(i),
                                variableWithData.values.getYHighValue(i));
                    }

                }
            }
            int size = series.getItemCount();
            old_value = series.getYValue(size-1);
            depth++;

        }
    }

    private void golobalTimeVar(YIntervalSeriesCollection dataSet, HashMap<String, VariableWithData> dataHashMap){
        int depth = 0;
        //XYSeries series = null;
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
                dataSet.addSeries(series);
                seriesList.add(series);
            } else {
                int itemCount = variableWithData.values.getItemCount();
                for (int i=0; i<itemCount; i++) {
                    series.add(variableWithData.values.getX(i).doubleValue(),
                            variableWithData.values.getYValue(i),
                            variableWithData.values.getYLowValue(i),
                            variableWithData.values.getYHighValue(i));
                }
                /*
                List<XYDataItem> items = (List<XYDataItem>) variableWithData.values.getItems();
                for (XYDataItem item : items) {
                    series.add(item);
                }
                */
            }
            globalTime.add(series.getYValue(series.getItemCount()-1));
            depth++;
        }
    }


    public static void showData(String modelPath) throws Exception {
        InputStream inputStream = new FileInputStream(modelPath);
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

    public static void main(String[] args) throws Exception{

        ParseJSONData.showData("./JSON/src/data.json");
    }

}
