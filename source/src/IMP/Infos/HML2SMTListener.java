package IMP.Infos;
import IMP.Basic.*;
import IMP.Translate.AbstractExpr;
import org.antlr.v4.runtime.Token;
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
    private List<Constraint> constrList = new ArrayList<Constraint>();
    //变量和常量map, key为变量名
    private HashMap<String, Variable> vars = new HashMap<String, Variable>();
    HashMap<String, AbstractExpr>  InitID2ExpMap = new HashMap<String, AbstractExpr>();
    private ParseTreeProperty<AbstractExpr> exprPtp = new ParseTreeProperty<AbstractExpr>();
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();
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
        if (var.isFinal)
            exprPtp.put(ctx, exprPtp.get(var.init.expr()));
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

    public void exitVariableConstraint(HMLParser.VariableConstraintContext ctx) {
        String name = ctx.ID().getText();
        String leftEnd = ctx.leftEnd.getText();
        String rightEnd = ctx.rightEnd.getText();
        constrList.add(new Constraint(name, leftEnd, rightEnd));
    }

    public List<Constraint> getConstraintsList(){
        return  this.constrList;
    }
}
