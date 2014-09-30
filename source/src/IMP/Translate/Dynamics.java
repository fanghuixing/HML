package IMP.Translate;



import java.util.ArrayList;
import java.util.List;

import AntlrGen.HMLParser;
import IMP.HML2SMT;
import jdk.nashorn.internal.ir.ContinueNode;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

/**
 * Created by fofo on 2014/9/30.
 */
public class Dynamics {
    private int depth;
    private List<ParserRuleContext> discrete = new ArrayList<ParserRuleContext>();
    private List<ParserRuleContext> continuous = new ArrayList<ParserRuleContext>();

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void addDiscrete(ParserRuleContext bsc){
        discrete.add(bsc);
    }

    public void addContinuous(ParserRuleContext bsc) {
        continuous.add(bsc);
    }

    @Override
    public String toString() {
        ParseTreeProperty<String> exprs = HML2SMT.getExprPtp();
        StringBuilder sb = new StringBuilder();
        sb.append("Discrete: ");
        for (ParserRuleContext r : discrete) {
            if (r instanceof HMLParser.AssignmentContext) {
                HMLParser.AssignmentContext ar = (HMLParser.AssignmentContext) r;
                sb.append(String.format("(= %s %s)", ar.ID(), exprs.get(ar.expr())));
            }
        }

        sb.append(" Continuous: ");
        for (ParserRuleContext r : continuous) {
            sb.append(r.getText());
        }
        return sb.toString();
    }
}
