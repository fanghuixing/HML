package IMP;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.FileInputStream;
import java.io.InputStream;
import AntlrGen.*;
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
        System.out.println(converter.getVarsInSMT2Formula(depth));

        System.out.println(converter.getInitializations());

    }
}
