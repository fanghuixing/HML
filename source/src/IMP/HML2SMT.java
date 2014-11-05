package IMP;

import AntlrGen.HMLLexer;
import AntlrGen.HMLParser;
import IMP.Basic.Constraint;
import IMP.Basic.VariableForSMT2;
import IMP.Check.ExecSMT;
import IMP.Check.Util;
import IMP.Exceptions.HMLException;
import IMP.Infos.AbstractExpr;
import IMP.Infos.HML2SMTListener;
import IMP.Infos.HSTErrorListener;
import IMP.Scope.ScopeConstructor;
import IMP.Translate.DiscreteWithContinuous;
import IMP.Translate.Dynamic;
import IMP.Translate.DynamicalVisitor;
import IMP.Translate.HMLProgram2SMTVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  The main class of the translation from HML model to SMT2 formulas
 */
public class HML2SMT {

    // The logger (log4j2) recording the infos, errors, traces...
    private static Logger  logger = LogManager.getLogger(HML2SMT.class.getName());

    // The max depth of the unrolling
    final static int depth = 15;

    // The HML model file path
    private static String modelPath = "./source/src/bouncingBall.hml";

    // The SMT2 formula path
    private static String smtPath ;

    // if the visualization argument is true, we will show the data in a browser
    private static boolean visualize = true;

    //If deepApproach is true, the unrolling is based on SMT,
    // otherwise, we try the full-unrolling
    private static boolean deepApproach = true;

    // The abstract expr map
    static ParseTreeProperty<AbstractExpr> exprPtp;

    // The guard map
    static ParseTreeProperty<AbstractExpr> guardPtp;

    // The initialization map, from var name to the initial value
    static HashMap<String, AbstractExpr>  InitID2ExpMap;

    // The list of vars
    static List<VariableForSMT2> varlist;

    // Signals
    static List<String> signals;

    // The listener that collects information in the model
    private static HML2SMTListener hml2SMTListener = new HML2SMTListener();

    // The parse tree for the HML model
    private static ParseTree  tree;

    // StringTemplate group
    private static STGroup group = new STGroupFile("HML.stg");

    // StringTemplate
    private static ST st = group.getInstanceOf("SMT2");

    public static void main(String[] args) throws Exception {
        InputStream inputStream = new FileInputStream(modelPath);
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
        signals = hml2SMTListener.getSignals();
        ScopeConstructor scl = new ScopeConstructor();
        walker.walk(scl, tree);

        varlist = hml2SMTListener.getVarlist();
        HMLProgram2SMTVisitor trans;
        if (!deepApproach)
            trans = new HMLProgram2SMTVisitor(scl.getScopes(),scl.getGlobals(), hml2SMTListener.getTmpMap(), depth);
        else
            trans = new DynamicalVisitor(scl.getScopes(),scl.getGlobals(), hml2SMTListener.getTmpMap(), depth);


        if(constructComponents(trans)) {
            writeFormulas(trans);
            if (visualize) {
                check(smtPath + " --visualize");
                Util.viewDataInWindow(smtPath);
            }
            else check(smtPath);
        }
        else  logger.error("Exit on Error, please check your model ...");




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

    public static boolean checkTemporaryFormulas(HMLProgram2SMTVisitor trans, String startPoint, int depth){
        addFlowsInSt(depth);
        addVarsToSMT(depth);
        prepareMainFormulas(st, trans.getPaths().get(0));
        st.add("formulas", startPoint);
        boolean res = false;
        try {
            writeToFile(st, System.currentTimeMillis(), "./source/src/dynamicChecking/HML_");
            res = check(smtPath);
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


    public  static void writeToFile(ST st, long pathId, String prefix) throws IOException {
        File out = new File(prefix + depth + "_" + pathId + ".smt2");
        if (out.createNewFile())  logger.debug("File successfully created");
        else                      logger.debug("File already exits.");
        st.write(out, new HSTErrorListener());
        smtPath = out.getPath();
    }

    private static boolean check(String path) {
        return ExecSMT.exec("0.0001", path);
    }






    /**
     *
     * @param prefix original var name
     * @param type The value can be "Bool", "Int" or "Real".
     * @param depth The unrolling depth, specifying the current depth
     * @return Variable list for SMT2 formulas, the name with "0" is for the value
     * before the action is taken, and "t" is for the value after the action is executed.
     */
    public static List getVarListForSMT2(String prefix, String type, int depth){
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
     * The list is created by HML2SMTListener
     * @return The list of Variables
     */
    public static List<VariableForSMT2> getVarlist() {
        return varlist;
    }

    /**
     *
     * @param trans visitor
     * @return true for success, false for fail
     */
    public static boolean constructComponents(HMLProgram2SMTVisitor trans){

        hml2SMTListener.getInitializations();

        InitID2ExpMap = hml2SMTListener.getInitID2ExpMap();
        
        trans.setCurrentVariableLink(hml2SMTListener.getFinalVariableLinks());
        try {
            trans.visit(tree);
        }catch (HMLException exp) {
            logger.error(exp.getMessage());
            return false;
        }
        addVarsToSMT(depth);
        addFlowsInSt(depth);
        return true;
    }

    private static void addVarsToSMT(int depth) {
        st.add("vars", varlist);
        for (VariableForSMT2 v : varlist) {
            st.add("uvars", getVarListForSMT2(v.name, v.type, depth));
        }
        st.add("tvars", getTimeOrModeVarListforSMT2("time", "Real", depth));
        st.add("mvars", getTimeOrModeVarListforSMT2("mode", "Int", depth));

        for (String signal : signals) {
            st.add("svars", getTimeOrModeVarListforSMT2(signal, "Real", depth));
        }
    }

    private static void removeVarsInSMT() {
        st.remove("vars");
        st.remove("uvars");
        st.remove("tvars");
        st.remove("mvars");
        st.remove("svars");
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

    public static boolean isSignal(String ID) {
        if (signals==null) return false;
        return signals.contains(ID);
    }

    public static List<String> getSignals(){
        return signals;
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
