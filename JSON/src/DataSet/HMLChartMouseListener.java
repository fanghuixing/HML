package DataSet;

import org.jfree.chart.*;
import org.jfree.chart.axis.ValueAxis;
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
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        JFreeChart chart = chartMouseEvent.getChart();
        CombinedDomainXYPlot plot = (CombinedDomainXYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();

        double x = xAxis.java2DToValue(chartMouseEvent.getTrigger().getX(), dataArea,
                RectangleEdge.BOTTOM);
        xCrosshair.setValue(x);

        int xPos = chartMouseEvent.getTrigger().getX();
        int yPos = chartMouseEvent.getTrigger().getY();

        System.out.println("x = " + xPos + ", y = " + yPos);

        Point2D point2D = chartPanel.translateScreenToJava2D(new Point(xPos, yPos));
        ChartRenderingInfo chartRenderingInfo = chartPanel.getChartRenderingInfo();
        Rectangle2D rectangle2D = chartRenderingInfo.getPlotInfo().getDataArea();
        XYPlot sub = (XYPlot) plot.getSubplots().get(0);

        ValueAxis yAxis = sub.getRangeAxis();

        RectangleEdge rectangleEdge1 = sub.getDomainAxisEdge();
        RectangleEdge rectangleEdge2 = sub.getRangeAxisEdge();
        double d1 = xAxis.java2DToValue(point2D.getX(), rectangle2D, rectangleEdge1);
        double d2 = yAxis.java2DToValue(point2D.getY(), rectangle2D, rectangleEdge2);

        System.out.println(d1 + " " + d2);
    }
}
