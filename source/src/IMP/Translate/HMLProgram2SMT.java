package IMP.Translate;

import AntlrGen.HMLBaseVisitor;
import IMP.Scope.GlobalScope;
import IMP.Scope.Scope;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

/**
 * Created by fofo on 2014/9/30.
 */
public class HMLProgram2SMT extends HMLBaseVisitor {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope


}
