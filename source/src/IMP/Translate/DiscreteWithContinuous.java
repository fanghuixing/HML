package IMP.Translate;


import AntlrGen.HMLParser;
import IMP.Basic.VariableForSMT2;
import IMP.HML2SMT;
import IMP.Infos.AbstractExpr;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by Huixing Fang on 2014/9/30.
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
    private boolean guardCheckEnable = false;
    HashMap<String, ConcreteExpr>  ID2ExpMap;
    private boolean odeFlag = false;
    private List<String> continuousVariables;



    HashMap<ConcreteExpr, String>  TempOdesMap = new HashMap<ConcreteExpr, String>();
    private int guardIndex =0;
    private static HashMap<String, Integer> odeformula = new HashMap<String, Integer>();
    private int mode;
    private final static String clock_ode ="(= d/dt[clock] 1)";
    private final static String global_ode = "(= d/dt[global] 1)";
    private final static ConcreteExpr emptyGuard =
            new ConcreteExpr("<=", AbstractExpr.Sort.GUARD,
            new ConcreteExpr("clock", AbstractExpr.Sort.VAR),
            new ConcreteExpr("0", AbstractExpr.Sort.CONSTANT));
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void addDiscrete(ContextWithVarLink bsc){
        discrete.add(bsc);
    }

    public void addContinuous(ContextWithVarLink bsc) {
        continuous = bsc;
    }

    private String addDepthFlagToVar(String v, String surfix) {
        if (HML2SMT.isSignal(v)) return String.format("%s_%s", v, depth);

        return String.format("%s_%s_%s", v, depth, surfix);
    }

    private void renderDisFormulas(){


        for (ContextWithVarLink c : discrete) {
            ParserRuleContext r = c.getPrc();
            if (r instanceof HMLParser.AssignmentContext) {
                HMLParser.AssignmentContext ass = (HMLParser.AssignmentContext) r;
                String ID = ass.ID().getText();
                HMLParser.ExprContext eprc = ass.expr();
                refreshAndSave(ID, eprc, c.getVrl());
            }
            else if (r instanceof HMLParser.SendSignalContext) {
                HMLParser.SendSignalContext sig = (HMLParser.SendSignalContext) r;
                String ID = sig.signal().ID().getText();
                AbstractExpr expr = new AbstractExpr("global", AbstractExpr.Sort.VAR);
                refreshAndSave(ID, expr, c.getVrl());
            }
            else if (r instanceof HMLParser.EqWithInitContext) {
                HMLParser.EqWithInitContext eqwi = (HMLParser.EqWithInitContext) r;
                String ID = eqwi.relation().ID().getText();
                HMLParser.ExprContext eprc = eqwi.expr();
                refreshAndSave(ID, eprc, c.getVrl());
            }
            else if (r instanceof  HMLParser.GuardContext) {
                //连续行为退出条件
                analyzeGuard( r, c.getVrl(), c.negation);

            }
            else if (r instanceof HMLParser.ExprContext) {
                //条件选择语句中的条件表达式
                analyzeCondition((HMLParser.ExprContext) r, c.getVrl(), c.negation);

            }
            else if (r instanceof HMLParser.SuspendContext) {
                analyzeGuard(r, c.getVrl(), c.negation);
            } else if (r instanceof HMLParser.WhenProContext) {
                // if you need to add the when guard to the starting point
                // we don't need this usually
                analyzeGuard(r, c.getVrl(), c.negation);
            }
        }
        //reset clock, after the guard has been analyzed.
        ID2ExpMap.put("clock", new ConcreteExpr("0", AbstractExpr.Sort.CONSTANT));
    }

    private void refreshAndSave(String ID, HMLParser.ExprContext eprc, VariableLink variableLink) {
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

    private void refreshAndSave(String ID, AbstractExpr expr, VariableLink variableLink) {
        ConcreteExpr concreteExpr = new ConcreteExpr(expr);
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
        ContextWithVarLink r = continuous;
        if (r==null) return "";
        if (r.getPrc() instanceof  HMLParser.OdeContext) {
            //如果是方程
            HMLParser.EquationContext equ = ((HMLParser.OdeContext) r.getPrc()).equation();
            analyzeEquaiton(equ, r);
        } else if (r.getPrc() instanceof HMLParser.SuspendContext) {
            HMLParser.EquationContext equ = null;
            analyzeEquaiton(equ, r);
        } else if (r.getPrc() instanceof HMLParser.WhenProContext) {
            HMLParser.EquationContext equ = null;
            analyzeEquaiton(equ, r);
        }
        StringBuilder result = new StringBuilder();
        List<String> vars = new ArrayList<String>();
        formatOdeAndVars(result,vars,r);
        return checkAndAddOde(result, vars);
    }


    private void formatOdeAndVars(StringBuilder result,  List<String> vars, ContextWithVarLink r){
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
    }

    private String checkAndAddOde(StringBuilder result, List<String> vars){
        continuousVariables = vars;
        StringBuilder flows = new StringBuilder();
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


    private ConcreteExpr fromGuardToConcreteExpr(ParserRuleContext guard){
        ParseTreeProperty<AbstractExpr> guardPtp = null;
        if (guard instanceof HMLParser.GuardContext) {
            while (guard instanceof HMLParser.ParGuardContext) {
                guard = ((HMLParser.ParGuardContext) guard).guard();
            }
            guardPtp = HML2SMT.getGuardPtp();
        } else if (guard instanceof HMLParser.SuspendContext) {
            guardPtp = HML2SMT.getExprPtp();
        } else if (guard instanceof HMLParser.WhenProContext) {
            guardPtp = HML2SMT.getGuardPtp();
        }

        return new ConcreteExpr(guardPtp.get(guard));
    }

    /**
     * 将　guard　转成　invariant
     * @param guard
     * @param variableLink
     */
    private String guard2Invariant(ParserRuleContext guard, VariableLink variableLink, int mode) {
        ConcreteExpr concreteExpr = fromGuardToConcreteExpr(guard);
        if (variableLink != null) concreteExpr.resolve(variableLink);
        //连续变化过程中满足的条件，即不满足guard的情况
        ConcreteExpr result = concreteExpr.negationForInv();
        List<String> branches = result.guardBranches(depth);
        String inv = mergeBranches(branches, mode, depth);
//        if (!guardCheckEnable) {
//            //如果没有启用guard检查，就需要处理瞬间返回的情况
//            //String empty = String.format("(and %s %s)", emptyGuard.toString(depth), concreteExpr.toString(depth));
//            //return String.format("(or %s %s)", empty, inv);
//            String test = String.format("(=> %s %s)", concreteExpr.toStringForStartPoint(depth),
//                    String.format("(forall_t %s [0 time_%s] %s)", mode,  depth, emptyGuard.toString(depth)));
//            return String.format("(and %s %s)", test, inv);
//            //因为是对当前的变量进行约束，所以使用当前depth
//        }
//        else
        return inv;
        //guardCheckEnable为真时候表示我们不需要再添加guard条件到公式中，因为我们已经判断过这个条件

    }

    private String mergeBranches(List<String> branches, int mode, int depth){
        StringBuilder sb = new StringBuilder();
        if (branches.size()==1)
            return  String.format("(forall_t %s [0 time_%s] %s)", mode,  depth, branches.remove(0));
        else if (branches.size()==0) return null;
        else {
            sb.append(String.format("(and %s %s)",
                    String.format("(forall_t %s [0 time_%s] %s)", mode,  depth, branches.remove(0)),
                    mergeBranches(branches, mode, depth)));
            return sb.toString();
        }
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

    private void analyzeEquaiton(HMLParser.EquationContext equ, ContextWithVarLink r){
        if (equ == null) {
            analyzeRelation(null, r);
        }
        else if (equ instanceof HMLParser.EqWithNoInitContext) {
            //如果方程不带初值
            HMLParser.RelationContext relation  = ((HMLParser.EqWithNoInitContext) equ).relation();
            analyzeRelation(relation, r);
        }
        else if (equ instanceof HMLParser.EqWithInitContext) {
            //如果方程不带初值
            HMLParser.RelationContext relation  = ((HMLParser.EqWithInitContext) equ).relation();
            analyzeRelation(relation, r);
        }
        else if (equ instanceof HMLParser.ParaEqContext) {
            //如果是方程组
            for (HMLParser.EquationContext e : ((HMLParser.ParaEqContext) equ).equation()) {
                analyzeEquaiton(e, r);

            }
        }
    }

    private void analyzeRelation(HMLParser.RelationContext relation, ContextWithVarLink r){
        ConcreteExpr flow = new ConcreteExpr(resolveRelation(relation)); //得到方程的抽象表示

        String result = flow.toString(r.getVrl());     //还原真实变量

        TempOdesMap.put(flow, result);

    }

    private AbstractExpr resolveRelation(HMLParser.RelationContext relation) {
        if (relation == null) return new AbstractExpr("", null, null );
        String varName = relation.ID().getText();
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        AbstractExpr left = new AbstractExpr("d/dt", new AbstractExpr(varName, AbstractExpr.Sort.VAR),null);
        AbstractExpr right = exprs.get(relation.expr());
        return new AbstractExpr("=", left, right);
    }

    public static  HashMap<Integer, String> getOdeMap() {
        return odeMap;
    }

    private void analyzeGuard(ParserRuleContext guard, VariableLink variableLink, boolean negation) {
        ConcreteExpr concreteExpr =  fromGuardToConcreteExpr(guard);



        if (variableLink != null) concreteExpr.resolve(variableLink);
        refreshExpression(concreteExpr);
        if (negation)   concreteExpr = concreteExpr.negation();
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

    private void prepareIDMap(){
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

            // for signals
            List<String> signals = HML2SMT.getSignals();
            for (String s : signals) {
                ID2ExpMap.put(s, new ConcreteExpr("-1", AbstractExpr.Sort.CONSTANT, null, null));
            }

        }
        else {
            // simulate the skip statement
            List<VariableForSMT2> vars = HML2SMT.getVarlist();
            for (VariableForSMT2 v : vars) {
                ID2ExpMap.put(v.getName(), new ConcreteExpr(v.getName(), AbstractExpr.Sort.VAR, null, null));
            }

            // for signals
            List<String> signals = HML2SMT.getSignals();
            StringBuilder sb = new StringBuilder();
            for (String s : signals) {
                sb.append(s).append("_").append(depth-1);
                ID2ExpMap.put(s, new ConcreteExpr(sb.toString(), AbstractExpr.Sort.CONSTANT, null, null));
                sb.delete( 0, sb.length() );
            }
        }
    }

    /**
     * Translate vars variants into SMT2 formulas
     * @param sb
     */
    private void transDisExprToSMT(StringBuilder sb){
        //show all the formulas for discrete actions
        for (Map.Entry<String, ConcreteExpr> abe : ID2ExpMap.entrySet()) {
            String ID = abe.getKey();
            if (isGuard(ID)) //guard condition (ID.startsWith("@"))
                sb.append(String.format(" %s ", abe.getValue().toString(depth-1)));
            else {
                sb.append(String.format(" (= %s %s) ", addDepthFlagToVar(abe.getKey(),"0"), abe.getValue().toString(depth - 1)));
                //String name = abe.getKey();

               // for vars that are not changed during the flow(suspend and when do not change vars)
//                if (continuous!=null && ((continuous.getParallel()!=null && odeFlag==false) ||
//                        continuous.getPrc() instanceof HMLParser.SuspendContext ||
//                        continuous.getPrc() instanceof HMLParser.WhenProContext)
//                        && !name.equals("clock") && !name.equals("global") && !HML2SMT.isSignal(name)) {
//                    sb.append(String.format(" (= %s %s) ", addDepthFlagToVar(abe.getKey(),"t"), addDepthFlagToVar(abe.getKey(),"0")));
//                }
            }
        }
        sb.append("\n");
    }


    private StringBuilder setUnchangedVars(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ConcreteExpr> abe : ID2ExpMap.entrySet()) {
            String ID = abe.getKey();
            if (!isGuard(ID)) {
                String name = abe.getKey();
                //logger.debug("Continuous Var : " + name);
                // for vars that are not changed during the flow(suspend and when do not change vars)
                if (continuous != null && !(continuousVariables.contains(name))
                    && !name.equals("clock") && !name.equals("global") && !HML2SMT.isSignal(name)) {
                    sb.append(String.format(" (= %s %s) ", addDepthFlagToVar(abe.getKey(), "t"), addDepthFlagToVar(abe.getKey(), "0")));
                }
            }
        }
        return sb;
    }



    private void renderGuard(StringBuilder sb){
        ParserRuleContext guard = null;
        ParserRuleContext parserRuleContext = continuous.getPrc();
        if (parserRuleContext instanceof HMLParser.OdeContext) {
            guard = ((HMLParser.OdeContext) parserRuleContext).guard();
        } else if (parserRuleContext instanceof HMLParser.SuspendContext) {
            guard = parserRuleContext;
        } else if (parserRuleContext instanceof HMLParser.WhenProContext) {
            guard = parserRuleContext;
        }



        invariant = guard2Invariant(guard, continuous.getVrl(), mode);
        sb.append(invariant);
    }


    private void checkContinuousList(List<ContextWithVarLink> conList){
        for (ContextWithVarLink con : conList) {
            if (con.getPrc() instanceof HMLParser.OdeContext) {
                odeFlag = true;
                return;
            }
        }
    }

    @Override
    public String toString() {

        //if (resultFormula!=null) return resultFormula;
        prepareIDMap();
        renderDisFormulas();
        StringBuilder sb = new StringBuilder();
        transDisExprToSMT(sb);
        List<ContextWithVarLink> conList = continuous.getParallel();
        if (conList!=null) {
            checkContinuousList(conList);
            if (odeFlag) {
                sb.append(renderParaSubOdes(conList));
            }
            else {
                continuous = conList.get(0);
                sb.append(renderConFormulas());
            }

        } else {
            sb.append(renderConFormulas());
        }
        sb.replace(0, 0, String.format("\n(= mode_%s %s)", depth, mode));

        if (conList!=null) {
            for (ContextWithVarLink con : conList) {
                continuous = con;
                renderGuard(sb);
            }
        } else {
            renderGuard(sb);
        }
        sb.append(String.format("(= mode_%s %s)", depth, mode));
        sb.append("\n");
        int sep = sb.indexOf("\n",1);
        sb.insert(sep, setUnchangedVars());
        discreteResult = sb.substring(0, sep);
        continuousResult = sb.substring(sep);//连续部分也包含了不变式
        resultFormula = sb.toString();
        return resultFormula;
    }

    // for the parallel composition of sub-pros that contains odes
    private String renderParaSubOdes(List <ContextWithVarLink> conList) {
        StringBuilder result = new StringBuilder();
        List<String> vars = new ArrayList<String>();
        for (ContextWithVarLink con : conList) {
            if (con.getPrc() instanceof HMLParser.OdeContext) {
                continuous = con;
                HMLParser.EquationContext equ = ((HMLParser.OdeContext) continuous.getPrc()).equation();
                analyzeEquaiton(equ, continuous);
                formatOdeAndVars(result,vars,continuous);
                //for each flow we need a new ode map
                TempOdesMap = new HashMap<ConcreteExpr, String>();
            }
        }
        return checkAndAddOde(result,vars);
    }

    /**
     *
     * @param concreteExpr 表达式
     * @param variableLink 变量关系
     * @return SMT2公式用于动态检查条件是否满足
     */
    public String getPartialResult(ConcreteExpr concreteExpr, VariableLink variableLink) {
        prepareIDMap();
        renderDisFormulas();
        StringBuilder sb = new StringBuilder();
        transDisExprToSMT(sb);
        sb.append(setUnchangedVarsForPartial());//for vars that not changed
        if (variableLink != null) concreteExpr.resolve(variableLink);
        concreteExpr.checkEmptyGuard();
        String startPoint = concreteExpr.toStringForStartPoint(depth);
        logger.debug("Start Point: " + startPoint);
        sb.append(startPoint);
        return sb.toString();
    }

    private StringBuilder setUnchangedVarsForPartial(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ConcreteExpr> abe : ID2ExpMap.entrySet()) {
            String ID = abe.getKey();
            if (!isGuard(ID)) {
                String name = abe.getKey();
                //logger.debug("Continuous Var : " + name);
                // for vars that are not changed during the flow(suspend and when do not change vars)
                if (!name.equals("clock") && !name.equals("global") && !HML2SMT.isSignal(name)) {
                    sb.append(String.format(" (= %s %s) ", addDepthFlagToVar(abe.getKey(), "t"), addDepthFlagToVar(abe.getKey(), "0")));
                }
            }
        }
        return sb;
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

    public void setGuardCheckEnable(boolean guardCheckEnable){
        this.guardCheckEnable = guardCheckEnable;
    }

    private ConcreteExpr createClockExpr(String op){
        return new ConcreteExpr(op, AbstractExpr.Sort.NVAR,
                new ConcreteExpr("clock", AbstractExpr.Sort.VAR),
                new ConcreteExpr("time_"+depth, AbstractExpr.Sort.NVAR));
    }

    private ConcreteExpr invariantExpr(ConcreteExpr concreteExpr){
        return concreteExpr.negationForInv();
        //测试表明dReal SMT2 公式里面的区间表示虽然形式上是闭区间，但实际上却是开区间的
        /*
        ConcreteExpr clock_term =  createClockExpr("=");
        ConcreteExpr termination = new ConcreteExpr("=>", AbstractExpr.Sort.NVAR, clock_term, concreteExpr);
        ConcreteExpr clock_stable = createClockExpr("<");
        ConcreteExpr stable = new ConcreteExpr("=>", AbstractExpr.Sort.NVAR, clock_stable, concreteExpr.negation());
        return new ConcreteExpr("and", AbstractExpr.Sort.NVAR, stable, termination);
        */
    }



}
