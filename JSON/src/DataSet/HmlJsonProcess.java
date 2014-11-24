package DataSet;
import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.general.SeriesException;
import org.jfree.data.xy.YIntervalSeries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-21.
 */
public class HmlJsonProcess {
    private static Logger logger = LogManager.getLogger(HmlJsonProcess.class);
    static HashSet<String> variables = new HashSet<String>();
    static HashMap<String, VariableWithData> dataHashMap = new HashMap<String, VariableWithData>();
    public static void init(CollectData collectData, BufferedReader inputStream){


        try {
            StringBuilder sb=new StringBuilder();
            Stack<String> stack = new Stack<String>();
            while (true) {
                String line = inputStream.readLine();
                if (line==null) break;
                else {
                    if (line.equals("[") ){
                        stack.push(" ");
                    } else if (line.equals("]")) {
                        stack.pop();
                    }

                    if (stack.size()>=2){
                        sb.append(line);
                    } else if (stack.size()==1 && sb.length()>1){
                        sb.append(']');
                        logger.debug(sb.length());
                        List<FlowOfVariable> flowOfVariables = JSON.parseArray(sb.toString(), FlowOfVariable.class);
                        processFlow(flowOfVariables);
                        sb = new StringBuilder();
                    }
                }

            }

        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        collectData.setVariables(variables);
        collectData.setDataHashMap(dataHashMap);
    }

    public static void main(String[] args){
        //init();
    }

    public static void processFlow(List<FlowOfVariable> flowOfVariables){
        for (FlowOfVariable flow : flowOfVariables) {
            visitData(flow);
        }
        logger.debug("End One Flow");
    }

    public static void visitData(FlowOfVariable flow) {
        //logger.debug("visit data");
        String[] key = flow.getKey().split("_");
        String name = key[0];
        variables.add(name);
        int depth = Integer.parseInt(key[1]);
        int mode = Integer.parseInt(flow.getMode());
        YIntervalSeries values = new YIntervalSeries(name, false, true);
        List<TimeValuePair> mappings = flow.getValues();;
        for (TimeValuePair mapping : mappings) {
            List<Double> time = mapping.time;
            List<Double> value = mapping.enclosure;

            if (name.equals("global") || name.equals("clock")) {
                Double t0 = value.get(0);
                Double t1 = value.get(1);

                values.add(t0, t0, t0, t0);
                if (Math.abs(t1 - t0) > 0.001)
                    values.add(t1, t1, t1, t1);

            } else {
                double t0 = time.get(0);
                double t1 = time.get(1);

                double v0 = value.get(0);
                double v1 = value.get(1);

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
        dataHashMap.put(flow.getKey(), new VariableWithData(name, mode, depth, values));

    }


    static class TimeValuePair{
        private List<Double> time = new ArrayList<Double>();
        private List<Double> enclosure = new ArrayList<Double>();

        public List<Double> getTime() {
            return time;
        }

        public void setTime(List<Double> time) {
            this.time = time;
        }

        public List<Double> getEnclosure() {
            return enclosure;
        }

        public void setEnclosure(List<Double> enclosure) {
            this.enclosure = enclosure;
        }
    }

    static class FlowOfVariable {
        private String key;
        private String mode;
        private String step;
        private List<TimeValuePair> values = new ArrayList<TimeValuePair>();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getStep() {
            return step;
        }

        public void setStep(String step) {
            this.step = step;
        }

        public List<TimeValuePair> getValues() {
            return values;
        }

        public void setValues(List<TimeValuePair> values) {
            this.values = values;
        }
    }



}
