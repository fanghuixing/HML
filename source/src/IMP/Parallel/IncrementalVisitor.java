package IMP.Parallel;


import AntlrGen.HMLLexer;
import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.Exceptions.TemplateNotDefinedException;
import IMP.HML2SMT;
import IMP.Infos.AbstractExpr;
import IMP.Scope.Scope;
import IMP.Scope.Symbol;
import IMP.Translate.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * HML IMP.Parallel
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-6.
 */
public class IncrementalVisitor extends HMLProgram2SMTVisitor implements Runnable{


    private static Logger logger = LogManager.getLogger(IncrementalVisitor.class);
    private static int sleepTime = 1000000;
    private ParserRuleContext currentCtx;
    private ParseTreeProperty<Scope> scopes;
    private Scope currentScope;
    private Stack<VariableLink> variableStack;
    private VariableLink currentVariableLink;
    private VisitTree currentTree;
    private ContextWithVarLink continuous;
    private HashMap<String, Template> tmpMap;
    private Thread mainThread;


    public IncrementalVisitor(ParserRuleContext currentCtx, Scope currentScope, Stack<VariableLink> variableStack, VariableLink currentVariableLink, VisitTree currentTree, HashMap<String, Template> tmpMap, Thread mainThread, ParseTreeProperty<Scope> scopes) {
        this.currentCtx = currentCtx;
        this.currentScope = currentScope;
        this.variableStack = variableStack;
        this.currentVariableLink = currentVariableLink;
        this.currentTree = currentTree;
        this.tmpMap = tmpMap;
        this.mainThread = mainThread;
        this.scopes = scopes;
    }

    public Void visitAtomPro(HMLParser.AtomProContext ctx){
        visit(ctx.atom());
        return null;
    }

    public Void visitAssignment(HMLParser.AssignmentContext ctx) {
        currentTree.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));
        return null;
    }

    public Void visitSendSignal(HMLParser.SendSignalContext ctx) {
        currentTree.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));
        return null;
    }

    public Void visitSuspend(HMLParser.SuspendContext ctx) {
        try {
            Integer time = Integer.valueOf(ctx.time.getText());
            if (time<=0) return null;
        }catch (NumberFormatException e) {
            logger.info("Cannot transfer to integer for suspend time.");
        }
        commonContinuousAnalysis(ctx, ctx);



        //check whether the time is out
        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(exprs.get(ctx));
        boolean res = checkExpr(concreteExpr, currentTree);
        if (!res) {
            ANTLRInputStream input = new ANTLRInputStream(String.format("suspend(%s - %s)", ctx.time.getText(), "clock"));
            HMLLexer lexer = new HMLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            HMLParser parser = new HMLParser(tokens);
            parser.setBuildParseTree(true);
            ParseTree tree = parser.atom();


            HMLParser.ExprContext nexpr = ((HMLParser.SuspendContext) tree).expr();
            exprs.put(nexpr, new AbstractExpr("-", AbstractExpr.Sort.NVAR, exprs.get(ctx.expr()), new AbstractExpr("clock_" + (currentTree.getCurrentDepth()-1) + "_t", AbstractExpr.Sort.CONSTANT)));
            //add the new suspend guard
            exprs.put(tree, new AbstractExpr(">=", new AbstractExpr("clock", AbstractExpr.Sort.VAR), exprs.get(nexpr)));
            visit(tree);
        }
       else{
            currentTree.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));
        }
        return null;
    }










    /**
     * Add continuous statements and its exit condition into dynamics objects
     * @param ctx   The parse rule context
     * @param guard The exit condition
     */
    private void commonContinuousAnalysis(ParserRuleContext ctx, ParserRuleContext guard){

        if (guard instanceof HMLParser.GuardedChoiceContext) {
            List<HMLParser.SingleGuardedChoiceContext> gcList;
            gcList = ((HMLParser.GuardedChoiceContext) guard).singleGuardedChoice();
            for (HMLParser.SingleGuardedChoiceContext sgc : gcList) {
                //boolean sat = checkGuard(sgc.guard(), currentTree);
                currentTree.getCurrentDynamics().setGuardCheckEnable(true);
                if (checkGuard(sgc.guard(), currentTree)) {
                    // if one of the guardedChoice is satisfiable at the begging
                    visit(sgc.blockStatement());
                    return; // do not forget this return
                }
            }
            continuous = new ContextWithVarLink(ctx, currentVariableLink);
            finish();
            visit(ctx);
        } else {
            // if suspend
            //set continuous
            continuous = new ContextWithVarLink(ctx, currentVariableLink);
            finish();
        }
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        visit(currentCtx);
        mainThread.interrupt();
    }

    private void finish(){
        // interrupt the main thread
        mainThread.interrupt();
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                logger.info(e.getMessage());
                return;
            }
        }
    }

    private boolean checkGuard(HMLParser.GuardContext guard, VisitTree visitTree){
        ParseTreeProperty<AbstractExpr> guardPtp = HML2SMT.getGuardPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(guardPtp.get(guard));
        return checkExpr(concreteExpr, visitTree);
    }

    private boolean checkExpr(ConcreteExpr concreteExpr, VisitTree visitTree) {
        Dynamic curDy = visitTree.getCurrentDynamics();
        int curDepth = visitTree.getCurrentDepth();
        curDy.setDepth(curDepth);
        String conditionStr =  curDy.getPartialResult(concreteExpr, currentVariableLink);
        logger.debug("Condition: " + conditionStr);
        return HML2SMT.checkTemporaryFormulas(this, conditionStr, curDepth);
    }



    @Override
    public Void visitSeqCom(@NotNull HMLParser.SeqComContext ctx) {

        for (HMLParser.BlockStatementContext bs : ctx.blockStatement())
            visit(bs);

        return null;
    }

    @Override
    public Void visitConChoice(@NotNull HMLParser.ConChoiceContext ctx) {
        HMLParser.ExprContext condition =  ctx.expr();
        boolean condSatInit;

        currentTree.getCurrentDynamics().setGuardCheckEnable(true);
        condSatInit = checkChoice(condition, currentTree);


        if (condSatInit) {
            //currentTree.addDiscrete(new ContextWithVarLink(condition, currentVariableLink));
            visit(ctx.blockStatement(0));
        }
        else {
            //currentTree.addDiscrete(new ContextWithVarLink(condition, currentVariableLink, true));
            visit(ctx.blockStatement(1));
        }
        return null;
    }

    private boolean checkChoice(HMLParser.ExprContext condition, VisitTree visitTree) {
        while (condition instanceof HMLParser.ParExprContext) {
            condition = ((HMLParser.ParExprContext) condition).parExpression().expr();
        }

        boolean needReverse = false;
        while (condition instanceof HMLParser.NegationExprContext) {
            condition = ((HMLParser.NegationExprContext) condition).expr();
            needReverse = !needReverse;
        }

        ParseTreeProperty<AbstractExpr> exprs = HML2SMT.getExprPtp();
        ConcreteExpr concreteExpr = new ConcreteExpr(exprs.get(condition));
        // If the inner part is satisfied, we chose the right branch, so we do the negation (!)
        boolean res = checkExpr(concreteExpr, visitTree);
        if (needReverse) return !res;
        else return res;
    }

    @Override
    public Void visitOde(@NotNull HMLParser.OdeContext ctx) {
        visit(ctx.equation());

        currentTree.getCurrentDynamics().setGuardCheckEnable(true);
        boolean guardSatInit = checkGuard(ctx.guard(), currentTree);
        if (guardSatInit) {
            currentTree.addDiscrete(new ContextWithVarLink(ctx.guard(), currentVariableLink));
            logger.debug("The Guard is satisfied initially, skip this flow.");
            return null;
        }


        continuous = new ContextWithVarLink(ctx, currentVariableLink);
        finish();
        visit(ctx);
        return null;
    }

    //eq with initialization
    public Void visitEqWithInit(HMLParser.EqWithInitContext ctx) {
        currentTree.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink)); //将初值对应为连续变量的值
        return null;
    }

    public Void visitParaEq(HMLParser.ParaEqContext ctx) {

        for (HMLParser.EquationContext e : ctx.equation())  visit(e);
        return null;
    }

    @Override
    public Void visitWhenPro(@NotNull HMLParser.WhenProContext ctx) {
        // for sequential program we can check the guard at the beginning
        commonContinuousAnalysis(ctx, ctx.guardedChoice());
        return null;
    }

    @Override
    public Void visitLoopPro(@NotNull HMLParser.LoopProContext ctx) {
        HMLParser.ExprContext boolCondition = ctx.parExpression().expr();
        if (boolCondition instanceof HMLParser.ConstantTrueContext) {
            while (true) {
                visit(ctx.parStatement().blockStatement());
            }
        }
        else if (boolCondition instanceof HMLParser.ConstantFalseContext) return null;
        else {
            HMLParser.ExprContext condition = ctx.parExpression().expr();

            boolean condSatInit = checkChoice(condition, currentTree);
            while (condSatInit) {
                visit(ctx.parStatement().blockStatement());
                condSatInit = checkChoice(condition, currentTree);
            }
        }
        return null;
    }

    @Override
    public Void visitCallTem(@NotNull HMLParser.CallTemContext ctx) {
        StringBuilder key = new StringBuilder();
        List<String> cvars = new ArrayList<String>(); // concrete vars
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
            if (template == null) {
                String msg = "No template defined for " + ctx.getText();
                logger.error(msg);
                throw new TemplateNotDefinedException(msg);
            }


            List<String> fvars = template.getFormalVarNames(); //formal vars

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

    @Override
    public Void visitParPro(@NotNull HMLParser.ParProContext ctx) {
        visit(ctx.parStatement().blockStatement());
        return null;
    }


    public Void visitTemplate(HMLParser.TemplateContext ctx) {
        currentScope = scopes.get(ctx);
        visit(ctx.parStatement().blockStatement());
        return null;
    }

    @Override
    public Void visit(@NotNull ParseTree tree) {
        return super.visit(tree);
    }

    public ContextWithVarLink getContinuous() {
        ContextWithVarLink res = continuous;
        continuous = null;
        return res;
    }









    public List<List<Dynamic>> getPaths() {
        List<List<Dynamic>> res  = new ArrayList<List<Dynamic>>();
        res.add(currentTree.getCurrentDynamicList());
        return res;
    }

}
