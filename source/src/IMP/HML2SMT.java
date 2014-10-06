package IMP;

import IMP.Basic.Constraint;
import IMP.Basic.VariableForSMT2;
import IMP.Infos.HML2SMTListener;
import IMP.Infos.HSTErrorListener;
import IMP.Scope.ScopeConstructor;
import IMP.Infos.AbstractExpr;
import IMP.Translate.DiscreteWithContinuous;
import IMP.Translate.Dynamic;
import IMP.Translate.HMLProgram2SMTVisitor;
import IMP.Translate.VisitTree;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import AntlrGen.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

/**
 * ${PROJECT_NAME} - ${PACKAGE_NAME}
 * Created by Huixing Fang on 2014/9/25.
 */
public class HML2SMT {
    final static int depth = 10;
    static ParseTreeProperty<AbstractExpr> exprPtp;
    static ParseTreeProperty<AbstractExpr> guardPtp;
    static  HashMap<String, AbstractExpr>  InitID2ExpMap;
    static List<VariableForSMT2> varlist;

    public static void main(String[] args) throws Exception {
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
        InputStream is = System.in;
        if ( inputFile!=null ) {
            is = new FileInputStream(inputFile);
        }

        //测试文件
        is = new FileInputStream("H:\\Antlr\\HML\\source\\src\\watertank.hml");

        ANTLRInputStream input = new ANTLRInputStream(is);
        HMLLexer lexer = new HMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HMLParser parser = new HMLParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.hybridModel();
        ParseTreeWalker walker = new ParseTreeWalker();
        HML2SMTListener hml2SMTListener = new HML2SMTListener();
        walker.walk(hml2SMTListener, tree);
        exprPtp = hml2SMTListener.getExprPtp();
        guardPtp = hml2SMTListener.getGuardPtp();
        ScopeConstructor scl = new ScopeConstructor();
        walker.walk(scl, tree);



        varlist = hml2SMTListener.getVarlist();
        STGroup group = new STGroupFile("HML.stg");
        ST st = group.getInstanceOf("SMT2");
        st.add("vars", varlist);
        for (VariableForSMT2 v : varlist) {
            st.add("uvars", getVarListforSMT2(v.name, v.type, depth));
        }

        st.add("tvars", getTimeOrModeVarListforSMT2("time", "Real", depth));
        st.add("mvars", getTimeOrModeVarListforSMT2("mode", "Int", depth));
        hml2SMTListener.getInitializations();



        List<Constraint> cons = hml2SMTListener.getConstraintsList();
        for (Constraint c : cons) {
            st.add("constraints", c.getNormalConstraintList(depth));
        }



        InitID2ExpMap = hml2SMTListener.getInitID2ExpMap();
        HMLProgram2SMTVisitor trans = new HMLProgram2SMTVisitor(scl.getScopes(),scl.getGlobals(), hml2SMTListener.getTmpMap(), depth);
        trans.setCurrentVariableLink(hml2SMTListener.getFinalVariableLinks());
        trans.visit(tree);
        //add paths
        //List<Dynamic> onePath = trans.getCurrentDynamicsList();



        List<List<Dynamic>> paths = trans.getPaths();
        for (List<Dynamic> onePath : paths) {
            System.out.println("------------------Begin one path------------------");
            for (Dynamic dy : onePath) {
                System.out.println(dy);
                st.add("formulas", dy.toString());
                System.out.println();
            }
            System.out.println("------------------End path------------------------");
            break;
        }



        for (Map.Entry<Integer,String> ode : DiscreteWithContinuous.getOdeMap().entrySet()) {
            System.out.println(ode.getValue());
            st.add("flows", ode.getValue());
        }


        int modeNum = DiscreteWithContinuous.getOdeMap().size();
        st.add("constraints", new Constraint("mode", "1", modeNum+"").getNormalConstraintList(depth));



        //String result = st.render();
        File out = new File("H:\\Antlr\\HML\\source\\src\\HML.smt2");
        if (out.createNewFile())  System.out.println("File successfully created");
        else                      System.out.println("File already exits.");
        st.write(out, new HSTErrorListener());

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
