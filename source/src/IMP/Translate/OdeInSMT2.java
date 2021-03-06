package IMP.Translate;

import java.util.List;

/**
 * 将ode转成smt2公式定义
 */
public class OdeInSMT2 {
    int odeIndex;
    List<String> vars;
    int depth;

    public OdeInSMT2(List<String> vars, int depth, int odeIndex) {
        this.odeIndex = odeIndex;
        this.vars = vars;
        this.depth = depth;
    }

    @Override
    public String toString() {
        StringBuilder formattedVars = getConVars();
        int separator = formattedVars.indexOf("@");
        String stableVars = formattedVars.substring(0, separator);
        String initVars = formattedVars.substring(separator+1);
        return String.format("(= %s (integral 0. time_%s %s flow_%s)) ", stableVars, depth, initVars, odeIndex);

    }

    //转成SMT2格式
    private StringBuilder getConVars() {
        StringBuilder conStable = new StringBuilder();
        StringBuilder conInit = new StringBuilder();
        for (String v : vars) {
            conStable.append(String.format("%s_%s_t ", v, depth));
            conInit.append(String.format("%s_%s_0 ", v, depth));
        }

        addClockVar(conStable, conInit);
        addGlobalClockVar(conStable, conInit);

        conStable.insert(0, "[").replace(conStable.length()-1,conStable.length()-1,"]");
        conInit.insert(0, "[").replace(conInit.length()-1, conInit.length()-1,"]");
        return  conStable.append("@").append(conInit);
    }

    //添加时钟变量, 每个连续行为从0开始计时
    private void addClockVar(StringBuilder conStable, StringBuilder conInit){
        conStable.append(String.format("%s_%s_t ", "clock", depth));
        conInit.append(String.format("%s_%s_0 ", "clock", depth));
    }

    //添加全局时钟变量，从系统开始运行的时候计时，不重置
    private void addGlobalClockVar(StringBuilder conStable, StringBuilder conInit){
        conStable.append(String.format("%s_%s_t ", "global", depth));
        conInit.append(String.format("%s_%s_0 ", "global", depth));
    }
}
