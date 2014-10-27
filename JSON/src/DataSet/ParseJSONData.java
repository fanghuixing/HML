package DataSet;

import AntlrGen.JSONLexer;
import AntlrGen.JSONParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import java.awt.Color;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
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

    public ParseJSONData(String title, List<String> variables, HashMap<String, VariableWithData> dataHashMap) {
        super(title);
        XYDataset dataSet = createDataSet(variables, dataHashMap);
        JFreeChart chart = createChart(dataSet);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }


    public XYDataset createDataSet(List<String> variables, HashMap<String, VariableWithData> dataHashMap) {
        XYSeriesCollection dataSet = new XYSeriesCollection();
        golobalTimeVar(dataSet, dataHashMap);
        variables.remove("global");
        for (String name : variables)
            createDataSetForOneVariable(name, dataSet, dataHashMap);

        return dataSet;
    }

    private void createDataSetForOneVariable(String name, XYSeriesCollection dataSet, HashMap<String, VariableWithData> dataHashMap){
        int depth = 0;
        XYSeries series = null;
        double old_value = 0;
        while (true) {
            String key = name + "_" + depth + "_0";
            VariableWithData variableWithData = dataHashMap.get(key);

            if (variableWithData == null) {
                if (depth>maxDepth) break;
                else {
                    VariableWithData clock = dataHashMap.get("clock"+ "_" + depth + "_0");
                    List<XYDataItem> items = (List<XYDataItem>) clock.values.getItems();
                    for (XYDataItem item : items) {
                        series.add(new XYDataItem(item.getX().doubleValue() + globalTime.get(depth - 1), old_value));
                    }

                }
            } else {
                if (depth == 0) {
                    series = variableWithData.values;
                    dataSet.addSeries(series);
                } else {
                    List<XYDataItem> items = (List<XYDataItem>) variableWithData.values.getItems();
                    for (XYDataItem item : items) {
                        series.add(new XYDataItem(item.getX().doubleValue() + globalTime.get(depth - 1), item.getY()));
                    }
                }
            }
            int size = series.getItemCount();
            old_value = series.getY(size-1).doubleValue();
            depth++;

        }
    }

    private void golobalTimeVar(XYSeriesCollection dataSet, HashMap<String, VariableWithData> dataHashMap){
        int depth = 0;
        XYSeries series = null;
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
            } else {
                List<XYDataItem> items = (List<XYDataItem>) variableWithData.values.getItems();
                for (XYDataItem item : items) {
                    series.add(item);
                }
            }
            globalTime.add(series.getMaxY());
            depth++;
        }
    }

    private static JFreeChart createChart(XYDataset dataset) {
        // create the chart...
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Line Chart Demo 2",
                // chart title
                "X",
                // x axis label
                "Y",
                // y axis label
                dataset,
                // data
                PlotOrientation.VERTICAL,
                true,
                // include legend
                true,
                // tooltips
                false
                // urls
        );
        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);


    // get a reference to the plot for further customisation...
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(Color.lightGray);
    plot.setAxisOffset(new RectangleInsets(5.0,5.0,5.0,5.0));
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    XYLineAndShapeRenderer renderer
            = (XYLineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseShapesFilled(true);
    renderer.setBaseLinesVisible(true);
    // change the auto tick unit selection to integer units only...
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    // OPTIONAL CUSTOMISATION COMPLETED.
    return chart;
}

    public static void ShowData(String modelPath) throws Exception {
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
}
