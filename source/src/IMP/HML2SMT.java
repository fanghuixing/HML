package IMP;

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
 * Created by Huixing Fang on 2014/9/25.
 */
public class HML2SMT {

    public static void main(String[] args) throws Exception {
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
        InputStream is = System.in;
        if ( inputFile!=null ) {
            is = new FileInputStream(inputFile);
        }
        ANTLRInputStream input = new ANTLRInputStream(is);
        HMLLexer lexer = new HMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HMLParser parser = new HMLParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.hybridModel();


        ParseTreeWalker walker = new ParseTreeWalker();
        HML2SMTListener converter = new HML2SMTListener();
        walker.walk(converter, tree);

        int depth = 10;




        List<VariableForSMT2> varlist = converter.getVarlist();
        STGroup group = new STGroupFile("HML.stg");
        ST st = group.getInstanceOf("SMT2");
        st.add("vars", varlist);
        for (VariableForSMT2 v : varlist) {
            st.add("uvars", getVarListforSMT2(v.name, v.type, depth));
        }

        st.add("tvars", getVarListforSMT2("time", "Real", depth));
        st.add("mvars", getVarListforSMT2("mode", "Int", depth));
        st.add("inits", converter.getInitializations());

        List<Constraint> cons = converter.getConstraintsList();
        for (Constraint c : cons) {
            st.add("constraints", c.getNormalConstraintList(depth));
        }


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
