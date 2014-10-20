package IMP.Translate;

import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.HML2SMT;
import IMP.Infos.AbstractExpr;
import IMP.Scope.GlobalScope;
import IMP.Scope.Scope;
import IMP.Scope.Symbol;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the visitor that does the main work for
 * unrolling (translation) from HML model to SMT2 formulas
 * @author fofo (fang.huixing@gmail.com)
 */
public class DynamicalVisitor extends HMLProgram2SMTVisitor {
    private static Logger logger = LogManager.getLogger(DynamicalVisitor.class.getName());
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope
    private static boolean CHECKINGGUARD = false;
    private int depth;
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();
    private VariableLink currentVariableLink;
    private Stack<VariableLink> variableStack = new Stack<VariableLink>();
    private VisitTree currentTree = new VisitTree(null,  new DiscreteWithContinuous(), new ArrayList<Dynamic>());




    /**
     * All the paths that represent running of the model
     * Each path contains (size=depth) dynamics which contains both discrete and continuous behaviors
     */
    private List<List<Dynamic>> paths = new ArrayList<List<Dynamic>>();
    int odenumering = 0;


    public DynamicalVisitor(ParseTreeProperty<Scope> scopes, GlobalScope globals, HashMap<String, Template> tmpMap, int depth) {
        super();
        this.scopes = scopes;
        this.globals = globals;
        this.depth = depth;
        this.tmpMap = tmpMap;
    }

    public Void visitHybridModel(HMLParser.HybridModelContext ctx) {

        currentScope = globals;
        visit(ctx.program());
        return null;
    }

    public Void visitProgram(HMLParser.ProgramContext ctx) {

        currentScope = scopes.get(ctx);
        visit(ctx.blockStatement());


        return null;
    }

    public Void visitSeqCom(HMLParser.SeqComContext ctx) {

        for (HMLParser.BlockStatementContext bs : ctx.blockStatement())
            visit(bs);

        return null;
    }

    public Void visitConChoice(HMLParser.ConChoiceContext ctx) {

        HMLParser.ExprContext condition =  ctx.expr();

        boolean condSatInit = checkChoice(condition, currentTree);

        //条件满足的情况
        if (condSatInit) {
            //currentTree.addDiscrete(new ContextWithVarLink(condition, currentVariableLink));
            visit(ctx.blockStatement(0));
        }
        else {//条件不满足的情况
            //currentTree.addDiscrete(new ContextWithVarLink(condition, currentVariableLink, true));
            visit(ctx.blockStatement(1));
        }
        return null;
    }






    public Void visitAtomPro(HMLParser.AtomProContext ctx) {

        visit(ctx.atom());
        return null;
    }

    public Void visitAssignment(HMLParser.AssignmentContext ctx) {

        currentTree.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));
        return null;
    }

    /**
     * 需要加条件不成立的分支
     * @param ctx 循环
     * @return null
     */
    public Void visitLoopPro(HMLParser.LoopProContext ctx) {
        HMLParser.ExprContext boolCondition = ctx.parExpression().expr();
        if (boolCondition instanceof HMLParser.ConstantTrueContext) {
            while (!isMaxDepth()) {
                visit(ctx.parStatement().blockStatement());
            }
        }
        else if (boolCondition instanceof HMLParser.ConstantFalseContext) return null;
        else {
            HMLParser.ExprContext condition = ctx.parExpression().expr();
            boolean condSatInit = checkChoice(condition, currentTree);
            while (condSatInit && !isMaxDepth()) {
                visit(ctx.parStatement().blockStatement());
                if (isMaxDepth()) return null;
                condSatInit = checkChoice(condition, currentTree);
            }
        }
        return null;
    }



    public Void visitOde(HMLParser.OdeContext ctx) {

        //maybe we don't have to check the guard initially
        if (CHECKINGGUARD) {
            boolean guardSatInit = checkGuard(ctx.guard(), currentTree);
            if (guardSatInit) {
                logger.debug("The Guard is satisfied initially, skip this flow.");
                return null;
            }
        }



        visit(ctx.equation());
        if (currentTree.getCurrentDepth()>depth) return null;

        currentTree.addContinuous(new ContextWithVarLink(ctx, currentVariableLink));
        currentTree.getCurrentDynamics().setGuardCheckEnable(true);
        currentTree.getCurrentDynamics().setDepth(currentTree.getCurrentDepth());
        currentTree.addDynamics(currentTree.getCurrentDynamics());
        currentTree.getCurrentDynamics().toString();
        if (currentTree.getCurrentDepth() < depth+1) {
            Dynamic dy = new DiscreteWithContinuous();
            dy.addDiscrete(new ContextWithVarLink(ctx.guard(), currentVariableLink));
            currentTree.setCurrentDynamics(dy);
        }
        else  finishOnePath(currentTree);

        return null;
    }

    private boolean checkGuard(HMLParser.GuardContext guard, VisitTree visitTree){
        ParseTreeProperty<AbstractExpr> guardPtp = HML2SMT.getGuardPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(guardPtp.get(guard));
        return checkExpr(concreteExpr, visitTree);
    }

    private boolean checkChoice(HMLParser.ExprContext condition, VisitTree visitTree) {
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(exprs.get(condition));
        return checkExpr(concreteExpr, visitTree);
    }

    private boolean checkExpr(ConcreteExpr concreteExpr, VisitTree visitTree) {
        Dynamic curDy = visitTree.getCurrentDynamics();
        List<Dynamic> curDyList = visitTree.getCurrentDynamicList();
        int curDepth = visitTree.getCurrentDepth();
        curDy.setDepth(curDepth);

        String conditionStr =  curDy.getPartialResult(concreteExpr, currentVariableLink);
        logger.debug("Condition: " + conditionStr);

        return HML2SMT.checkTempleFormulas(this, conditionStr, curDepth);
    }


    private void finishOnePath(VisitTree leaf) {
        //paths.add(leaf.getCurrentDynamicList());
        //leaf.delete();//递归地从树中删除已经保存的path，这样可以使树变小，遍历的时候快些
        //System.out.println("Finish one Path" + paths.size());

    }

    //不带初始值的方程
    public Void visitEqWithNoInit(HMLParser.EqWithNoInitContext ctx) {

        return null;
    }

    //带初始值的方程
    public Void visitEqWithInit(HMLParser.EqWithInitContext ctx) {
        currentTree.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink)); //将初值对应为连续变量的值
        return null;
    }

    public Void visitParaEq(HMLParser.ParaEqContext ctx) {

        for (HMLParser.EquationContext e : ctx.equation())  visit(e);
        return null;
    }


    public Void visitCallTem(HMLParser.CallTemContext ctx) {

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
        currentScope = scopes.get(ctx);
        visit(ctx.parStatement().blockStatement());
        return null;
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

    //判定是否已经到达最大深度
    private boolean isMaxDepth(){
       if (currentTree.getCurrentDepth()<depth+1) return false;
       return true;

    }

    public Void visit(ParseTree tree){
        if (isMaxDepth()) {
            return null;
        }
        else return superVisit(tree);
    }

    public Void superVisit(ParseTree tree){
        return super.superVisit(tree);
    }

    private List<Dynamic> copyList(List<Dynamic> from) {
        List<Dynamic> l = new ArrayList<Dynamic>();
        for (Dynamic s : from) {
            l.add(s);
        }
        return l;
    }

    public List<List<Dynamic>> getPaths() {
        List<List<Dynamic>> res  = new ArrayList<List<Dynamic>>();
        res.add(currentTree.getCurrentDynamicList());
        return res;
    }
}