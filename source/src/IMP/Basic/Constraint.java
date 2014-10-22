package IMP.Basic;

import java.util.ArrayList;
import java.util.List;

public class Constraint {

    String name;
    String leftEnd;
    String rightEnd;
    String type;

    public Constraint(String name, String leftEnd, String rightEnd) {
        this.name = name;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
    }

    public Constraint(String name, String leftEnd, String rightEnd, String type) {
        this.name = name;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.type = type;
    }

    class NormalConstraint {
        String left;
        String right;
        public NormalConstraint(String left, String right){
            this.left = left;
            this.right =right;
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }
    }

    public List getNormalConstraintList(int depth){
        List<NormalConstraint> l = new ArrayList<NormalConstraint>();
        for (int i=0; i<=depth; i++) {
            if (this.name.equals("time") || this.name.equals("mode") ) {
                l.add(new NormalConstraint(this.leftEnd, this.name+"_"+i));
                l.add(new NormalConstraint(this.name+"_"+i, this.rightEnd));
            }
            else if (this.type!=null && this.type.equals("signal")) {
                //If this is a signal
                l.add(new NormalConstraint(this.leftEnd, this.name+"_"+i));
                l.add(new NormalConstraint(this.name+"_"+i, this.rightEnd));
            }
            else {
                l.add(new NormalConstraint(this.leftEnd, this.name + "_" + i + "_0"));
                l.add(new NormalConstraint(this.name + "_" + i + "_0", this.rightEnd));
                l.add(new NormalConstraint(this.leftEnd, this.name + "_" + i + "_t"));
                l.add(new NormalConstraint(this.name + "_" + i + "_t", this.rightEnd));
            }
        }
        return l;
    }
}
