package IMP.Translate;



import java.util.*;

import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.HML2SMT;
import com.sun.javafx.scene.control.skin.EmbeddedTextContextMenuContent;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;


/**
 * Created by fofo on 2014/9/30.
 */
public class Dynamics {
    private static int odeIndex = 1;
    private static final String guardPrefix = "@guard:";
    private static HashMap<Integer,String> odeMap = new HashMap<Integer, String>();
    private int depth;
    private List<ContextWithVarLink> discrete = new ArrayList<ContextWithVarLink>();
    private ContextWithVarLink continuous;
    private String resultFormula;
    HashMap<String, AbstractExpr>  ID2ExpMap;
    HashMap<AbstractExpr, String>  TempOdesMap = new HashMap<AbstractExpr, String>();
    private int guardIndex =0;


    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void addDiscrete(ContextWithVarLink bsc){
        discrete.add(bsc);
    }

    public void addContinuous(ContextWithVarLink bsc) {
        continuous = bsc;
    }

    public String addDepthFlagToVar(String v) {
        return String.format("%s_%s_0", v, depth);
    }

    private void renderDisFormulas(){
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        for (ContextWithVarLink c : discrete) {
            ParserRuleContext r = c.getPrc();
            if (r instanceof HMLParser.AssignmentContext) {
                HMLParser.AssignmentContext ass = (HMLParser.AssignmentContext) r;
                String ID = ass.ID().getText();
                HMLParser.ExprContext eprc = ass.expr();
                refeshAndSave(ID, eprc, c.getVrl());
            }
            if (r instanceof HMLParser.EqWithInitContext) {
                HMLParser.EqWithInitContext eqwi = (HMLParser.EqWithInitContext) r;
                String ID = eqwi.relation().ID().getText();
                HMLParser.ExprContext eprc = eqwi.expr();
                refeshAndSave(ID, eprc, c.getVrl());
            }
            if (r instanceof  HMLParser.GuardContext) {
                //System.out.println("--------------analysis guard-----------");
                analyzeGuard((HMLParser.GuardContext) r, c.getVrl());
            }
        }
    }

    private void refeshAndSave(String ID, HMLParser.ExprContext eprc, VariableLink variableLink) {
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr abstractExpr = exprs.get(eprc);
        if (variableLink!=null) {
            abstractExpr.resolve(variableLink);
            ID = variableLink.getRealVar(ID);
        }
        refreshExpression(abstractExpr);
        ID2ExpMap.put(ID, abstractExpr);
    }



    private void refreshExpression(AbstractExpr abstractExpr) {
        List<String> IDList = abstractExpr.getIDList();
        for (String ID : IDList) {
            AbstractExpr abstractExprforID = ID2ExpMap.get(ID);
            if (abstractExprforID != null) {
                System.out.println(String.format("Relacing %s ... ... ...", ID));
                abstractExpr.replace(ID, abstractExprforID);
            }
        }
    }
    private String renderConFormulas(){
        StringBuilder flows = new StringBuilder();
        ContextWithVarLink r = continuous;

        if (r.getPrc() instanceof  HMLParser.OdeContext) {
                //如果是方程
                HMLParser.EquationContext equ = ((HMLParser.OdeContext) r.getPrc()).equation();
                analyzeEquaiton(equ, flows, r);
        }
        StringBuilder result = new StringBuilder();
        List<String> vars = new ArrayList<String>();
        for (Map.Entry<AbstractExpr, String>  e : TempOdesMap.entrySet()) {
            result.append(e.getValue());
            addList(vars, e.getKey().getVarsList(r.getVrl())); // 获取方程中涉及的变量
        }

        odeMap.put(odeIndex, String.format("(define-ode flow_%s (%s))", odeIndex, result)); //存储方程定义
        removeDuplicate(vars);
        OdeInSMT2 odeInSMT2 = new OdeInSMT2(vars, depth, odeIndex); //准备SMT2格式的连续行为表示
        flows.append(odeInSMT2.toString());
        odeIndex++;
        return flows.toString();
    }

    private void addList(List<String> target, List<String> from) {
        for (String s : from) {
            target.add(s);
        }
    }

    private void removeDuplicate(List<String> list) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        for (String s : list) {
            m.put(s, null);
        }
        list.clear();
        list.addAll(m.keySet());
    }

    private void analyzeEquaiton(HMLParser.EquationContext equ, StringBuilder flows, ContextWithVarLink r){
        if (equ instanceof HMLParser.EqWithNoInitContext) {
            //如果方程不带初值
            HMLParser.RelationContext relation  = ((HMLParser.EqWithNoInitContext) equ).relation();
            analyzeRelation(relation, flows, r);
        }
        else if (equ instanceof HMLParser.EqWithInitContext) {
            //如果方程不带初值
            HMLParser.RelationContext relation  = ((HMLParser.EqWithInitContext) equ).relation();
            analyzeRelation(relation, flows, r);
        }
        else if (equ instanceof HMLParser.ParaEqContext) {
            //如果是方程组
            for (HMLParser.EquationContext e : ((HMLParser.ParaEqContext) equ).equation()) {
                analyzeEquaiton(e, flows, r);
            }
        }
    }

    private void analyzeRelation(HMLParser.RelationContext relation, StringBuilder flows, ContextWithVarLink r){
        AbstractExpr flow = resolveRelation(relation); //得到方程的抽象表示
        String result = flow.toString(r.getVrl());     //还原真实变量

        TempOdesMap.put(flow, result);

    }

    private AbstractExpr resolveRelation(HMLParser.RelationContext relation) {
        String varName = relation.ID().getText();
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr left = new AbstractExpr("d/dt", new AbstractExpr(varName, AbstractExpr.Sort.VAR),null);
        AbstractExpr right = exprs.get(relation.expr());
        return new AbstractExpr("=", left, right);
    }

    public static HashMap<Integer, String> getOdeMap() {
        return odeMap;
    }

    public void analyzeGuard(HMLParser.GuardContext guard, VariableLink variableLink) {
        ParseTreeProperty<AbstractExpr> guardPtp = HML2SMT.getGuardPtp();

        AbstractExpr abstractExpr = guardPtp.get(guard);

        if (variableLink!=null) {
            //variableLink.printAll();
            abstractExpr.resolve(variableLink);
        }
        refreshExpression(abstractExpr);
        // 因为Guard是没有副作用的，所以可以放入ID2ExpMap中
        // 在导出公式的时候需要处理这个特殊的ID
        ID2ExpMap.put(guardName(""+guardIndex), abstractExpr);
        guardIndex++;
    }

    private String guardName (String oldName) {
        return guardPrefix + oldName;
    }

    private boolean isGuard(String name) {
        return name.startsWith(guardPrefix);
    }

    @Override
    public String toString() {

        if (resultFormula!=null) return resultFormula;
        StringBuilder sb = new StringBuilder();

        if (depth == 0) {
            System.out.println("Use the variables initializations information");
            ID2ExpMap = HML2SMT.getInitID2ExpMap();
            System.out.println("Size : " + ID2ExpMap.size());
            for (Map.Entry idem : ID2ExpMap.entrySet()) {
                System.out.println("The mapping in Initializations:" +idem.getKey());
            }
        }
        else  ID2ExpMap = new HashMap<String, AbstractExpr>();
        renderDisFormulas();
        //show all the formulas for discrete actions
        for (Map.Entry<String, AbstractExpr> abe : ID2ExpMap.entrySet()) {
            String ID = abe.getKey();
            if (ID.startsWith("@"))
                sb.append(String.format("(%s)", abe.getValue().toString(depth-1)));
            else
                sb.append(String.format("(= %s %s)", addDepthFlagToVar(abe.getKey()), abe.getValue().toString(depth-1)));
        }
        sb.append("\n");
        sb.append(renderConFormulas());
        resultFormula = sb.toString();
        return resultFormula;
    }
}
