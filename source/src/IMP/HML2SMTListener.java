package IMP;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;
import AntlrGen.HMLBaseListener;
import AntlrGen.HMLParser;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.*;

/**
 * Created by Huixing Fang on 2014/9/25.
 *
 */
public class HML2SMTListener extends HMLBaseListener {
    Logger log = Logger.getInstance();


    private HashMap<String, Variable> vars = new HashMap<String, Variable>();
    private ParseTreeProperty<String> exprPtp = new ParseTreeProperty<String>();

    private ParseTreeProperty<String> flow = new ParseTreeProperty<String>();
    private HashMap<HMLParser.OdeContext, Flow> flows  = new HashMap<HMLParser.OdeContext, Flow>();
    private HashMap<String, List> templs = new HashMap<String, List>();
    private int indexOfFlow = 0;
    /**
     *
     * @return 以SMT2 公式的形式表示的变量声明字符串（不包含常量）
     */
    public String getVarsInSMT2Formula(int depth){
        if(vars == null){
            return "";
        }
        StringBuilder svars = new StringBuilder();
        Iterator it = vars.entrySet().iterator();

        String key;
        Variable value;
        while(it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = (Variable) entry.getValue();
            if (value.isFinal) continue; // 常量不用声明，直接在转成的公式中替换

            svars.append(String.format("(declare-fun %s () %s)", key,  value.type));
            svars.append('\n');
            for (int i=0; i < depth; i++){
                svars.append(String.format("(declare-fun %s_%s_0 () %s)", key, i, value.type));
                svars.append('\n');
                svars.append(String.format("(declare-fun %s_%s_t () %s)", key, i, value.type));
                svars.append('\n');
            }

        }
        return svars.toString();
    }

    /**
     * 获取变量初值
     * @return
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
            if (!value.isFinal)  // 常量不用声明，直接在转成的公式中替换
                inits.append(String.format("(= %s_0_0 %s) ", key,  value.init.getText()));
        }
        inits.append(")");
        return inits.toString();
    }

    private String getFlow(ParseTree ctx) { return flow.get(ctx); }
    private void setFlow(ParseTree ctx, String s) { flow.put(ctx, s); }

    public String getFlowsListInString(){
        StringBuilder flowsString = new StringBuilder();
        Collection<Flow> fs = flows.values();
        for (Flow f : fs){
            flowsString.append(String.format("(define-ode flow_%s (%s))", f.id, f.ode));
            flowsString.append("\n");
        }
        return flowsString.toString();
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
        }
    }

    public void exitIDExpr(HMLParser.IDExprContext ctx) {
        String ID = ctx.getText();
        Variable var = vars.get(ID);
        if (var == null) {
            exprPtp.put(ctx, ID); // 当这个变量是Template参数变量时候
            return;
        }
        if (var.isFinal)
            exprPtp.put(ctx, exprPtp.get(var.init.expr()));
    }

    public void exitINTExpr(HMLParser.INTExprContext ctx){
        exprPtp.put(ctx, ctx.getText());
    }

    public void exitFLOATExpr(HMLParser.FLOATExprContext ctx){
        exprPtp.put(ctx, ctx.getText());
    }

    public void exitConstantTrue(HMLParser.ConstantTrueContext ctx){
        exprPtp.put(ctx, ctx.getText());
    }

    public void exitConstantFalse(HMLParser.ConstantFalseContext ctx){
        exprPtp.put(ctx, ctx.getText());
    }

    public void exitNegationExpr(HMLParser.NegationExprContext ctx){
        if (ctx.prefix.getText().equals("-")) {
            exprPtp.put(
                    ctx, String.format("(- 0 %s)", exprPtp.get(ctx.expr()))
            );
        }
        else
            exprPtp.put(
                    ctx, String.format("(not %s)", exprPtp.get(ctx.expr()))
            );
    }

    public void exitMExpr(HMLParser.MExprContext ctx) {
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitAExpr(HMLParser.AExprContext ctx) {
        setExprPtpForTriple(ctx, ctx.left, ctx.op, ctx.right);
    }

    public void exitCompExpr(HMLParser.CompExprContext ctx) {
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
        String left = exprPtp.get(leftEC);
        String right = exprPtp.get(rightEC);
        String op = opt.getText();
        exprPtp.put(ctx, String.format("(%s %s %s)", op, left, right));
        left = null;
        right = null;
        op = null;
    }

    public void exitFloorExpr(HMLParser.FloorExprContext ctx) {
        exprPtp.put(
                ctx,
                String.format("(floor ( %s ) )", exprPtp.get(ctx.expr()))
        );
    }

    public void exitCeilExpr(HMLParser.CeilExprContext ctx) {
        exprPtp.put(
                ctx,
                String.format("(ceil ( %s ) )", exprPtp.get(ctx.expr()))
        );
    }

    public void exitParExpr(HMLParser.ParExprContext ctx) {
        exprPtp.put(
                ctx,
                String.format("( %s )", exprPtp.get(ctx.parExpression().expr()))
        );
    }

    public void enterTemplate(HMLParser.TemplateContext ctx) {
        String ID = ctx.ID().getText();
        if (templs.get(ID) != null) {
            log.log("Duplicated Template " + ID + ctx.formalParameters().getText() +  " had been defined already!");
            return;
        }
        ArrayList<HMLParser.TypeContext> ptypes = new ArrayList<HMLParser.TypeContext>();
        HMLParser.FormalParameterDeclsContext fpdc = ctx.formalParameters().formalParameterDecls();
        while (fpdc!=null) {
            ptypes.add(fpdc.type());
            fpdc = fpdc.formalParameterDeclsRest().formalParameterDecls();
        }
        templs.put(ID, ptypes);
    }





/*
    public void exitEqWithNoInit(HMLParser.EqWithNoInitContext ctx){
        setFlow(ctx, getFlow(ctx.relation()));
    }


    public void exitRelation(HMLParser.RelationContext ctx) {
        String x = String.format("(= d/dt[%s] %s)", ctx.ID().getText(), exprPtp.get(ctx.expr()));
        setFlow(ctx, x);
    }

    public void exitEqWithInit(HMLParser.EqWithInitContext ctx){
        setFlow(ctx, getFlow(ctx.relation()));
    }

    public void exitParaEq(HMLParser.ParaEqContext ctx){
        setFlow(ctx, getFlow(ctx.equation(0)) + getFlow(ctx.equation(1)));
    }

    public void exitOde(HMLParser.OdeContext ctx){
        String x =  getFlow(ctx.equation());
        Flow f = new Flow(++indexOfFlow, x);
        flows.put(ctx, f);
    }

*/

}
