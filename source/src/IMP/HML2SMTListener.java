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

    //用于转换SMT2公式的变量列表
    private List<VariableForSMT2> varlist = new ArrayList<VariableForSMT2>();
    //变量和常量map, key为变量名
    private HashMap<String, Variable> vars = new HashMap<String, Variable>();
    private ParseTreeProperty<String> exprPtp = new ParseTreeProperty<String>();
    private ParseTreeProperty<String> flow = new ParseTreeProperty<String>();
    private HashMap<HMLParser.OdeContext, Flow> flows  = new HashMap<HMLParser.OdeContext, Flow>();
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();
    private int indexOfFlow = 0;
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

    public List<VariableForSMT2> getVarlist() {
        return varlist;
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

    public void exitTemplate(HMLParser.TemplateContext ctx) {
        log.log("Exit Template ...");
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
            log.log("Duplicated Template " + key + " had been defined already!");
            return;
        }
        //以模板名加参数类型为key保存Template, tmpMap为存储体
        tmpMap.put(key.toString(), new Template(formalVarNames, ctx));
        log.log("Store Template: " +key.toString());
    }

}
