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

    private List<String> IDlist = null;
    private String renderStr = null;

    public  AbstractExpr(String ID) {
        this(ID, null, null);
    }

    public AbstractExpr(String ID, AbstractExpr left, AbstractExpr right) {
        this.ID = ID;
        Left = left;
        Right = right;
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


}
