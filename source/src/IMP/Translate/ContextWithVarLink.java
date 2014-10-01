package IMP.Translate;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Created by fofo on 2014/10/1.
 */
public class ContextWithVarLink {

    ParserRuleContext prc;
    VariableLink vrl;

    public ContextWithVarLink(ParserRuleContext prc, VariableLink vrl ) {
        this.vrl = vrl;
        this.prc = prc;
    }

    public ParserRuleContext getPrc() {
        return prc;
    }

    public VariableLink getVrl() {
        return vrl;
    }
}
