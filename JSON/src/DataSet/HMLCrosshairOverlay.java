package DataSet;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-29.
 */
public class HMLCrosshairOverlay extends CrosshairOverlay {

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        Shape savedClip = g2.getClip();
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        //Rectangle2D dataArea = HMLChartMouseListener.dataArea;
        g2.clip(dataArea);
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
        Iterator iterator = super.getDomainCrosshairs().iterator();
        while (iterator.hasNext()) {
            Crosshair ch = (Crosshair) iterator.next();
            if (ch.isVisible()) {
                double x = ch.getValue();
                double xx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    drawVerticalCrosshair(g2, dataArea, xx, ch);
                }
                else {
                    drawHorizontalCrosshair(g2, dataArea, xx, ch);
                }
            }
        }
        ValueAxis yAxis = plot.getRangeAxis();
        RectangleEdge yAxisEdge = plot.getRangeAxisEdge();
        iterator = super.getRangeCrosshairs().iterator();
        while (iterator.hasNext()) {
            Crosshair ch = (Crosshair) iterator.next();
            if (ch.isVisible()) {
                double y = ch.getValue();
                double yy=0;
                try {
                    yy = HMLChartMouseListener.yAxis.valueToJava2D(y, HMLChartMouseListener.dataArea, RectangleEdge.LEFT);

                    if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                        drawHorizontalCrosshair(g2, HMLChartMouseListener.dataArea, yy, ch);
                    } else {
                        drawVerticalCrosshair(g2, HMLChartMouseListener.dataArea, yy, ch);
                    }
                }catch (NullPointerException e) {}

            }
        }
        g2.setClip(savedClip);
    }


}
