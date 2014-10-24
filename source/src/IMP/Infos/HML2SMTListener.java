package IMP.Infos;
import IMP.Basic.*;
import IMP.Translate.VariableLink;
import org.antlr.v4.runtime.Token;
import AntlrGen.HMLBaseListener;
import AntlrGen.HMLParser;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
/**
 * Created by Huixing Fang on 2014/9/25.
 *
 */
public class HML2SMTListener extends HMLBaseListener {
    private static Logger  logger = LogManager.getLogger(HML2SMTListener.class.getName());

    //用于转换SMT2公式的变量列表
    private List<VariableForSMT2> varlist = new ArrayList<VariableForSMT2>();
    private List<Constraint> constrList = new ArrayList<Constraint>();
    //变量和常量map, key为变量名
    private HashMap<String, Variable> vars = new HashMap<String, Variable>();
    HashMap<String, AbstractExpr>  InitID2ExpMap = new HashMap<String, AbstractExpr>();
    private ParseTreeProperty<AbstractExpr> exprPtp = new ParseTreeProperty<AbstractExpr>();
    private ParseTreeProperty<AbstractExpr> guardPtp = new ParseTreeProperty<AbstractExpr>();
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();
    private VariableLink finalVariableLinks = new VariableLink(null);
    private List<String> signals = new ArrayList<String>();


    public VariableLink getFinalVariableLinks() {
        return finalVariableLinks;
    }

    public List<VariableForSMT2> getVarlist() {
        return varlist;
    }

    public HashMap<String, Template> getTmpMap() {
        return tmpMap;
    }


    public HashMap<String, AbstractExpr> getInitID2ExpMap() {
        return InitID2ExpMap;
    }

    /**
     * 获取变量初值
     * @return 变量初始值
     */
    public String getInitializations( ) {
        StringBuilder inits = new StringBuilder();
        inits.append("(and ");

        Iterator it = vars.entrySet().iterator();

        String key;
        Variable value;
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = (Variable) entry.getValue();
            if (!value.isFinal) {
                // 常量不用声明，直接在转成的公式中替换
                inits.append(String.format("(= %s_0_0 %s) ", key, value.init.getText()));
                InitID2ExpMap.put(key, new AbstractExpr(value.init.getText()));
            }
        }
        inits.append(")");
        return inits.toString();
    }

    public ParseTreeProperty<AbstractExpr> getExprPtp() {
        return exprPtp;
    }

    /**
     * Scanning of Variables
     */
    public void  exitVariableDeclaration(HMLParser.VariableDeclarationContext ctx){
        boolean isFinal = false;
        String type = null;
        // 判断类型
        if (ctx.type().primitiveType() instanceof HMLParser.BoolTypeContext)
            type = "Bool";
        else if (ctx.type().primitiveType() instanceof HMLParser.IntTypeContext)
            type = "Int";
        else if (ctx.type().primitiveType() instanceof HMLParser.FloatTypeContext)
            type = "Real";

        //判断是否是常量
        if (ctx.modifier() instanceof HMLParser.FinalModifierContext)
            isFinal = true;

        //得到变量列表
        String varName;
        Variable newVar;
        List<HMLParser.VariableDeclaratorContext> vds = ctx.variableDeclarators().variableDeclarator();
        for (HMLParser.VariableDeclaratorContext v : vds) {
            varName = v.variableDeclaratorId().ID().getText();
            newVar = new Variable(type, v.variableInitializer(), isFinal);
            vars.put(varName, newVar);
            if (!isFinal)
                varlist.add(new VariableForSMT2(varName, type));
        }
    }

    public void exitIDExpr(HMLParser.IDExprContext ctx) {
        String ID = ctx.getText();
        Variable var = vars.get(ID);
        if (var == null) {
            exprPtp.put(ctx, new AbstractExpr(ID, AbstractExpr.Sort.VAR)); // 当这个变量是Template参数变量时候
            return;
        }
        if (var.isFinal) {
            exprPtp.put(ctx, exprPtp.get(var.init.expr()));
            finalVariableLinks.setRealVar(ctx.ID().getText(), "@"+exprPtp.get(ctx).toString());
            //以@开头表示这个值是常量
        }
        else
            exprPtp.put(ctx, new AbstractExpr(ID, AbstractExpr.Sort.VAR));
    }

    public void exitINTExpr(HMLParser.INTExprContext ctx){
        exprPtp.put(ctx, new AbstractExpr(ctx.getText()));
    }

    public void exitFLOATExpr(HMLParser.FLOATExprContext ctx){
        exprPtp.put(ctx, new AbstractExpr(ctx.getText()));
    }

    public void exitConstantTrue(HMLParser.ConstantTrueContext ctx){
        exprPtp.put(ctx,  new AbstractExpr(ctx.getText()));
    }

    public void exitConstantFalse(HMLParser.ConstantFalseContext ctx){
        exprPtp.put(ctx, new AbstractExpr(ctx.getText()));
    }

    public void exitNegationExpr(HMLParser.NegationExprContext ctx){
        if (ctx.prefix.getText().equals("-")) {
            String ID = "-";
            AbstractExpr left =  new AbstractExpr("0");
            AbstractExpr right = exprPtp.get(ctx.expr());
            exprPtp.put(ctx,new AbstractExpr(ID, left, right));
        }
        else{
            String ID = "not";
            AbstractExpr left = exprPtp.get(ctx.expr());
            exprPtp.put(ctx,new AbstractExpr(ID, left, null));
        }
    }

    public void exitMExpr(HMLParser.MExprContext ctx) {
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitAExpr(HMLParser.AExprContext ctx) {

        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitCompExpr(HMLParser.CompExprContext ctx) {
        logger.debug("=================================comp expr=================================" + ctx.getText());
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitLogicalAndExpr(HMLParser.LogicalAndExprContext ctx) {
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitLogicalOrExpr(HMLParser.LogicalOrExprContext ctx) {
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitLogicalXorExpr(HMLParser.LogicalXorExprContext ctx) {
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    private void setExprPtpForTriple(HMLParser.ExprContext ctx,
        HMLParser.ExprContext leftEC, Token opt, HMLParser.ExprContext rightEC) {
        AbstractExpr left = exprPtp.get(leftEC);
        AbstractExpr right = exprPtp.get(rightEC);
        String op = opt.getText();
        if (op.equals("==")) op = "=";
        exprPtp.put(ctx,  new AbstractExpr(op, left, right));
    }

    public void exitFloorExpr(HMLParser.FloorExprContext ctx) {
        exprPtp.put(ctx,new AbstractExpr("floor", exprPtp.get(ctx.expr()), null));
    }

    public void exitCeilExpr(HMLParser.CeilExprContext ctx) {
        exprPtp.put(ctx,new AbstractExpr("ceil", exprPtp.get(ctx.expr()), null));
    }

    public void exitParExpr(HMLParser.ParExprContext ctx) {
        exprPtp.put(ctx, exprPtp.get(ctx.parExpression().expr()));
    }

    public void exitTemplate(HMLParser.TemplateContext ctx) {
        logger.debug("Exit Template ...");
        StringBuilder key = new StringBuilder();
        List<String> formalVarNames = new ArrayList<String>();
        key.append(ctx.ID().getText());
        HMLParser.FormalParameterDeclsContext fpc = ctx.formalParameters().formalParameterDecls();
        while (fpc != null) {
            key.append(fpc.type().getText());
            formalVarNames.add(fpc.formalParameterDeclsRest().variableDeclaratorId().ID().getText());
            fpc = fpc.formalParameterDeclsRest().formalParameterDecls();
        }
        if (tmpMap.get(key.toString()) != null) {
            logger.debug("Duplicated Template " + key + " had been defined already!");
            return;
        }
        //以模板名加参数类型为key保存Template, tmpMap为存储体
        tmpMap.put(key.toString(), new Template(formalVarNames, ctx));
        logger.debug("Store Template: " +key.toString());
    }

    public void exitVariableConstraint(HMLParser.VariableConstraintContext ctx) {
        String name = ctx.ID().getText();
        String leftEnd = ctx.leftEnd.getText();
        String rightEnd = ctx.rightEnd.getText();
        constrList.add(new Constraint(name, leftEnd, rightEnd));
        if (name.equals("global")) {
            for (String s : signals) {
                constrList.add(new Constraint(s, "-1", rightEnd, "signal"));
            }
        }
    }

    public List<Constraint> getConstraintsList(){
        return  this.constrList;
    }

    public void exitEmptyGuard(HMLParser.EmptyGuardContext ctx) {
        guardPtp.put(ctx, new AbstractExpr("true", AbstractExpr.Sort.GUARD, null, null));
    }

    public void exitSignalGuard(HMLParser.SignalGuardContext ctx) {
        AbstractExpr left = new AbstractExpr(ctx.signal().ID().getText(), AbstractExpr.Sort.Signal, null, null);
        guardPtp.put(ctx, new AbstractExpr(">=", AbstractExpr.Sort.GUARD, left, new AbstractExpr("global", AbstractExpr.Sort.VAR)));
    }

    public void exitBoolGuard(HMLParser.BoolGuardContext ctx) {

        guardPtp.put(ctx, exprPtp.get(ctx.expr()));
    }

    public void exitTimeOutGuard(HMLParser.TimeOutGuardContext ctx) {
        guardPtp.put(ctx, new AbstractExpr(">=", AbstractExpr.Sort.GUARD, new AbstractExpr("clock", AbstractExpr.Sort.VAR) , exprPtp.get(ctx.expr())));
    }

    public void exitConjunctGuard(HMLParser.ConjunctGuardContext ctx) {
        guardPtp.put(ctx, new AbstractExpr("and", AbstractExpr.Sort.GUARD,
                guardPtp.get(ctx.guard(0)), guardPtp.get(ctx.guard(1))));
    }

    public void exitDisjunctGuard(HMLParser.DisjunctGuardContext ctx) {
        guardPtp.put(ctx, new AbstractExpr("or", AbstractExpr.Sort.GUARD,
                guardPtp.get(ctx.guard(0)), guardPtp.get(ctx.guard(1))));
    }

    public void exitParGuard(HMLParser.ParGuardContext ctx) {
        guardPtp.put(ctx, guardPtp.get(ctx.guard()));
    }

    public void exitSignalDeclaration(HMLParser.SignalDeclarationContext ctx) {
        logger.debug("Exit Signal Declaration " + ctx.ID());
        signals.add(ctx.ID().getText());
    }

    public void exitSendSignal(HMLParser.SendSignalContext ctx) {
        logger.debug("Exit SendSignal " + ctx.getText());

    }

    public void exitSuspend(HMLParser.SuspendContext ctx) {
        logger.debug("Exit Suspend " + ctx.getText());
        exprPtp.put(ctx, new AbstractExpr(">=", new AbstractExpr("clock", AbstractExpr.Sort.VAR), exprPtp.get(ctx.expr())));
    }

    public void exitWhenPro(HMLParser.WhenProContext ctx) {
        logger.debug("Exit When Statement " + ctx.getText());
        HMLParser.GuardedChoiceContext guardedChoice = ctx.guardedChoice();
        List<HMLParser.SingleGuardedChoiceContext> gcList = guardedChoice.singleGuardedChoice();
        AbstractExpr res;
        if (gcList.size()>1) {
            // more than one guarded choice
            res = new AbstractExpr("or", AbstractExpr.Sort.GUARD, null, null);
            for (HMLParser.SingleGuardedChoiceContext sgc : gcList) {
                AbstractExpr abe = guardPtp.get(sgc.guard());
                res = res.addGuardedChoice(abe, "or");
            }
        } else {
            // only one guarded choice
            res = guardPtp.get(gcList.get(0).guard());
        }
        guardPtp.put(ctx, res);

    }

    public ParseTreeProperty<AbstractExpr> getGuardPtp() {
        return guardPtp;
    }

    public List<String> getSignals() {
        return signals;
    }
}
