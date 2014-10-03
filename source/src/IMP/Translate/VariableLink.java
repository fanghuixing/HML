package IMP.Translate;

import AntlrGen.HMLParser.IDExprContext;

import java.net.IDN;
import java.util.HashMap;

/**
 * Created by fofo on 2014/10/1.
 */
public class VariableLink {
    HashMap<String, String> Virtual2RealVar = new HashMap<String, String>();


    public String getRealVar(String virtualVar) {
        String real = Virtual2RealVar.get(virtualVar);
        if (real==null) return virtualVar;
        return real;
    }

    public void setRealVar(String virtualVar, String realVar) {
        Virtual2RealVar.put(virtualVar, realVar);
    }

    public void clear(){
        Virtual2RealVar.clear();
    }
}