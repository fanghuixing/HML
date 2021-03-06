package DataSet;

import AntlrGen.JSONBaseVisitor;
import AntlrGen.JSONParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.general.SeriesException;
import org.jfree.data.xy.YIntervalSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-27.
 */
public class CollectData extends JSONBaseVisitor{

    private static Logger logger = LogManager.getLogger(CollectData.class);
    static HashSet<String> variables = new HashSet<String>();
    static List<JSONParser.FlowContext> flows;

    static HashMap<String, VariableWithData> dataHashMap = new HashMap<String, VariableWithData>();

    public CollectData() {

    }


    public static void setVariables(HashSet<String> variables) {
        CollectData.variables = variables;
    }

    public static void setDataHashMap(HashMap<String, VariableWithData> dataHashMap) {
        CollectData.dataHashMap = dataHashMap;
    }

    public Void visitJsonData(JSONParser.JsonDataContext ctx) {
        flows = ctx.flow();
        for (JSONParser.FlowContext flow : flows) {
            visit(flow);
        }
        return null;
    }

    public Void visitFlow(JSONParser.FlowContext ctx) {
        List<JSONParser.DataContext> dataList = ctx.data();
        for (JSONParser.DataContext data : dataList) {
            visit(data);
        }
        return null;
    }

    public Void visitData(JSONParser.DataContext ctx) {
        //logger.debug("visit data");
        String[] key = ctx.key().ID().getText().split("_");
        String name = key[0];
        variables.add(name);
        int depth = Integer.parseInt(key[1]);
        int mode = Integer.parseInt(ctx.mod().INT().getText());
        YIntervalSeries  values = new YIntervalSeries(name, false, true);
        List<JSONParser.MappingContext> mappings = ctx.values().mapping();



        for (JSONParser.MappingContext mapping : mappings) {

            List<JSONParser.NumberContext> time = mapping.time().interval().number();
            List<JSONParser.NumberContext> value = mapping.value().interval().number();

            if (name.equals("global") || name.equals("clock")) {
                Double t0 = Double.parseDouble(value.get(0).getText());
                Double t1 = Double.parseDouble(value.get(1).getText());
                values.add(t0, t0, t0, t0);
                if (Math.abs(t0-t1)>0.001)
                    values.add(t1, t1, t1, t1);
            } else {
                double t0 = Double.parseDouble(time.get(0).getText());
                double t1 = Double.parseDouble(time.get(1).getText());

                double v0 = Double.parseDouble(value.get(0).getText());
                double v1 = Double.parseDouble(value.get(1).getText());

                //Skip the dirty or too coarse data
                if (Math.abs(t0-t1)>2) {
                    continue;
                }

                //If the data is clean
                try {
                    if (v0 <= v1) {
                        values.add((t0 + t1) / 2, (v0 + v1) / 2, v0, v1);
                    } else {
                        values.add((t0 + t1) / 2, (v0 + v1) / 2, v1, v0);
                    }
                } catch (SeriesException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        dataHashMap.put(ctx.key().ID().getText(), new VariableWithData(name, mode, depth, values));
        return null;
    }

    public List<String> getVariables() {
        List<String> res = new ArrayList<String>();
        res.addAll(variables);
        return res;
    }

    public HashMap<String, VariableWithData> getDataHashMap() {
        return dataHashMap;
    }
}
