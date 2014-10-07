package IMP.Translate;

import AntlrGen.HMLBaseVisitor;
import AntlrGen.HMLParser;
import IMP.Basic.Template;
import IMP.Scope.GlobalScope;
import IMP.Scope.Scope;
import IMP.Scope.Symbol;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import java.util.*;

/**
 * This is the visitor that does the main work for
 * unrolling (translation) from HML model to SMT2 formulas
 * @author fofo (fang.huixing@gmail.com)
 */
public class HMLProgram2SMTVisitor extends HMLBaseVisitor<Void> {
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





    public HMLProgram2SMTVisitor(ParseTreeProperty<Scope> scopes, GlobalScope globals, HashMap<String, Template> tmpMap, int depth) {
        this.scopes = scopes;
        this.globals = globals;
        this.depth = depth;
        this.tmpMap = tmpMap;
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
        System.out.println("End visit of Program ... ...");
        return null;
    }

    public Void visitSeqCom(HMLParser.SeqComContext ctx) {
        System.out.println("Visiting Seq Program... ... ...");
        for (HMLParser.BlockStatementContext bs : ctx.blockStatement())
            visit(bs);
        System.out.println("End visit of Seq Program ... ...");
        return null;
    }

    public Void visitConChoice(HMLParser.ConChoiceContext ctx) {
        System.out.println("Visiting Conditional Choice....." + ctx.getText());
        HMLParser.ExprContext condition =  ctx.expr();
        //如果可以判定这个条件当前的值，就可以极大地降低分支数目
        System.out.println(condition.getText());

        List<VisitTree> leaves = new ArrayList<VisitTree>();
        root.collectLeaves(leaves);
        VisitTree oldRoot = root;
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
        System.out.println("Visiting Atom Program... ... ...");
        visit(ctx.atom());
        return null;
    }

    public Void visitAssignment(HMLParser.AssignmentContext ctx) {
        System.out.println("Visiting Assignment... ... ...");
        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();
        root.collectLeaves(dynamicsLeaves);
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            System.out.println(ctx.getText());
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

        System.out.println("Visiting Loop Program... ... ...");
        HMLParser.ExprContext boolCondition = ctx.parExpression().expr();
        if (boolCondition instanceof HMLParser.ConstantTrueContext) {
            while (!isMaxDepth()) {
                visit(ctx.parStatement().blockStatement());
            }
        }
        else if (boolCondition instanceof HMLParser.ConstantFalseContext) {
            return null;
        }

        System.out.println("End visit Loop...");
        return null;
    }



    public Void visitOde(HMLParser.OdeContext ctx) {
        System.out.println("Visiting Ode ... ...");
        visit(ctx.equation());


        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();

        root.collectLeaves(dynamicsLeaves);
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            System.out.println("\n");
            System.out.println("THE DEPTH OF THE LEAF IS : "+ leaf.getCurrentDepth() + " " + depth + ", the dynamic list size: " + leaf.getCurrentDynamicList().size());
            Dynamic dynamic = leaf.getCurrentDynamics();
            dynamic.addContinuous(new ContextWithVarLink(ctx, currentVariableLink));
            dynamic.setDepth(leaf.getCurrentDepth());
            leaf.getCurrentDynamicList().add(dynamic);
            System.out.println("THE DEPTH OF THE LEAF IS : "+ leaf.getCurrentDepth() + " " + depth + ", the dynamic list size: " + leaf.getCurrentDynamicList().size());
            System.out.println("\n");
            if (leaf.getCurrentDepth() < depth+1) {
                Dynamic dy = new DiscreteWithContinuous();
                dy.addDiscrete(new ContextWithVarLink(ctx.guard(), currentVariableLink));
                leaf.setCurrentDynamics(dy);
            }
            else  finishOnePath(leaf);
        }






        return null;
    }


    private void finishOnePath(VisitTree leaf) {
        System.out.println("Finishing one Path of Unrolling.....");
        paths.add(leaf.getCurrentDynamicList());
        leaf.delete();//递归地从树中删除已经保存的path，这样可以使树变小，遍历的时候快些


    }

    //不带初始值的方程
    public Void visitEqWithNoInit(HMLParser.EqWithNoInitContext ctx) {
        System.out.println("Visiting Ode without init ... ...");
        return null;
    }

    //带初始值的方程
    public Void visitEqWithInit(HMLParser.EqWithInitContext ctx) {
        System.out.println("Visiting Ode with init ... ...");
        List<VisitTree> dynamicsLeaves = new ArrayList<VisitTree>();
        root.collectLeaves(dynamicsLeaves);
        //如果已经到达最大深度，就在树中删除该节点路径
        for (VisitTree leaf : dynamicsLeaves) {
            leaf.getCurrentDynamics().addDiscrete(new ContextWithVarLink(ctx, currentVariableLink)); //将初值对应为连续变量的值
        }

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

        List<VisitTree> vt = new ArrayList<VisitTree>();
        visitTree.collectLeaves(vt);
        for (VisitTree v : vt){
            //对每一个叶子判断是否完成了深度展开，如果有一个叶子没有达到则需要继续展开
            if (v.getCurrentDynamicList().size()<depth+1)
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
        return paths;
    }
}
