package DataSet;

import AntlrGen.JSONLexer;
import AntlrGen.JSONParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Stack;

/**
 * HML DataSet
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-21.
 */
public class HmlJsonProcess {
    private static Logger logger = LogManager.getLogger(HmlJsonProcess.class);
    public static void init(CollectData collectData, BufferedReader inputStream){

        try {
            StringBuilder sb=new StringBuilder();
            Stack<String> stack = new Stack<String>();
            while (true) {
                String line = inputStream.readLine();
                if (line==null) break;
                else {
                    if (line.equals("[") ){
                        stack.push(" ");
                    } else if (line.equals("]")) {
                        stack.pop();
                    }

                    if (stack.size()>=2){
                        sb.append(line);
                    } else if (stack.size()==1 && sb.length()>1){
                        sb.append(']');
                        logger.debug(sb.length());
                        processFlow(sb.toString(), collectData);
                        sb = new StringBuilder();
                    }
                }

            }

        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    public static void main(String[] args){
        //init();
    }

    public static void processFlow(String inputStream, CollectData collectData){
        ANTLRInputStream input = new ANTLRInputStream(inputStream);
        JSONLexer lexer = new JSONLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);
        parser.setBuildParseTree(true);
        logger.debug("Build Tree");
        ParseTree tree;
        tree = parser.flow();
        logger.debug("Tree Built");
        collectData.visit(tree);
    }





}
