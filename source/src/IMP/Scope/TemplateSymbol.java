package IMP.Scope;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by fofo on 2014/9/30.
 */
public class TemplateSymbol extends Symbol implements Scope{
    Scope enclosingScope;
    Map<String, Symbol> arguments = new LinkedHashMap<String, Symbol>();

    public TemplateSymbol(String name, Type type, Scope enclosingScope) {
        super(name, type);
        this.enclosingScope = enclosingScope;
    }

    @Override
    public String getScopeName() {
        return name;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(Symbol sym) {
        arguments.put(sym.name, sym);
        sym.scope = this; // track the scope in each symbol
    }

    @Override
    public Symbol resolve(String name) {
        Symbol s = arguments.get(name);
        if ( s!=null ) return s;
        // if not here, check any enclosing scope
        if ( getEnclosingScope() != null ) {
            return getEnclosingScope().resolve(name);
        }
        return null; // not found
    }

    @Override
    public String toString() {
        return "Template:"+super.toString()+":"+arguments.values();
    }
}
