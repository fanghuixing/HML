package DataSet;



import org.jfree.data.xy.XYSeries;



/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-27.
 */
public class VariableWithData {
    String name;
    int mode;
    int depth;
    XYSeries values;

    public VariableWithData(String name, int mode , int depth,  XYSeries values) {
        this.name = name;
        this.values = values;
        this.depth = depth;
        this.mode = mode;
    }
}
