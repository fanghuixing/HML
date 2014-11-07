package IMP.Translate;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ContextWithVarLink {

    ParserRuleContext prc;
    VariableLink vrl;
    boolean negation = false;//if negation = true, the condition has to be reversed.

    List<ContextWithVarLink> parallel=null;

    public ContextWithVarLink(List<ContextWithVarLink> parallel){ this.parallel = parallel;}

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

    public List<ContextWithVarLink> getParallel() {
        return parallel;
    }

    public void setParallel(List<ContextWithVarLink> parallel) {
        this.parallel = parallel;
    }

    public void addContextWithlink(ContextWithVarLink contextWithVarLink){
        if (parallel==null)
            this.parallel = new ArrayList<ContextWithVarLink>();
        this.parallel.add(contextWithVarLink);
    }
}
