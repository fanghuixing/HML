package DataSet;

import org.jfree.chart.*;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;

import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-28.
 */
public class HMLChartMouseListener implements ChartMouseListener {
    public static Rectangle2D dataArea;
    public static double yy;
    public static ValueAxis yAxis;
    private Crosshair xCrosshair;
    private Crosshair yCrosshair;
    private ChartPanel chartPanel;
    public HMLChartMouseListener(Crosshair xCrosshair, Crosshair yCrosshair, ChartPanel chartPanel) {
        this.xCrosshair = xCrosshair;
        this.yCrosshair = yCrosshair;
        this.chartPanel = chartPanel;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {

    }

    @Override
    public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {
        int xc = chartMouseEvent.getTrigger().getX();
        int yc = chartMouseEvent.getTrigger().getY();
        dataArea = chartPanel.getScreenDataArea(xc, yc);

        JFreeChart chart = chartMouseEvent.getChart();
        CombinedDomainXYPlot plot = (CombinedDomainXYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();

        Point2D point = chartPanel.translateScreenToJava2D(new Point(xc, yc));
        XYPlot sub =  plot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), point);
        try {
            double x = xAxis.java2DToValue(xc, dataArea, RectangleEdge.BOTTOM);
            xCrosshair.setValue(x);
            yAxis = sub.getRangeAxis();
            double y = yAxis.java2DToValue(yc, dataArea, RectangleEdge.LEFT);
            yCrosshair.setValue(y);
         }catch (NullPointerException e){
            yCrosshair.setValue(0);
        }





    }
}
