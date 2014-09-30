package IMP.Basic;

import AntlrGen.HMLParser.VariableInitializerContext;


/**
 * <p> The class for recording type, initial value and final flag for a variable </p>
 */
public class Variable extends VariableForSMT2 {


    public  Variable(String type, VariableInitializerContext init, boolean isFinal){
        this.type = type;
        this.init = init;
        this.isFinal = isFinal;
    }
    public  Variable(String name, String type, VariableInitializerContext init, boolean isFinal){
        this.name = name;
        this.type = type;
        this.init = init;
        this.isFinal = isFinal;
    }
    public VariableInitializerContext init;
    public boolean isFinal;
}
