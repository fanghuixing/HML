package IMP.Translate;

/**
 * Created by fofo on 2014/10/4.
 */
public class TypedString {
    AbstractExpr.Sort sort;
    String string;

    public TypedString(String string, AbstractExpr.Sort sort) {
        this.string = string;
        this.sort = sort;
    }
}
