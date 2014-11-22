package DataSet;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-21.
 */
public class DataCreator {
    List<YIntervalSeriesCollection> seriesCollectionList = new ArrayList<YIntervalSeriesCollection>();
    int maxDepth;
    List<Double> globalTime = new ArrayList<Double>();
    List<Double> initGlobalTime = new ArrayList<Double>();

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
                    //logger.debug("Get Old Data for Variable " + name);
                    VariableWithData clock = dataHashMap.get("clock"+ "_" + depth + "_0");
                    int itemCount = clock.values.getItemCount();
                    if (depth==0)
                        series = new YIntervalSeries(name, false, true);
                    else
                        series = new YIntervalSeries(name+depth, false, true);
                    dataSet.addSeries(series);
                    seriesCollection.addSeries(series);
                    if (depth==0) {
                        for (int i = 1; i <= depth; i++) {
                            String k = name + "_" + i + "_0";
                            VariableWithData data = dataHashMap.get(k);
                            if (variableWithData != null) {
                                old_value = data.values.getYValue(0);
                                break;
                            }
                        }
                        for (int i = 0; i < itemCount; i++)
                            series.add(clock.values.getX(i).doubleValue(), old_value, old_value, old_value);

                    } else {
                        for (int i = 0; i < itemCount; i++)
                            series.add(clock.values.getX(i).doubleValue() + initGlobalTime.get(depth), old_value, old_value, old_value);
                            //series.add(clock.values.getX(i).doubleValue() + globalTime.get(depth - 1), old_value, old_value, old_value);

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
                        addSeries(series, variableWithData, i, initGlobalTime.get(depth));
                        //addSeries(series, variableWithData, i, globalTime.get(depth - 1));
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
            initGlobalTime.add(series.getYHighValue(0));
            depth++;
        }
    }

}
