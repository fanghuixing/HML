package IMP.Translate;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Created by fofo on 2014/10/1.
 */
public class ContextWithVarLink {

    ParserRuleContext prc;
    VariableLink vrl;
    boolean negation = false;//if negation = true, the condition has to be reversed.

    public ContextWithVarLink(ParserRuleContext prc, VariableLink vrl ) {
        this.vrl = vrl;
        this.prc = prc;
    }

    public ContextWithVarLink(ParserRuleContext prc, VariableLink vrl, boolean negation) {
        this.prc = prc;
        this.vrl = vrl;
        this.negation = negation;
    }

    public ParserRuleContext getPrc() {
        return prc;
    }

    public VariableLink getVrl() {
        return vrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextWithVarLink)) return false;

        ContextWithVarLink that = (ContextWithVarLink) o;

        if (negation != that.negation) return false;
        if (prc != that.prc) return false;
        if (vrl != that.vrl) return false;

        return true;
    }

}
