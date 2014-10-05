package IMP.Infos;

/**
 * Created by fofo on 2014/10/1.
 * 在对程序进行展开过程中，不能对该类的对象进行修改，
 * 否则会破坏原始程序结构
 */
public class AbstractExpr {
    public String ID;

    public AbstractExpr Left;

    public AbstractExpr Right;

    public Sort sort = Sort.NVAR;

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



}
