package IMP.Translate;

import AntlrGen.HMLBaseVisitor;
import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.Scope.GlobalScope;
import IMP.Scope.Scope;
import IMP.Scope.Symbol;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by fofo on 2014/9/30.
 */
public class HMLProgram2SMT extends HMLBaseVisitor<Void> {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope
    private int currentDepth;
    private int depth;
    private List<Dynamics> dynamicsList = new ArrayList<Dynamics>();
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();

    Dynamics currentDynamics = new Dynamics();


    public HMLProgram2SMT(ParseTreeProperty<Scope> scopes, GlobalScope globals, HashMap<String, Template> tmpMap, int depth) {
        this.scopes = scopes;
        this.globals = globals;
        this.depth = depth;
        this.tmpMap = tmpMap;
    }



    public List<Dynamics> getDynamicsList() {
        return dynamicsList;
    }

    public Void visitHybridModel(HMLParser.HybridModelContext ctx) {
        System.out.println("Visiting HybridModel... ... ...");
        currentScope = globals;
        visit(ctx.program());
        return null;
    }

    public Void visitProgram(HMLParser.ProgramContext ctx) {
        System.out.println("Visiting Program... ... ...");
        currentScope = scopes.get(ctx);
        visit(ctx.blockStatement());
        return null;
    }

    public Void visitSeqCom(HMLParser.SeqComContext ctx) {
        System.out.println("Visiting Seq Program... ... ...");
        for (HMLParser.BlockStatementContext bs : ctx.blockStatement())
            visit(bs);
        return null;
    }

    public Void visitAtomPro(HMLParser.AtomProContext ctx) {
        System.out.println("Visiting Atom Program... ... ...");
        visit(ctx.atom());
        return null;
    }

    public Void visitAssignment(HMLParser.AssignmentContext ctx) {
        System.out.println("Visiting Assignment... ... ...");
        currentDynamics.addDiscrete(ctx);
        return null;
    }

    /**
     * 需要加条件不成立的分支
     * @param ctx
     * @return null
     */
    public Void visitLoopPro(HMLParser.LoopProContext ctx) {
        System.out.println("Visiting Loop Program... ... ...");
        currentDynamics.addDiscrete(ctx.parExpression().expr());
        visit(ctx.parStatement().blockStatement());
        return null;
    }

    public Void visitOde(HMLParser.OdeContext ctx) {
        System.out.println("Visiting Ode ... ...");
        currentDynamics.addContinuous(ctx);
        currentDynamics.setDepth(currentDepth++);
        dynamicsList.add(currentDynamics);
        createNewDynamics();
        return null;
    }

    public Void visitCallTem(HMLParser.CallTemContext ctx) {
        System.out.println("Visiting Call Template...");
        StringBuilder key = new StringBuilder();
        key.append(ctx.ID().getText());
        if (ctx.exprList()!=null) {
            List<HMLParser.ExprContext> exprs = ctx.exprList().expr();
            for (HMLParser.ExprContext e : exprs) {
                Symbol s = currentScope.resolve(e.getText());
                key.append(getType(s.getType()));
            }
            Template template = tmpMap.get(key.toString());
            System.out.println(key.toString());
            visit(template.getTemplateContext());
        }

        return null;
    }

    public Void visitTemplate(HMLParser.TemplateContext ctx) {
        visit(ctx.parStatement().blockStatement());
        return null;
    }

    private void createNewDynamics(){
        currentDynamics = new Dynamics();
    }

    public static String getType(Symbol.Type type) {
        if (type.equals(Symbol.Type.Real))   return "float";
        if (type.equals(Symbol.Type.Int))    return "int";
        if (type.equals(Symbol.Type.Bool))  return "boolean";

        return "NULL";
    }
}
