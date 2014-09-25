package IMP;

import AntlrGen.HMLParser;

/**
 * Created by Huixing Fang on 2014/9/25.
 */
public class Variable {

    public  Variable(String type, HMLParser.VariableInitializerContext init, boolean isFinal){
        this.type = type;
        this.init = init;
        this.isFinal = isFinal;
    }

    String type;
    HMLParser.VariableInitializerContext init;
    boolean isFinal;
}
