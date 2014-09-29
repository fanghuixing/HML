package IMP;

/**
 * Created by fofo on 2014/9/29.
 */
public class VariableForSMT2 {



    String name;
    String type;
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
