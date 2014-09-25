package IMP;

import AntlrGen.HMLParser;
import AntlrGen.HMLParser.VariableInitializerContext;
/**
 * Created by Huixing Fang on 2014/9/25.
 */
public class Variable {

    public  Variable(String type, VariableInitializerContext init, boolean isFinal){
        this.type = type;
        this.init = init;
        this.isFinal = isFinal;
    }

    String type;
    VariableInitializerContext init;
    boolean isFinal;
}
