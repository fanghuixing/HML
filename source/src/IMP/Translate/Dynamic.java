package IMP.Translate;

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


}
