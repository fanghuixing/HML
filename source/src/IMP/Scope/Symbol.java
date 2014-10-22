package IMP.Scope;

/**
 * Created by fofo on 2014/9/30.
 */
public class Symbol {
    public static enum Type {
        NULL, Real, Int, Bool, Signal
    }

    String name;      // All symbols at least have a name
    Type type;
    Scope scope;      // All symbols know what scope contains them.

    public Symbol(String name) { this.name = name; }
    public Symbol(String name, Type type) { this(name); this.type = type; }
    public String getName() { return name; }
    public Type getType() {return type; }

    public String toString() {
        if ( type!=Type.NULL ) return '<'+getName()+":"+type+'>';
        return getName();
    }
}