package IMP;


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



    private HashMap<String, Variable> vars = new HashMap<String, Variable>();


    ParseTreeProperty<String> smt = new ParseTreeProperty<String>();

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
            if (value.isFinal) continue; // 常量不用声明，直接在转成的公式中替换
            else
                inits.append(String.format("(= %s %s) ", key,  value.init.getText()));


        }
        inits.append(")");
        return inits.toString();
    }

    String getSMT(ParseTree ctx) { return smt.get(ctx); }
    void setSMT(ParseTree ctx, String s) { smt.put(ctx, s); }

    public void exitEqWithNoInit(HMLParser.EqWithNoInitContext ctx){
        setSMT(ctx, getSMT(ctx.relation()));
    }


    public void exitRelation(HMLParser.RelationContext ctx) {
        String x = String.format("(= d/dt[%s] %s)", ctx.ID().getText(), ctx.expr().getText());
        setSMT(ctx, x);
    }

    public void exitEqWithInit(HMLParser.EqWithInitContext ctx){
        String x = String.format("(= %s %s) ", ctx.relation().ID(), ctx.expr());
        setSMT(ctx, x + getSMT(ctx.relation()));
    }

    public void exitParaEq(HMLParser.ParaEqContext ctx){
        setSMT(ctx, getSMT(ctx.equation(0)) + getSMT(ctx.equation(1)));
    }

    public void exitOde(HMLParser.OdeContext ctx){
        String x = String.format("(define-ode flow ( %s ))", getSMT(ctx.equation()));
        setSMT(ctx, x);
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


}
