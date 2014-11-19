package IMP.Translate;

import AntlrGen.HMLBaseVisitor;
import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.Exceptions.TemplateNotDefinedException;
import IMP.Scope.GlobalScope;
import IMP.Scope.Scope;
import IMP.Scope.Symbol;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * This is the visitor that does the main work for
 * unrolling (translation) from HML model to SMT2 formulas
 * @author fofo (fang.huixing@gmail.com)
 */
public class HMLProgram2SMTVisitor extends HMLBaseVisitor<Void> {
    private static Logger logger = LogManager.getLogger(HMLProgram2SMTVisitor.class.getName());
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope

    private int depth;
    private HashMap<String, Template> tmpMap = new HashMap<String, Template>();
    private VariableLink currentVariableLink;
    private Stack<VariableLink> variableStack = new Stack<VariableLink>();
    private VisitTree root = new VisitTree(null,  new DiscreteWithContinuous(), new ArrayList<Dynamic>());
    private VisitTree visitTree = root;



    /**
     * All the paths that represent running of the model
     * Each path contains (size=depth) dynamics which contains both discrete and continuous behaviors
     */
    private List<List<Dynamic>> paths = new ArrayList<List<Dynamic>>();
    int odenumering = 0;

    public HMLProgram2SMTVisitor() {
    }

    public HMLProgram2SMTVisitor(ParseTreeProperty<Scope> scopes, GlobalScope globals, HashMap<String, Template> tmpMap, int depth) {
        this.scopes = scopes;
        this.globals = globals;
        this.depth = depth;
        this.tmpMap = tmpMap;
    }

    public Void visitSuspend(HMLParser.SuspendContext ctx) {
        logger.debug("Visit Suspend " + ctx.getText());
        try {
            Integer time = Integer.valueOf(ctx.time.getText());
            if (time<=0) return null;
        }catch (NumberFormatException e) {

        }

        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();

        root.collectLeaves(dynamicsLeaves);
        //logger.debug("visit suspend, leaf size: " + dynamicsLeaves.size());
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            //当leaf的深度达到depth+1时停止
            if (leaf.getCurrentDepth()>depth) continue;
            Dynamic dynamic = leaf.getCurrentDynamics();
            dynamic.addContinuous(new ContextWithVarLink(ctx, currentVariableLink));
            dynamic.setDepth(leaf.getCurrentDepth());
            leaf.getCurrentDynamicList().add(dynamic);
            dynamic.toString();
            if (leaf.getCurrentDepth() < depth+1) {
                Dynamic dy = new DiscreteWithContinuous();
                dy.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));
                leaf.setCurrentDynamics(dy);
            }
            else  finishOnePath(leaf);
        }

        root.merge();
        return null;

    }


    public Void visitWhenPro(HMLParser.WhenProContext ctx) {
        //logger.debug("visit when pro ");
        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();

        root.collectLeaves(dynamicsLeaves);

        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            //当leaf的深度达到depth+1时停止
            if (leaf.getCurrentDepth()>depth) continue;

            Dynamic leftDynamic = leaf.getCurrentDynamics().copy();
            Dynamic rightDynamic = leaf.getCurrentDynamics().copy();
            List<Dynamic> leftList = copyList(leaf.getCurrentDynamicList());
            List<Dynamic> righList = copyList(leaf.getCurrentDynamicList());
            VisitTree leftTree = new VisitTree(leaf,leftDynamic, leftList);
            VisitTree rightTree = new VisitTree(leaf,rightDynamic, righList);
            //创建分支也需要在树根节点创建， 这里需要修改
            leaf.addChild(leftTree);
            leaf.addChild(rightTree);

            //for left tree, that the guard is invalid
            leftDynamic.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink, true));
            leftDynamic.addContinuous(new ContextWithVarLink(ctx, currentVariableLink));
            leftDynamic.setDepth(leftTree.getCurrentDepth());
            leftTree.getCurrentDynamicList().add(leftDynamic);
            leftDynamic.toString();
            if (leftTree.getCurrentDepth() < depth+1) {
                Dynamic dy = new DiscreteWithContinuous();
                //dy.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));
                leftTree.setCurrentDynamics(dy);
            }
            else  finishOnePath(leftTree);

            //for right tree, that the guard is valid at the begging
            rightDynamic.addDiscrete(new ContextWithVarLink(ctx, currentVariableLink));

        }

        root.merge();

        //如果可以判定这个条件当前的值，就可以极大地降低分支数目
        List<VisitTree> leaves = new ArrayList<VisitTree>();
        root.collectLeaves(leaves);
        VisitTree oldRoot = root;

        List<HMLParser.SingleGuardedChoiceContext> gcList = ctx.guardedChoice().singleGuardedChoice();
        for (VisitTree v : leaves) {
            if (v.getCurrentDepth() < depth+1) {
                for (HMLParser.SingleGuardedChoiceContext sgc : gcList) {
                    Dynamic dynamic = v.getCurrentDynamics().copy();
                    List<Dynamic> dynamicList = copyList(v.getCurrentDynamicList());
                    VisitTree tree = new VisitTree(v, dynamic, dynamicList);
                    v.addChild(tree);
                    root = tree;
                    logger.debug(sgc.guard().getText());
                    dynamic.addDiscrete(new ContextWithVarLink(sgc.guard(), currentVariableLink));
                    visit(sgc.blockStatement());
                    root = oldRoot;//需要指向原来的叶子节点
                }
            }
        }

        root.merge();


        return null;
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
        //logger.debug("visit conditional choice : " + ctx.getText());
        HMLParser.ExprContext condition =  ctx.expr();

        //如果可以判定这个条件当前的值，就可以极大地降低分支数目
        List<VisitTree> leaves = new ArrayList<VisitTree>();
        root.collectLeaves(leaves);
        VisitTree oldRoot = root;
        //logger.debug("visitConChoice, leaves size: " + leaves.size());
        for (VisitTree v : leaves) {
            Dynamic leftDynamic = v.getCurrentDynamics().copy();
            Dynamic rightDynamic = v.getCurrentDynamics().copy();
            List<Dynamic> leftList = copyList(v.getCurrentDynamicList());
            List<Dynamic> righList = copyList(v.getCurrentDynamicList());
            VisitTree leftTree = new VisitTree(v,leftDynamic, leftList);
            VisitTree rightTree = new VisitTree(v,rightDynamic, righList);
            //创建分支也需要在树根节点创建， 这里需要修改
            v.addChild(leftTree);
            v.addChild(rightTree);
            root = leftTree;
            leftDynamic.addDiscrete(new ContextWithVarLink(condition, currentVariableLink));
            visit(ctx.blockStatement(0));
            root = rightTree;
            rightDynamic.addDiscrete(new ContextWithVarLink(condition, currentVariableLink, true));
            visit(ctx.blockStatement(1));

        }
        root = oldRoot;//需要指向原来的叶子节点

        return null;
    }






    public Void visitAtomPro(HMLParser.AtomProContext ctx) {

        visit(ctx.atom());
        return null;
    }

    public Void visitAssignment(HMLParser.AssignmentContext ctx) {

        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();
        root.collectLeaves(dynamicsLeaves);
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {

            leaf.getCurrentDynamics().addDiscrete(new ContextWithVarLink(ctx,currentVariableLink));
        }

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
        else if (boolCondition instanceof HMLParser.ConstantFalseContext) {
            return null;
        } else {
            //如果可以判定这个条件当前的值，就可以极大地降低分支数目
            List<VisitTree> leaves = new ArrayList<VisitTree>();
            root.collectLeaves(leaves);
            VisitTree oldRoot = root;
            //logger.debug("visitConChoice, leaves size: " + leaves.size());
            for (VisitTree v : leaves) {
                Dynamic leftDynamic = v.getCurrentDynamics().copy();
                Dynamic rightDynamic = v.getCurrentDynamics().copy();
                List<Dynamic> leftList = copyList(v.getCurrentDynamicList());
                List<Dynamic> righList = copyList(v.getCurrentDynamicList());
                VisitTree leftTree = new VisitTree(v,leftDynamic, leftList);
                VisitTree rightTree = new VisitTree(v,rightDynamic, righList);
                //创建分支也需要在树根节点创建， 这里需要修改
                v.addChild(leftTree);
                v.addChild(rightTree);
                root = leftTree;
                leftDynamic.addDiscrete(new ContextWithVarLink(boolCondition, currentVariableLink));
                visit(ctx.parStatement().blockStatement());
                if (!isMaxDepth(leftTree)) visit(ctx);
                root = rightTree;
                rightDynamic.addDiscrete(new ContextWithVarLink(boolCondition, currentVariableLink, true));
            }
            root = oldRoot;//需要指向原来的叶子节点
        }
        return null;
    }





    public Void visitSendSignal(HMLParser.SendSignalContext ctx) {
        logger.debug(String.format("Visit Send Signal %s -> %s ",  ctx.getText(), currentVariableLink.getRealVar(ctx.signal().ID().getText())));
        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();
        root.collectLeaves(dynamicsLeaves);
        for (VisitTree leaf : dynamicsLeaves) {

            leaf.getCurrentDynamics().addDiscrete(new ContextWithVarLink(ctx,currentVariableLink));
        }
        return null;
    }



    public Void visitOde(HMLParser.OdeContext ctx) {

        visit(ctx.equation());
        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();

        root.collectLeaves(dynamicsLeaves);
        //logger.debug("visit ode, leaf size:" + dynamicsLeaves.size());
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            //当leaf的深度达到depth+1时停止
            if (leaf.getCurrentDepth()>depth) continue;
            Dynamic dynamic = leaf.getCurrentDynamics();
            dynamic.addContinuous(new ContextWithVarLink(ctx, currentVariableLink));
            dynamic.setDepth(leaf.getCurrentDepth());
            leaf.getCurrentDynamicList().add(dynamic);
            dynamic.toString();
            if (leaf.getCurrentDepth() < depth+1) {
                Dynamic dy = new DiscreteWithContinuous();
                dy.addDiscrete(new ContextWithVarLink(ctx.guard(), currentVariableLink));
                leaf.setCurrentDynamics(dy);
            }
            else  finishOnePath(leaf);
        }

        root.merge();



        return null;
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

        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();
        root.collectLeaves(dynamicsLeaves);
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            leaf.getCurrentDynamics().addDiscrete(new ContextWithVarLink(ctx, currentVariableLink)); //将初值对应为连续变量的值
        }

        return null;
    }

    public Void visitParaEq(HMLParser.ParaEqContext ctx) {

        for (HMLParser.EquationContext e : ctx.equation())  visit(e);
        return null;
    }


    public Void visitCallTem(HMLParser.CallTemContext ctx) {
        //logger.debug("visit call template :" + ctx.getText());

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
        }
            Template template = tmpMap.get(key.toString());

            if (template == null) {
                String msg = "No template defined for " + ctx.getText();
                logger.error(msg);
                throw new TemplateNotDefinedException(msg);
            }
            List<String> fvars = template.getFormalVarNames();


            variableStack.push(currentVariableLink);
            VariableLink vlk = new VariableLink(currentVariableLink);
            int i = 0;
            for (String fv : fvars) {
                vlk.setRealVar(fv, getRealVarName(cvars.get(i)));
                i++;
            }
            currentVariableLink = vlk;
            Scope oldScope = currentScope;
            visit(template.getTemplateContext());
            currentVariableLink = variableStack.pop();
            currentScope = oldScope;
        

        return null;
    }

    public String getRealVarName(String virtualName) {
        if (currentVariableLink==null) return virtualName;
        return currentVariableLink.getRealVar(virtualName);
    }

    public Void visitTemplate(HMLParser.TemplateContext ctx) {
        //logger.debug("visit template : "  + ctx.getText());

        currentScope = scopes.get(ctx);
        visit(ctx.parStatement().blockStatement());
        return null;
    }



    public static String getType(Symbol.Type type) {
        if (type.equals(Symbol.Type.Real))   return "float";
        if (type.equals(Symbol.Type.Int))    return "int";
        if (type.equals(Symbol.Type.Bool))  return "boolean";
        if (type.equals(Symbol.Type.Signal)) return "Signal";
        return "NULL";
    }

    public void setCurrentVariableLink(VariableLink currentVariableLink) {
        this.currentVariableLink = currentVariableLink;
    }

    //判定是否已经到达最大深度
    private boolean isMaxDepth(){

        List<VisitTree> vt = new ArrayList<VisitTree>();
        visitTree.collectLeaves(vt);
        for (VisitTree v : vt){
            //对每一个叶子判断是否完成了深度展开，如果有一个叶子没有达到则需要继续展开
            if (v.getCurrentDepth()<depth+1)
                return false;
        }
        return true;

    }


    //判定是否已经到达最大深度
    private boolean isMaxDepth(VisitTree tree){

        List<VisitTree> vt = new ArrayList<VisitTree>();
        tree.collectLeaves(vt);
        for (VisitTree v : vt){
            //对每一个叶子判断是否完成了深度展开，如果有一个叶子没有达到则需要继续展开
            if (v.getCurrentDepth()<depth+1)
                return false;
        }
        return true;

    }

    public Void visit(org.antlr.v4.runtime.tree.ParseTree tree){
        if (isMaxDepth()) {
            return null;
        }
        else return superVisit(tree);
    }

    public Void superVisit(org.antlr.v4.runtime.tree.ParseTree tree){
        return super.visit(tree);
    }

    private List<Dynamic> copyList(List<Dynamic> from) {
        List<Dynamic> l = new ArrayList<Dynamic>();
        for (Dynamic s : from) {
            l.add(s);
        }
        return l;
    }

    public List<List<Dynamic>> getPaths() {
        List<VisitTree> leaves = new ArrayList<VisitTree>();
        visitTree.collectLeaves(leaves);
        logger.debug("getPaths, leaf size: " + leaves.size());
        for (VisitTree v : leaves) {
            paths.add(v.getCurrentDynamicList());
        }
        return paths;
    }
}
