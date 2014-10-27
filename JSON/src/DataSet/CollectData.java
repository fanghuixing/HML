package DataSet;

import AntlrGen.JSONBaseVisitor;
import AntlrGen.JSONParser;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-27.
 */
public class CollectData extends JSONBaseVisitor{

    HashSet<String> variables = new HashSet<String>();
    List<JSONParser.FlowContext> flows;

    HashMap<String, VariableWithData> dataHashMap = new HashMap<String, VariableWithData>();

    public CollectData() {

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
        String[] key = ctx.key().ID().getText().split("_");
        String name = key[0];
        variables.add(name);
        int depth = Integer.parseInt(key[1]);
        int mode = Integer.parseInt(ctx.mod().INT().getText());
        XYSeries values = new XYSeries(name);
        List<JSONParser.MappingContext> mappings = ctx.values().mapping();
        for (JSONParser.MappingContext mapping : mappings) {
            List<JSONParser.NumberContext> time = mapping.time().interval().number();
            List<JSONParser.NumberContext> value = mapping.value().interval().number();
            for (int i =0 ; i< time.size();  i++) {
                if (name.equals("global")) {
                    Double v = Double.parseDouble(value.get(i).getText());
                    values.add(v, v);
                }
                else {
                    Double t = Double.parseDouble(time.get(i).getText());
                    Double v = Double.parseDouble(value.get(i).getText());
                    values.add(t, v);
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
