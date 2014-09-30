package IMP.Scope;

/**
 * Created by fofo on 2014/9/30.
 */
public class GlobalScope extends BaseScope {
    public GlobalScope(Scope enclosingScope) { super(enclosingScope); }
    public String getScopeName() { return "globals"; }
}
