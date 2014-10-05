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

    public String toString(int depth) {
        StringBuilder sb  = new StringBuilder();
        sb.append("(");

        if (sort== AbstractExpr.Sort.VAR) //对变量需要加下标
            sb.append(ID).append("_").append(depth).append("_t");
        else if (sort== AbstractExpr.Sort.GUARD) {
            if (ID.equals("timeout")) sb.append(ID).append("_").append(depth);
            else sb.append(ID);
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

    public String toString(VariableLink cwvk) {
        StringBuilder sb  = new StringBuilder();
        sb.append("(");

        System.out.println("---Render String From : " + ID);
        String realID = cwvk.getRealVar(ID);
        System.out.println("---To : " + realID);
        if (realID.startsWith("@")) {
            //如果是常量，则设置替换后的类型为CONSTANT
            System.out.println("This is a constant");
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
        if (this.sort== AbstractExpr.Sort.VAR && variableLink!=null) {
            System.out.println("Resolve --------- " + ID);
            ID = variableLink.getRealVar(this.ID);

            if (ID.startsWith("@")) {
                //如果是常量，则设置替换后的类型为CONSTANT
                ID = ID.substring(1);
                sort = AbstractExpr.Sort.CONSTANT;
            }
            System.out.println("to --------- " + ID);
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

}
