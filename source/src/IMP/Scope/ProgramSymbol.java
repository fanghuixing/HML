package IMP.Scope;

/**
 * Created by fofo on 2014/9/30.
 */
public class ProgramSymbol extends TemplateSymbol {
    public ProgramSymbol(String name, Type type, Scope enclosingScope) {
        super(name, type, enclosingScope);
    }

    @Override
    public String toString() {
        return "Program:"+ name + ":" + arguments.values();
    }
}
