package IMP.Translate;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import AntlrGen.HMLParser;
import IMP.HML2SMT;
import IMP.Scope.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;


/**
 * Created by fofo on 2014/9/30.
 */
public class Dynamics {
    private int depth;
    private List<ParserRuleContext> discrete = new ArrayList<ParserRuleContext>();
    private List<ContextWithVarLink> continuous = new ArrayList<ContextWithVarLink>();
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

        sb.append("Discrete: ");
        renderDiscreteFormulas(ID2ExpMap);

        sb.append(" Continuous: ");
        renderContinuousFormulas(ID2ExpMap);

        //show all the formulas for discrete actions
        for (Map.Entry abe : ID2ExpMap.entrySet()) {
            sb.append(String.format("(= %s %s)", abe.getKey(), abe.getValue()));
        }

        return sb.toString();
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

    private void renderDiscreteFormulas(HashMap<String, AbstractExpr>  ID2ExpMap){
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();

        for (ParserRuleContext r : discrete) {
            if (r instanceof HMLParser.AssignmentContext) {
                HMLParser.AssignmentContext ass = (HMLParser.AssignmentContext) r;
                String ID = ass.ID().getText();
                HMLParser.ExprContext eprc = ass.expr();
                refeshAndSave(ID, eprc, ID2ExpMap, exprs);
            }
            if (r instanceof HMLParser.EqWithInitContext) {
                HMLParser.EqWithInitContext eqwi = (HMLParser.EqWithInitContext) r;
                String ID = eqwi.relation().ID().getText();
                HMLParser.ExprContext eprc = eqwi.expr();
                refeshAndSave(ID, eprc, ID2ExpMap, exprs);
            }
        }
    }

    private void refeshAndSave(String ID, HMLParser.ExprContext eprc, HashMap<String, AbstractExpr>  ID2ExpMap, ParseTreeProperty<AbstractExpr> exprs ) {
        AbstractExpr abstractExpr = exprs.get(eprc);
        refreshExpression(abstractExpr, ID2ExpMap);
        ID2ExpMap.put(ID, abstractExpr);
        System.out.println("Put in ID2ExpMap : " + ID + "->" + abstractExpr);
    }

    private void renderContinuousFormulas(HashMap<String, AbstractExpr>  ID2ExpMap){
        for (ContextWithVarLink r : continuous) {
            if (r.getPrc() instanceof  HMLParser.OdeContext) {
                HMLParser.EquationContext equ = ((HMLParser.OdeContext) r.getPrc()).equation();
                if (equ instanceof HMLParser.EqWithNoInitContext) {
                    HMLParser.RelationContext relation  = ((HMLParser.EqWithNoInitContext) equ).relation();
                    resolveRelation(relation, r.getVrl());
                }
            }

        }
    }

    private void resolveRelation(HMLParser.RelationContext relation, VariableLink variableLink) {
        String varName = relation.ID().getText();
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr expr = exprs.get(relation.expr());
        AbstractExpr flow = new AbstractExpr("=", new AbstractExpr(String.format("d/dt[%s]", varName)), expr);
        System.out.println(flow);

        System.out.println("The real var name is : " + variableLink.getRealVar(varName));


    }


}
