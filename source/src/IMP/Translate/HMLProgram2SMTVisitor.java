package IMP.Translate;

import AntlrGen.HMLBaseVisitor;
import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.Basic.Variable;
import IMP.Scope.GlobalScope;
import IMP.Scope.Scope;
import IMP.Scope.Symbol;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.*;

/**
 * Created by fofo on 2014/9/30.
 */
public class HMLProgram2SMTVisitor extends HMLBaseVisitor<Void> {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope
    private int currentDepth;
    private int depth;
    private List<Dynamics> dynamicsList = new ArrayList<Dynamics>();
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();
    private VariableLink currentVariableLink;
    private Stack<VariableLink> variableStack = new Stack<VariableLink>();


    Dynamics currentDynamics = new Dynamics();


    public HMLProgram2SMTVisitor(ParseTreeProperty<Scope> scopes, GlobalScope globals, HashMap<String, Template> tmpMap, int depth) {
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
        currentDynamics.addDiscrete(new ContextWithVarLink(ctx,currentVariableLink));
        return null;
    }

    /**
     * 需要加条件不成立的分支
     * @param ctx 循环
     * @return null
     */
    public Void visitLoopPro(HMLParser.LoopProContext ctx) {
        System.out.println("Visiting Loop Program... ... ...");
        currentDynamics.addDiscrete(new ContextWithVarLink(ctx.parExpression().expr(),currentVariableLink));
        visit(ctx.parStatement().blockStatement());
        return null;
    }

    public Void visitOde(HMLParser.OdeContext ctx) {
        System.out.println("Visiting Ode ... ...");
        visit(ctx.equation());
        currentDynamics.addContinuous(new ContextWithVarLink(ctx, currentVariableLink));
        currentDynamics.setDepth(currentDepth++);
        dynamicsList.add(currentDynamics);
        createNewDynamics();

        //add guard into the discrete part
        currentDynamics.addDiscrete(new ContextWithVarLink(ctx.guard(), currentVariableLink));
        return null;
    }




    //不带初始值的方程
    public Void visitEqWithNoInit(HMLParser.EqWithNoInitContext ctx) {
        System.out.println("Visiting Ode without init ... ...");
        return null;
    }

    //带初始值的方程
    public Void visitEqWithInit(HMLParser.EqWithInitContext ctx) {
        System.out.println("Visiting Ode with init ... ...");
        currentDynamics.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink)); //将初值对应为连续变量的值
        return null;
    }

    public Void visitParaEq(HMLParser.ParaEqContext ctx) {
        System.out.println("Visiting Para Ode  ... ...");
        for (HMLParser.EquationContext e : ctx.equation())  visit(e);
        return null;
    }


    public Void visitCallTem(HMLParser.CallTemContext ctx) {
        System.out.println("Visiting Call Template...");
        StringBuilder key = new StringBuilder();
        List<String> cvars = new ArrayList<String>();
        key.append(ctx.ID().getText());
        if (ctx.exprList()!=null) {
            List<HMLParser.ExprContext> exprs = ctx.exprList().expr();
            for (HMLParser.ExprContext e : exprs) {
                //模板调用时候传入的参数类型
                Symbol s = currentScope.resolve(e.getText());
                cvars.add(e.getText());
                key.append(getType(s.getType()));
            }
            Template template = tmpMap.get(key.toString());
            System.out.println(key.toString());

            List<String> fvars = template.getFormalVarNames();

            variableStack.push(currentVariableLink);
            VariableLink vlk = new VariableLink(currentVariableLink);
            int i = 0;
            for (String fv : fvars) {
                vlk.setRealVar(fv, getRealVarName(cvars.get(i)));
                i++;
            }
            currentVariableLink = vlk;
            visit(template.getTemplateContext());
            currentVariableLink = variableStack.pop();
        }

        return null;
    }

    public String getRealVarName(String virtualName) {
        if (currentVariableLink==null) return virtualName;
        return currentVariableLink.getRealVar(virtualName);
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

    public void setCurrentVariableLink(VariableLink currentVariableLink) {
        this.currentVariableLink = currentVariableLink;
    }
}
