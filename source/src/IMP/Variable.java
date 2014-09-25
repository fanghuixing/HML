package IMP;

import AntlrGen.HMLParser;
import AntlrGen.HMLParser.VariableInitializerContext;


/**
 * <p> The class for recording type, initial value and final flag for a variable </p>
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
