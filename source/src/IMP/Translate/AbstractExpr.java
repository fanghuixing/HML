package IMP.Translate;

import AntlrGen.HMLParser;

import java.sql.RowIdLifetime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fofo on 2014/10/1.
 */
public class AbstractExpr {
    public String ID;
    public AbstractExpr Left;
    public AbstractExpr Right;
    public Sort sort = Sort.NVAR;
    private List<String> IDlist = null;
    private String renderStr = null;

    public  AbstractExpr(String ID) {
        //非变量叶子节点
        this(ID, Sort.NVAR, null, null);
    }


    public  AbstractExpr(String ID, Sort sort) {
        //带sort的叶子节点
        this(ID, sort, null, null);
    }

    public  AbstractExpr(String ID, AbstractExpr left, AbstractExpr right) {
        //非变量
        this(ID, Sort.NVAR, left, right);
    }

    public static enum Sort {
        VAR, NVAR, GUARD, CONSTANT
    }

    public AbstractExpr(String ID, Sort sort, AbstractExpr left, AbstractExpr right) {
        this.ID = ID;
        this.sort = sort;
        this.Left = left;
        this.Right = right;
    }

    @Override
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

        if (sort==Sort.VAR) //对变量需要加下标
            sb.append(ID).append("_").append(depth).append("_t");
        else if (sort==Sort.GUARD) {
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

        sb.append(cwvk.getRealVar(ID));
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
        if (this.sort==Sort.VAR && variableLink!=null) {
            //System.out.println("Resolve --------- " + ID);
            ID = variableLink.getRealVar(this.ID);
            if (ID.startsWith("@")) {
                ID = ID.substring(1);
                sort = Sort.CONSTANT;
            }
            //System.out.println("to --------- " + ID);
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

    public void replace(String ID, AbstractExpr abstractExpr) {
        if (this.ID.equals(ID)) {
            this.ID = abstractExpr.ID;
            this.sort = abstractExpr.sort;
            this.Left = abstractExpr.Left;
            this.Right = abstractExpr.Right;
            this.IDlist = abstractExpr.IDlist;
            this.renderStr = abstractExpr.renderStr;
        }
        else {
            if (Left != null)
                this.Left.replace(ID, abstractExpr);
            if (Right != null)
                this.Right.replace(ID, abstractExpr);
        }


    }

    public List<String> getVarsList(VariableLink variableLink){
        List<String> vars = new ArrayList<String>();
        if (this.sort==Sort.VAR){
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

    public void appendList(List<String> target, List<String> from) {
        for (String s : from) {
            target.add(s);
        }
    }

}
