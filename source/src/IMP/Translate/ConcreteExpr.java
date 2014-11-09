package IMP.Translate;

import IMP.Infos.AbstractExpr;

import java.util.ArrayList;
import java.util.List;

/**
 * HML-IMP.Translate
 * Created by fofo on 2014/10/5.
 * The deepcopy is implemented in this expr class
 */
public class ConcreteExpr {
    public String ID;

    public ConcreteExpr Left;

    public ConcreteExpr Right;

    public AbstractExpr.Sort sort = AbstractExpr.Sort.NVAR;

    private List<String> IDlist = null;

    private String renderStr = null;

    public  ConcreteExpr(AbstractExpr abstractExpr) {
        copy(abstractExpr);
    }

    public ConcreteExpr(String ID,  AbstractExpr.Sort sort){
        this(ID, sort, null, null);
    }

    public ConcreteExpr(String ID,  AbstractExpr.Sort sort, ConcreteExpr left, ConcreteExpr right) {
        this.ID = ID;
        this.Left = left;
        this.Right = right;
        this.sort = sort;
    }

    private  void copy(AbstractExpr abstractExpr) {
        if (abstractExpr!=null) {
            this.ID = abstractExpr.ID;
            this.sort = abstractExpr.sort;
            if (abstractExpr.Left!=null)
                this.Left = new ConcreteExpr(abstractExpr.Left);
            if (abstractExpr.Right!=null)
                this.Right = new ConcreteExpr(abstractExpr.Right);
        }
    }

    private ConcreteExpr clone(ConcreteExpr concreteExpr){
        if (concreteExpr!=null)
            return new ConcreteExpr(concreteExpr.ID,concreteExpr.sort,
                    clone(concreteExpr.Left), clone(concreteExpr.Right));
        else return null;
    }


    public String toString() {
        if (renderStr!=null) return renderStr;
        StringBuilder sb  = new StringBuilder();
        sb.append("(");
        sb.append(ID);
        if (Left != null) {
            sb.append(" ");
            sb.append(Left.toString());
            if (Right != null) {
                sb.append(" ");
                sb.append(Right.toString());
            }
            sb.append(")");
        }
        else
            sb.deleteCharAt(0);
        renderStr = sb.toString();
        return renderStr;
    }

    public String toStringForStartPoint(int depth) {
        StringBuilder sb  = new StringBuilder();
        sb.append("(");

        if (sort== AbstractExpr.Sort.VAR) { //对变量需要加下标
            if (ID.equals("clock")) sb.append(ID).append("_").append(depth-1).append("_t");
            else sb.append(ID).append("_").append(depth).append("_0");
        }
        else if (sort== AbstractExpr.Sort.GUARD) {
            if (ID.equals("timeout")) sb.append(ID).append("_").append(depth);
            else sb.append(ID);
        }
        else if (sort == AbstractExpr.Sort.Signal) {
            sb.append(ID).append("_").append(depth);
        }
        else
            sb.append(ID);

        if (Left != null) {
            sb.append(" ");
            sb.append(Left.toStringForStartPoint(depth));
            if (Right != null) {
                sb.append(" ");
                sb.append(Right.toStringForStartPoint(depth));
            }
            sb.append(")");
        }
        else
            sb.deleteCharAt(0);
        return sb.toString();
    }

    public String toString(int depth) {
        StringBuilder sb  = new StringBuilder();
        sb.append("(");

        if (sort== AbstractExpr.Sort.VAR) //对变量需要加下标
            sb.append(ID).append("_").append(depth).append("_t");
        else if (sort== AbstractExpr.Sort.GUARD) {
            if (ID.equals("timeout")) sb.append(ID).append("_").append(depth);
            else sb.append(ID);
        }
        else if (sort == AbstractExpr.Sort.Signal) {
            sb.append(ID).append("_").append(depth);
        }
        else
            sb.append(ID);

       if (Left != null) {
            sb.append(" ");
            sb.append(Left.toString(depth));
            if (Right != null) {
                sb.append(" ");
                sb.append(Right.toString(depth));
            }
            sb.append(")");
        }
        else
            sb.deleteCharAt(0);
        return sb.toString();
    }

    public String toStringForSignalInv(int depth){
        return String.format("(%s %s %s)", ID, Left.ID + "_" + depth, Right.toString(depth));
    }

    public String toString(VariableLink cwvk) {
        StringBuilder sb  = new StringBuilder();
        sb.append("(");


        String realID = cwvk.getRealVar(ID);

        if (realID.startsWith("@")) {
            //如果是常量，则设置替换后的类型为CONSTANT

            realID = realID.substring(1);
            sb.append(realID);
            sort = AbstractExpr.Sort.CONSTANT;
        }
        else sb.append(realID); //其他情况
        if (Left != null) {
            sb.append(" ");
            sb.append(Left.toString(cwvk));
            if (Right != null) {
                sb.append(" ");
                sb.append(Right.toString(cwvk));
            }
            sb.append(")");
        }
        else
            sb.deleteCharAt(0);


        if (ID.equals("d/dt")) {
            sb.deleteCharAt(0);
            sb.deleteCharAt(sb.length()-1);
            sb.replace(4,5,"[");
            sb.append("]");
        }
        return sb.toString();
    }

    public void resolve(VariableLink variableLink){


        if ( (this.sort== AbstractExpr.Sort.VAR || this.sort == AbstractExpr.Sort.Signal) && variableLink!=null) {

            ID = variableLink.getRealVar(this.ID);

            if (ID.startsWith("@")) {
                //如果是常量，则设置替换后的类型为CONSTANT
                ID = ID.substring(1);
                sort = AbstractExpr.Sort.CONSTANT;
            }

        }
        else {
            if (this.Left != null) {
                Left.resolve(variableLink);
            }
            if (this.Right != null) {
                Right.resolve(variableLink);
            }
        }

    }

    public ConcreteExpr signalInv() {
        ConcreteExpr res = new ConcreteExpr("<", AbstractExpr.Sort.GUARD, this, new ConcreteExpr("global", AbstractExpr.Sort.VAR));
        return res;
    }

    public ConcreteExpr signalExit(){
        this.sort = AbstractExpr.Sort.NVAR;
        ConcreteExpr res = new ConcreteExpr(">=", AbstractExpr.Sort.GUARD, this, new ConcreteExpr("global", AbstractExpr.Sort.VAR));
        return res;
    }

    public List<String> getIDList() {
        if (IDlist!=null) return IDlist;
        IDlist = new ArrayList<String>();
        IDlist.add(ID);
        if (Left != null) {
            for (String s : Left.getIDList()) {
                IDlist.add(s);
            }
        }
        if (Right != null) {
            for (String s : Right.getIDList()) {
                IDlist.add(s);
            }
        }
        return IDlist;
    }

    public void replace(String ID, ConcreteExpr concreteExpr) {
        if (this.ID.equals(ID)) {
            //clone a copy of concreteExpr
            //do not copy directly
            ConcreteExpr c = clone(concreteExpr);
            this.ID = c.ID;
            this.sort = c.sort;
            this.Left = c.Left;
            this.Right = c.Right;
            this.IDlist = c.IDlist;
            this.renderStr = c.renderStr;
        }
        else {
            if (Left != null)
                this.Left.replace(ID, concreteExpr);
            if (Right != null)
                this.Right.replace(ID, concreteExpr);
        }


    }

    public List<String> getVarsList(VariableLink variableLink){
        List<String> vars = new ArrayList<String>();
        if (this.sort== AbstractExpr.Sort.VAR){
            vars.add(variableLink.getRealVar(ID));
        }
        else {
            if (Left != null)
                appendList(vars, Left.getVarsList(variableLink));
            if (Right != null)
                appendList(vars, Right.getVarsList(variableLink));
        }
        return vars;
    }

    private void appendList(List<String> target, List<String> from) {
        for (String s : from) {
            target.add(s);
        }
    }

    public ConcreteExpr negationForInv(){
        ConcreteExpr result =new ConcreteExpr("not", AbstractExpr.Sort.NVAR, clone(this), null );
        result.checkEmptyGuard();
        return result;
    }

    /**
     *  转换成INV时不能直接用true，因为not true 为false，
     */
    public void checkEmptyGuard(){
        if (ID.equals("true")) {
             ID = ">=";
             Left = new ConcreteExpr("clock", AbstractExpr.Sort.VAR);
             Right =new ConcreteExpr("0", AbstractExpr.Sort.CONSTANT);
        }
        if (Left != null) {
            Left.checkEmptyGuard();
        }

        if (Right != null) {
            Right.checkEmptyGuard();
        }

    }

    public ConcreteExpr negation(){
        return new ConcreteExpr("not", AbstractExpr.Sort.NVAR, clone(this), null );
    }

    public void switchToInvariant() {

        ConcreteExpr tmp = Left;
        Left = Right;
        Right = tmp;

    }

    public List<String> guardBranches(int depth){
        if (ID.equals("not")) {
            List<String> tmp =Left.guardBranches(depth);
            List<String> res = new ArrayList<String>();
            for (String s : tmp) {
                res.add(String.format("(not %s)", s));
            }
            return res;
        }
        else if (ID.equals("or")) {
            List<String> ltmp = Left.guardBranches(depth);
            List<String> rtmp = Right.guardBranches(depth);
            List<String> res = new ArrayList<String>();
            res.addAll(ltmp);
            res.addAll(rtmp);
            return res;
        }
        else {
            List<String> tmp = new ArrayList<String>();
            tmp.add(toString(depth));
            return tmp;
        }
    }

}
