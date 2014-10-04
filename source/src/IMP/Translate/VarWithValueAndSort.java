package IMP.Translate;

/**
 * Created by fofo on 2014/10/4.
 */

public class VarWithValueAndSort {
    String value;
    AbstractExpr.Sort sort;
    public VarWithValueAndSort(String value, AbstractExpr.Sort sort){
        this.value=value;
        this.sort=sort;
    }

}
