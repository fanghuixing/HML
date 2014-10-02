package IMP.Translate;

import java.util.List;

/**
 * Created by fofo on 2014/10/2.
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
        conStable.insert(0, "[").replace(conStable.length()-1,conStable.length()-1,"]");
        conInit.insert(0, "[").replace(conInit.length()-1, conInit.length()-1,"]");
        return  conStable.append("@").append(conInit);
    }
}
