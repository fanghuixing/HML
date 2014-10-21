package IMP.Scope;

import AntlrGen.HMLBaseListener;
import AntlrGen.HMLParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by fofo on 2014/9/30.
 */
public class ScopeConstructor extends HMLBaseListener {
    private static Logger  logger = LogManager.getLogger(ScopeConstructor.class.getName());
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
    GlobalScope globals;
    Scope currentScope; // 当前所处的scope


    public ParseTreeProperty<Scope> getScopes() {
        return scopes;
    }

    public GlobalScope getGlobals() {
        return globals;
    }

    void saveScope(ParserRuleContext ctx, Scope s) { scopes.put(ctx, s);
}

    public void enterHybridModel(HMLParser.HybridModelContext ctx) {
        globals = new GlobalScope(null);
        currentScope = globals;
    }

    public void enterTemplate(HMLParser.TemplateContext ctx) {
        StringBuilder name = new StringBuilder();
        name.append(ctx.ID().getText()).append("(");
        HMLParser.FormalParameterDeclsContext fpc = ctx.formalParameters().formalParameterDecls();
        while (fpc != null) {
            name.append(fpc.type().getText()).append(",");
            fpc = fpc.formalParameterDeclsRest().formalParameterDecls();
        }
        name.deleteCharAt(name.length()-1).append(")");
        Symbol.Type type = Symbol.Type.NULL;

        TemplateSymbol template = new TemplateSymbol(name.toString(), type, currentScope);
        currentScope.define(template);
        saveScope(ctx, template);
        currentScope = template;
    }

    public void exitTemplate(HMLParser.TemplateContext ctx) {
        logger.debug("Exit Template " + currentScope.toString());
        currentScope = currentScope.getEnclosingScope();
    }

    public void enterProgram(HMLParser.ProgramContext ctx) {
        String name = "Main";
        Symbol.Type type = Symbol.Type.NULL;
        ProgramSymbol pro = new ProgramSymbol(name, type, currentScope);
        currentScope.define(pro);
        saveScope(ctx, pro);
        currentScope = pro;
    }

    public void exitProgram(HMLParser.ProgramContext ctx) {
        logger.debug("Exit Program " + currentScope.toString());
        currentScope = currentScope.getEnclosingScope();
    }

    public void exitFormalParameterDecls(HMLParser.FormalParameterDeclsContext ctx) {
        HMLParser.FormalParameterDeclsContext fpc = ctx;
        while (fpc != null) {
            defineVar(fpc.type(), fpc.formalParameterDeclsRest().variableDeclaratorId().ID().getSymbol());
            fpc = fpc.formalParameterDeclsRest().formalParameterDecls();
        }
    }

    public void exitVariableDeclaration(HMLParser.VariableDeclarationContext ctx) {
        for (HMLParser.VariableDeclaratorContext v :
                ctx.variableDeclarators().variableDeclarator()){
            defineVar(ctx.type(), v.variableDeclaratorId().ID().getSymbol());
        }
    }



    void defineVar(HMLParser.TypeContext typeCtx, Token nameToken) {
        String typeName = typeCtx.getText();
        Symbol.Type type = getType(typeName);
        VariableSymbol var = new VariableSymbol(nameToken.getText(), type);
        currentScope.define(var); // Define symbol in current scope
    }

    public static Symbol.Type getType(String tokenType) {
        if (tokenType.equals("float"))   return Symbol.Type.Real;
        if (tokenType.equals("int"))    return Symbol.Type.Int;
        if (tokenType.equals("boolean"))  return Symbol.Type.Bool;

        return Symbol.Type.NULL;
    }


}
