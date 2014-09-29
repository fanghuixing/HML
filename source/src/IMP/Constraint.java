package IMP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fofo on 2014/9/29.
 */
public class Constraint {

    String name;
    String leftEnd;
    String rightEnd;

    public Constraint(String name, String leftEnd, String rightEnd) {
        this.name = name;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
    }

    class NormalConstraint {
        String left;
        String right;
        public NormalConstraint(String left, String right){
            this.left = left;
            this.right =right;
        }


    }

    public List getNormalConstraintList(int depth){
        List<NormalConstraint> l = new ArrayList<NormalConstraint>();
        for (int i=0; i<=depth; i++) {
            l.add(new NormalConstraint(this.leftEnd, this.name+"_"+i+"_0"));
            l.add(new NormalConstraint(this.name+"_"+i+"_0", this.rightEnd));
            l.add(new NormalConstraint(this.leftEnd, this.name+"_"+i+"_t"));
            l.add(new NormalConstraint(this.name+"_"+i+"_t", this.rightEnd));
        }
        return l;
    }
}
