package IMP.Translate;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import AntlrGen.HMLParser;
import IMP.HML2SMT;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;


/**
 * Created by fofo on 2014/9/30.
 */
public class Dynamics {
    private static int odeIndex = 1;
    private static HashMap<Integer,String> odeMap = new HashMap<Integer, String>();
    private int depth;
    private List<ParserRuleContext> discrete = new ArrayList<ParserRuleContext>();
    private List<ContextWithVarLink> continuous = new ArrayList<ContextWithVarLink>();
    private String resultFormula;
    public void setDepth(int depth) {
        this.depth = depth;
    }



    public void addDiscrete(ParserRuleContext bsc){
        discrete.add(bsc);
    }

    public void addContinuous(ContextWithVarLink bsc) {
        continuous.add(bsc);
    }

    @Override
    public String toString() {
        if (resultFormula!=null) return resultFormula;
        StringBuilder sb = new StringBuilder();
        HashMap<String, AbstractExpr>  ID2ExpMap;
        if (depth == 0) {
            System.out.println("Use the variables initializations information");
            ID2ExpMap = HML2SMT.getInitID2ExpMap();
            System.out.println("Size : " + ID2ExpMap.size());
            for (Map.Entry idem : ID2ExpMap.entrySet()) {
                System.out.println("The mapping in Initializations:" +idem.getKey());
            }
        }
        else  ID2ExpMap = new HashMap<String, AbstractExpr>();
        renderDisFormulas(ID2ExpMap);
        //show all the formulas for discrete actions
        for (Map.Entry<String, AbstractExpr> abe : ID2ExpMap.entrySet()) {
            sb.append(String.format("(= %s %s)", addDepthFlagToVar(abe.getKey()), abe.getValue().toString(depth)));
        }
        sb.append("\n");
        sb.append(renderConFormulas());
        resultFormula = sb.toString();
        return resultFormula;
    }

    public String addDepthFlagToVar(String v) {
        return String.format("%s_%s_0", v, depth);
    }




    private void refreshExpression(AbstractExpr abstractExpr, HashMap<String, AbstractExpr> ID2ExpMap) {
        List<String> IDList = abstractExpr.getIDList();
        for (String ID : IDList) {
            AbstractExpr abstractExprforID = ID2ExpMap.get(ID);
            if (abstractExprforID != null) {
                System.out.println(String.format("Relacing %s ... ... ...", ID));
                abstractExpr.replace(ID, abstractExprforID);
            }
        }
    }

    private void renderDisFormulas(HashMap<String, AbstractExpr> ID2ExpMap){
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        for (ParserRuleContext r : discrete) {
            if (r instanceof HMLParser.AssignmentContext) {
                HMLParser.AssignmentContext ass = (HMLParser.AssignmentContext) r;
                String ID = ass.ID().getText();
                HMLParser.ExprContext eprc = ass.expr();
                refeshAndSave(ID, eprc, ID2ExpMap);
            }
            if (r instanceof HMLParser.EqWithInitContext) {
                HMLParser.EqWithInitContext eqwi = (HMLParser.EqWithInitContext) r;
                String ID = eqwi.relation().ID().getText();
                HMLParser.ExprContext eprc = eqwi.expr();
                refeshAndSave(ID, eprc, ID2ExpMap);
            }
        }
    }

    private void refeshAndSave(String ID, HMLParser.ExprContext eprc, HashMap<String, AbstractExpr>  ID2ExpMap) {
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr abstractExpr = exprs.get(eprc);
        refreshExpression(abstractExpr, ID2ExpMap);
        ID2ExpMap.put(ID, abstractExpr);
    }

    private String renderConFormulas(){
        StringBuilder flows = new StringBuilder();
        for (ContextWithVarLink r : continuous) {
            if (r.getPrc() instanceof  HMLParser.OdeContext) {
                HMLParser.EquationContext equ = ((HMLParser.OdeContext) r.getPrc()).equation();
                if (equ instanceof HMLParser.EqWithNoInitContext) {
                    HMLParser.RelationContext relation  = ((HMLParser.EqWithNoInitContext) equ).relation();
                    AbstractExpr flow = resolveRelation(relation, r.getVrl());
                    String result = flow.toString(r.getVrl());
                    odeMap.put(odeIndex, String.format("(define-ode flow_%s (%s))", odeIndex, result));
                    List<String> vars = flow.getVarsList(r.getVrl());
                    OdeInSMT2 odeInSMT2 = new OdeInSMT2(vars, depth, odeIndex);
                    flows.append(odeInSMT2.toString());
                    odeIndex++;
                }
            }
        }
        return flows.toString();
    }


    private AbstractExpr resolveRelation(HMLParser.RelationContext relation, VariableLink variableLink) {
        String varName = relation.ID().getText();
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr left = new AbstractExpr("d/dt", new AbstractExpr(varName, AbstractExpr.Sort.VAR),null);
        AbstractExpr right = exprs.get(relation.expr());
        return new AbstractExpr("=", left, right);
    }

    public static HashMap<Integer, String> getOdeMap() {
        return odeMap;
    }


}
