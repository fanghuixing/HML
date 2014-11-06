package IMP.Parallel;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * HML IMP.Parallel
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-6.
 */
public class DynamicsContext {

    private List<ParserRuleContext> discreteList = new ArrayList<ParserRuleContext>();
    private List<ParserRuleContext> continuousList = new ArrayList<ParserRuleContext>();

    public void addDiscrete(ParserRuleContext ctx) {
        discreteList.add(ctx);
    }

    public void addContinuous(ParserRuleContext ctx) {
        continuousList.add(ctx);
    }

    public List<ParserRuleContext> getDiscreteList() {
        return discreteList;
    }

    public List<ParserRuleContext> getContinuousList() {
        return continuousList;
    }
}
