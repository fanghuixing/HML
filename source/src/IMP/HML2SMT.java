package IMP;

import IMP.Basic.Constraint;
import IMP.Basic.VariableForSMT2;
import IMP.Check.ExecSMT;
import IMP.Infos.HML2SMTListener;
import IMP.Infos.HSTErrorListener;
import IMP.Merge.PathsMerge;
import IMP.Scope.ScopeConstructor;
import IMP.Infos.AbstractExpr;
import IMP.Translate.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.PathMatcher;
import java.util.*;


import AntlrGen.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 */
public class HML2SMT {
    private static Logger  logger = LogManager.getLogger(HML2SMT.class.getName());
    final static int depth = 20;
    private static String modelPath = "./source/src/bouncingBall.hml";
    //若为true则选择基于SMT判定的深度优先展开，若为false则选择全展开的方式
    private static boolean deepApproach = true;
    static ParseTreeProperty<AbstractExpr> exprPtp;
    static ParseTreeProperty<AbstractExpr> guardPtp;
    static HashMap<String, AbstractExpr>  InitID2ExpMap;
    static List<VariableForSMT2> varlist;
    private static HML2SMTListener hml2SMTListener = new HML2SMTListener();
    private static ParseTree  tree;
    private static STGroup group = new STGroupFile("HML.stg");
    private static ST st = group.getInstanceOf("SMT2");

    public static void main(String[] args) throws Exception {
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
        InputStream inputStream = System.in;
        if ( inputFile!=null ) inputStream = new FileInputStream(inputFile);
        inputStream = new FileInputStream(modelPath);
        ANTLRInputStream input = new ANTLRInputStream(inputStream);
        HMLLexer lexer = new HMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HMLParser parser = new HMLParser(tokens);
        parser.setBuildParseTree(true);
        tree = parser.hybridModel();
        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(hml2SMTListener, tree);
        exprPtp = hml2SMTListener.getExprPtp();
        guardPtp = hml2SMTListener.getGuardPtp();
        ScopeConstructor scl = new ScopeConstructor();
        walker.walk(scl, tree);

        varlist = hml2SMTListener.getVarlist();
        HMLProgram2SMTVisitor trans;
        if (!deepApproach)
            trans = new HMLProgram2SMTVisitor(scl.getScopes(),scl.getGlobals(), hml2SMTListener.getTmpMap(), depth);
        else
            trans = new DynamicalVisitor(scl.getScopes(),scl.getGlobals(), hml2SMTListener.getTmpMap(), depth);

        constructComponents(trans);
        writeFormulas(trans);

        //PathsMerge PM = new PathsMerge();
        //PM.mergePaths(paths);
        //PM.getMergeResult();
    }

    private static void writeFormulas(HMLProgram2SMTVisitor trans) throws IOException{
        List<List<Dynamic>> paths = trans.getPaths();
        int pathId = 0;
        for (List<Dynamic> onePath : paths) {
            prepareMainFormulas(st, onePath);
            writeToFile(st, pathId++, "./source/src/HML_");
            st.remove("formulas");
        }
    }

    public static boolean checkTempleFormulas(HMLProgram2SMTVisitor trans, String startPoint, int depth){
        addFlowsInSt(depth);
        addVarsToSMT(depth);
        prepareMainFormulas(st, trans.getPaths().get(0));
        st.add("formulas", startPoint);
        boolean res = false;
        try {
            res = writeToFile(st, System.currentTimeMillis(), "./source/src/dynamicChecking/HML_");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        st.remove("formulas");
        removeFlowsInSt();
        removeVarsInSMT();
        return res;
    }

    private static void prepareMainFormulas(ST st, List<Dynamic> onePath){
        for (Dynamic dy : onePath) {
            st.add("formulas", dy.getDiscreteDynamics());
            st.add("formulas", dy.getContinuousDynamics());
            st.add("formulas", "\n");
        }
    }


    public  static boolean writeToFile(ST st, long pathId, String prefix) throws IOException {
        File out = new File(prefix + depth + "_" + pathId + ".smt2");
        if (out.createNewFile())  logger.debug("File successfully created");
        else                      logger.debug("File already exits.");
        st.write(out, new HSTErrorListener());
        return ExecSMT.exec("0.0001", out.getPath());
    }


    public static List getVarListforSMT2(String prefix, String type, int depth){
        List<VariableForSMT2> list = new ArrayList<VariableForSMT2>();
        for (int i=0; i<= depth; i++) {
            list.add(new VariableForSMT2(prefix + "_" + i + "_0", type));
            list.add(new VariableForSMT2(prefix + "_" + i + "_t", type));
        }
        return list;
    }

    public static List getTimeOrModeVarListforSMT2(String prefix, String type, int depth){
        List<VariableForSMT2> list = new ArrayList<VariableForSMT2>();
        for (int i=0; i<= depth; i++) {
            list.add(new VariableForSMT2(prefix + "_" + i, type));
        }
        return list;
    }


    public static ParseTreeProperty<AbstractExpr> getExprPtp() {
        return exprPtp;
    }

    public static HashMap<String, AbstractExpr> getInitID2ExpMap() {
        return InitID2ExpMap;
    }

    public static ParseTreeProperty<AbstractExpr> getGuardPtp() {
        return guardPtp;
    }

    /**
     * 该值由HML2SMTListener生成
     * @return 变量列表
     */
    public static List<VariableForSMT2> getVarlist() {
        return varlist;
    }


    public static void constructComponents(HMLProgram2SMTVisitor trans){

        hml2SMTListener.getInitializations();

        InitID2ExpMap = hml2SMTListener.getInitID2ExpMap();
        //HMLProgram2SMTVisitor trans = new HMLProgram2SMTVisitor(scl.getScopes(),scl.getGlobals(), hml2SMTListener.getTmpMap(), depth);
        trans.setCurrentVariableLink(hml2SMTListener.getFinalVariableLinks());
        trans.visit(tree);
        addVarsToSMT(depth);
        addFlowsInSt(depth);
    }

    private static void addVarsToSMT(int depth) {
        st.add("vars", varlist);
        for (VariableForSMT2 v : varlist) {
            st.add("uvars", getVarListforSMT2(v.name, v.type, depth));
        }
        st.add("tvars", getTimeOrModeVarListforSMT2("time", "Real", depth));
        st.add("mvars", getTimeOrModeVarListforSMT2("mode", "Int", depth));
    }

    private static void removeVarsInSMT() {
        st.remove("vars");
        st.remove("uvars");
        st.remove("tvars");
        st.remove("mvars");
    }

    public static void addFlowsInSt(int depth){
        for (Map.Entry<Integer,String> ode : DiscreteWithContinuous.getOdeMap().entrySet()) {
            logger.trace(ode.getValue());
            st.add("flows", ode.getValue());
        }

        List<Constraint> cons = hml2SMTListener.getConstraintsList();
        for (Constraint c : cons) {
            st.add("constraints", c.getNormalConstraintList(depth));
        }
        int modeNum = DiscreteWithContinuous.getOdeMap().size();
        String leftEnd = "1";
        if (modeNum==0) leftEnd = "0";
        st.add("constraints", new Constraint("mode", leftEnd, modeNum + "").getNormalConstraintList(depth));
    }

    public static void removeFlowsInSt(){
        st.remove("flows");
        st.remove("constraints");
    }

    
    /*
    //获取符号xpath的子树，且子树的类型由Class c指定
    public static  void getMaths(ParseTree tree, String xpath, HMLParser parser, List<ParseTree> trees, Class c){
        Collection<ParseTree> col = XPath.findAll(tree, xpath, parser);
        for (ParseTree t : col) {
            // 只加入Class c类型的对象
            if ( c.isInstance(t)) trees.add(t);
        }
    }
    */

}
