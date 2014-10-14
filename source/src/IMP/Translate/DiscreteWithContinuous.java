package IMP.Translate;


import AntlrGen.HMLParser;
import IMP.Basic.Variable;
import IMP.Basic.VariableForSMT2;
import IMP.HML2SMT;
import IMP.Infos.AbstractExpr;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by fofo on 2014/9/30.
 * 这个Dynamic实现中用了ConcreteExpr，而没有去修改AbstractExpr
 * 这样就不会破坏已经存储的程序结构
 */
public class DiscreteWithContinuous implements Dynamic{

    private static Logger logger = LogManager.getLogger(DiscreteWithContinuous.class.getName());
    private static int odeIndex = 1;
    private static final String guardPrefix = "@guard:";
    private static HashMap<Integer,String> odeMap = new HashMap<Integer, String>();
    private int depth;
    private List<ContextWithVarLink> discrete = new ArrayList<ContextWithVarLink>();
    private ContextWithVarLink continuous;
    private String invariant = null;
    private String resultFormula;
    private String discreteResult;
    private String continuousResult;
    HashMap<String, ConcreteExpr>  ID2ExpMap;

    HashMap<ConcreteExpr, String>  TempOdesMap = new HashMap<ConcreteExpr, String>();
    private int guardIndex =0;
    private static HashMap<String, Integer> odeformula = new HashMap<String, Integer>();
    private int mode;
    private final static String clock_ode ="(= d/dt[clock] 1)";
    private final static String global_ode = "(= d/dt[global] 1)";

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void addDiscrete(ContextWithVarLink bsc){
        discrete.add(bsc);
    }

    public void addContinuous(ContextWithVarLink bsc) {
        continuous = bsc;
    }

    private String addDepthFlagToVar(String v) {
        return String.format("%s_%s_0", v, depth);
    }

    private void renderDisFormulas(){
        //reset clock
        ID2ExpMap.put("clock", new ConcreteExpr("0", AbstractExpr.Sort.CONSTANT));
        for (ContextWithVarLink c : discrete) {
            ParserRuleContext r = c.getPrc();
            if (r instanceof HMLParser.AssignmentContext) {
                HMLParser.AssignmentContext ass = (HMLParser.AssignmentContext) r;
                String ID = ass.ID().getText();
                HMLParser.ExprContext eprc = ass.expr();
                refeshAndSave(ID, eprc, c.getVrl());
            }
            else if (r instanceof HMLParser.EqWithInitContext) {
                HMLParser.EqWithInitContext eqwi = (HMLParser.EqWithInitContext) r;
                String ID = eqwi.relation().ID().getText();
                HMLParser.ExprContext eprc = eqwi.expr();
                refeshAndSave(ID, eprc, c.getVrl());
            }
            else if (r instanceof  HMLParser.GuardContext) {
                //连续行为退出条件
                analyzeGuard((HMLParser.GuardContext) r, c.getVrl());
            }
            else if (r instanceof HMLParser.ExprContext) {
                //条件选择语句中的条件表达式
                analyzeCondition((HMLParser.ExprContext) r, c.getVrl(), c.negation);

            }
        }
    }

    private void refeshAndSave(String ID, HMLParser.ExprContext eprc, VariableLink variableLink) {
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr abstractExpr = exprs.get(eprc);
        ConcreteExpr concreteExpr = new ConcreteExpr(abstractExpr);
        if (variableLink!=null) {
            concreteExpr.resolve(variableLink);

            ID = variableLink.getRealVar(ID);

        }
        refreshExpression(concreteExpr);
        ID2ExpMap.put(ID, concreteExpr);
    }



    private void refreshExpression(ConcreteExpr concreteExpr) {
        List<String> IDList = concreteExpr.getIDList();
        for (String ID : IDList) {
            ConcreteExpr concreteExprforID = ID2ExpMap.get(ID);
            if (concreteExprforID != null) {

                concreteExpr.replace(ID, concreteExprforID);
            }
        }
    }
    private String renderConFormulas(){
        StringBuilder flows = new StringBuilder();
        ContextWithVarLink r = continuous;
        if (r==null) return null;

        if (r.getPrc() instanceof  HMLParser.OdeContext) {
            //如果是方程
            HMLParser.EquationContext equ = ((HMLParser.OdeContext) r.getPrc()).equation();
            analyzeEquaiton(equ, flows, r);


        }
        StringBuilder result = new StringBuilder();
        List<String> vars = new ArrayList<String>();
        List<Map.Entry<ConcreteExpr,String>> tempOdesList = new ArrayList<Map.Entry<ConcreteExpr, String>>();
        for (Map.Entry<ConcreteExpr, String>  e : TempOdesMap.entrySet()) {
            tempOdesList.add(e);
        }
        //支持排序，使得方程的表示形式唯一
        Collections.sort(tempOdesList, new Comparator<Map.Entry<ConcreteExpr, String>>(){
            public int compare(Map.Entry<ConcreteExpr, String> arg0, Map.Entry<ConcreteExpr, String> arg1) {
                return arg0.getValue().compareTo(arg1.getValue());
            }
        });
        //这里得到result就会与顺序无关的了
        //比如(= d/dt[waterLevel] 0.5)(= d/dt[waterLevel] 1)和
        //(= d/dt[waterLevel] 1)(= d/dt[waterLevel] 0.5)就会统一成
        //(= d/dt[waterLevel] 0.5)(= d/dt[waterLevel] 1)
        for (Map.Entry<ConcreteExpr, String>  e : tempOdesList) {
            result.append(e.getValue());
            addList(vars, e.getKey().getVarsList(r.getVrl())); // 获取方程中涉及的变量
        }
        if (!odeformula.containsKey(result.toString())) {
            //如果是新的flow
            odeformula.put(result.toString(), odeIndex);
            odeMap.put(odeIndex, String.format("(define-ode flow_%s (%s))", odeIndex, result+clock_ode + global_ode)); //存储方程定义

            removeDuplicate(vars);
            OdeInSMT2 odeInSMT2 = new OdeInSMT2(vars, depth, odeIndex); //准备SMT2格式的连续行为表示
            mode = odeIndex;

            flows.append(odeInSMT2.toString());
            odeIndex++;
        }
        else {
            //如果flow定义已存在
            int index = odeformula.get(result.toString());
            removeDuplicate(vars);
            OdeInSMT2 odeInSMT2 = new OdeInSMT2(vars, depth, index); //准备SMT2格式的连续行为表示
            mode = index;
            flows.append(odeInSMT2.toString());
        }

        return flows.toString();
    }


    /**
     * 将guard转成invariant
     * @param guard
     * @param variableLink
     */
    private String guard2Invariant(HMLParser.GuardContext guard, VariableLink variableLink, int mode) {
        if (guard instanceof HMLParser.SignalGuardContext) {
            return  null;//need to do
        }
        else  {
            ParseTreeProperty<AbstractExpr> guardPtp = HML2SMT.getGuardPtp();
            ConcreteExpr concreteExpr = new ConcreteExpr(guardPtp.get(guard));
            if (variableLink != null) concreteExpr.resolve(variableLink);
            ConcreteExpr result = invariantExpr(concreteExpr);
            return String.format("(forall_t %s [0 time_%s] %s) ", mode,  depth, result.toString(depth));
            //因为是对当前的变量进行约束，所以使用当前depth
        }
    }

    private ConcreteExpr createClockExpr(String op){
       return new ConcreteExpr(op, AbstractExpr.Sort.NVAR,
                new ConcreteExpr("clock", AbstractExpr.Sort.VAR),
                new ConcreteExpr("time_"+depth, AbstractExpr.Sort.NVAR));
    }

    private ConcreteExpr invariantExpr(ConcreteExpr concreteExpr){
        return concreteExpr.negation();
        //测试表明dReal SMT2 公式里面的区间表示虽然形式上是闭区间，但实际上却是开区间的
        /*
        ConcreteExpr clock_term =  createClockExpr("=");
        ConcreteExpr termination = new ConcreteExpr("=>", AbstractExpr.Sort.NVAR, clock_term, concreteExpr);
        ConcreteExpr clock_stable = createClockExpr("<");
        ConcreteExpr stable = new ConcreteExpr("=>", AbstractExpr.Sort.NVAR, clock_stable, concreteExpr.negation());
        return new ConcreteExpr("and", AbstractExpr.Sort.NVAR, stable, termination);
        */
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
        ConcreteExpr flow = new ConcreteExpr(resolveRelation(relation)); //得到方程的抽象表示

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

    public static  HashMap<Integer, String> getOdeMap() {
        return odeMap;
    }

    private void analyzeGuard(HMLParser.GuardContext guard, VariableLink variableLink) {
        ParseTreeProperty<AbstractExpr> guardPtp = HML2SMT.getGuardPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(guardPtp.get(guard));
        if (variableLink!=null) {
            //variableLink.printAll();
            concreteExpr.resolve(variableLink);
        }
        refreshExpression(concreteExpr);
        // 因为Guard是没有副作用的，所以可以放入ID2ExpMap中
        // 在导出公式的时候需要处理这个特殊的ID
        ID2ExpMap.put(guardName(""+guardIndex), concreteExpr);
        //guard name 只会出现在hashmap的key中，不会在expr中出现，所以expr中不用判段guard名称前缀
        guardIndex++;
    }


    /**
     *
     * @param Condition 条件表达式
     * @param variableLink 变量关系
     * @param negation 是否要取非，若该值为true，则取需要对表达式取非
     */
    private void analyzeCondition(HMLParser.ExprContext Condition, VariableLink variableLink, boolean negation) {
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(exprs.get(Condition));

        if (variableLink!=null) concreteExpr.resolve(variableLink);

        refreshExpression(concreteExpr);

        if (negation)   concreteExpr = concreteExpr.negation();

        // 因为Guard是没有副作用的，所以可以放入ID2ExpMap中
        // 在导出公式的时候需要处理这个特殊的ID
        ID2ExpMap.put(guardName(""+guardIndex), concreteExpr);
        //guard name 只会出现在hashmap的key中，不会在expr中出现，所以expr中不用判段guard名称前缀
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

        ID2ExpMap = new HashMap<String, ConcreteExpr>();
        if (depth == 0) {
            logger.debug("Use the variables initializations information");

            for (Map.Entry<String, AbstractExpr> e : HML2SMT.getInitID2ExpMap().entrySet()) {
                ID2ExpMap.put(e.getKey(), new ConcreteExpr(e.getValue()));
            }
            logger.debug("ID2ExpMap Size : " + ID2ExpMap.size());
            for (Map.Entry idem : ID2ExpMap.entrySet()) {
                logger.debug("The mapping in Initializations:" +idem.getKey());
            }
        }


        if (depth!=0) {
            List<VariableForSMT2> vars = HML2SMT.getVarlist();
            for (VariableForSMT2 v : vars) {
                ID2ExpMap.put(v.getName(), new ConcreteExpr(v.getName(), AbstractExpr.Sort.VAR, null, null));
            }
        }

        renderDisFormulas();
        //show all the formulas for discrete actions
        for (Map.Entry<String, ConcreteExpr> abe : ID2ExpMap.entrySet()) {
            String ID = abe.getKey();
            if (ID.startsWith("@"))
                sb.append(String.format("(%s)", abe.getValue().toString(depth-1)));
            else
                sb.append(String.format("(= %s %s)", addDepthFlagToVar(abe.getKey()), abe.getValue().toString(depth-1)));
        }
        sb.append("\n");
        sb.append(renderConFormulas());

        sb.replace(0, 0, String.format("\n(= mode_%s %s)", depth, mode));
        HMLParser.GuardContext guard = ((HMLParser.OdeContext) continuous.getPrc()).guard();
        invariant = guard2Invariant(guard, continuous.getVrl(), mode);
        //sb.append(String.format("(forall_t %s [0 time_%s] %s) ", mode,  depth, invariant));
        sb.append(invariant);
        sb.append(String.format("(= mode_%s %s)", depth, mode));
        sb.append("\n");
        int sep = sb.indexOf("\n",1);
        discreteResult = sb.substring(0, sep);
        continuousResult = sb.substring(sep);//连续部分也包含了不变式
        resultFormula = sb.toString();
        return resultFormula;
    }

    public HashMap<Integer, String> getOdeDefinitionMap(){
        return odeMap;
    }
    public DiscreteWithContinuous copy(){
        DiscreteWithContinuous obj = new DiscreteWithContinuous();
        obj.discrete = new ArrayList<ContextWithVarLink>();
        for (ContextWithVarLink e : this.discrete) {
            obj.discrete.add(e);
        }
        obj.continuous = this.continuous;
        obj.depth = this.depth;
        return obj;
    }


    @Override
    public String getDiscreteDynamics() {
        return discreteResult;
    }

    @Override
    public String getContinuousDynamics() {
        return continuousResult;
    }


    @Override
    public void setDiscreteDynamics(String discreteDynamics) {
        this.discreteResult = discreteDynamics;
    }

    @Override
    public void setContinuousDynamics(String continuousDynamics) {
        this.continuousResult = continuousDynamics;
    }

    public ContextWithVarLink getContinuous() {
        return continuous;
    }


}
