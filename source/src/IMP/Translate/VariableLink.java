package IMP.Translate;

import AntlrGen.HMLParser.IDExprContext;
import IMP.Basic.Variable;

import java.net.IDN;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fofo on 2014/10/1.
 */
public class VariableLink {
    HashMap<String, String> Virtual2RealVar = new HashMap<String, String>();
    private VariableLink enclosingLink;
    public VariableLink(VariableLink enclosingLink) {
        this.enclosingLink = enclosingLink;
    }




    public String getRealVar(String virtualVar) {
        String real = Virtual2RealVar.get(virtualVar);
        if (real==null) return virtualVar;
        else {
            if (enclosingLink != null)
                return enclosingLink.getRealVar(real);
            else return real;
        }
    }













    public void setRealVar(String virtualVar, String realVar) {
        Virtual2RealVar.put(virtualVar, realVar);
    }

    public void clear(){
        Virtual2RealVar.clear();
    }

    public void printAll() {
        for (Map.Entry e : Virtual2RealVar.entrySet()) {
            System.out.println(String.format("%s : %s ", e.getKey(), e.getValue()));
        }
        if (enclosingLink!=null){
            System.out.println("The Enclosing Variable Link ....");
            enclosingLink.printAll();
        }

    }

}
