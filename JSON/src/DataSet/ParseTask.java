package DataSet;

import AntlrGen.JSONLexer;
import AntlrGen.JSONParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-21.
 */public class ParseTask implements Runnable{
    private BufferedReader data;
    private static Logger logger = LogManager.getLogger(ParseTask.class);
    public ParseTask(BufferedReader data) {
        this.data = data;
    }

    public  void processFlow(BufferedReader inputStream){

        ANTLRInputStream input = null;
        try {
            input = new ANTLRInputStream(inputStream);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        JSONLexer lexer = new JSONLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);
        parser.setBuildParseTree(true);
        logger.debug("Build Tree");

        ParseTree tree;
        tree = parser.flow();
        logger.debug("Tree Built");

    }

    @Override
    public void run() {
        try {
            processFlow(data);
            data=null;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}