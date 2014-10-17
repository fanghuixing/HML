package IMP.Translate;

import AntlrGen.HMLParser;

import java.util.HashMap;

/**
 * HML-IMP.Translate
 * Created by fofo on 2014/10/5.
 */
public interface Dynamic{

    public void setDepth(int depth) ;

    public void addDiscrete(ContextWithVarLink bsc);

    public void addContinuous(ContextWithVarLink bsc);

    public HashMap<Integer, String> getOdeDefinitionMap();

    public Dynamic copy();

    /**
     *
     * @return 解析后的离散行为以SMT2公式表示，前后有换行符
     */
    public String getDiscreteDynamics();

    /**
     *
     * @return 解析后的连续行为以SMT2公式表示，前后有换行符
     */
    public String getContinuousDynamics();

    public void setDiscreteDynamics(String discreteDynamics);

    public void setContinuousDynamics(String continuousDynamics);
    public ContextWithVarLink getContinuous();

    public String getPartialResult(HMLParser.GuardContext guard, VariableLink variableLink);
    public void setGuardCheckEnable(boolean checkEnable);

}
