package IMP.Basic;

/**
 * This class is used when we translating the variable in a model
 * into the variable (appended with "_t" or "_0" and depth information) in SMT2 formulas
 * The value of type can be "Bool", "Int" or "Real".
 *
 */
public class VariableForSMT2 {
    public String name;

    public String type;
    public VariableForSMT2(){}
    public VariableForSMT2(String name, String type){
         this.name = name;
         this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

}
