package IMP.Translate;

import java.util.ArrayList;


/**
 * HML-IMP.Translate
 * Created by fofo on 2014/10/5.
 * 以树的结构存储所有Dynamic分支，
 * 每一条从根节点到叶子节点的路径可以组成一次深度展开
 */
public class VisitTree {
    /**
     * 当前节点所处的层次，从0开始递增，直到达到最大展开深度
     */
    private int layer = 0;

    /**
     * 存储语法环境
     */
    private DiscreteWithContinuous dynamics = null;

    /**
     * 当达到最大深度或者该节点没有子树可以展开时，terminal为true
     */
    private boolean terminal = false;

    /**
     * 父节点，双向树可以方便遍历
     */
    private VisitTree father = null;

    /**
     * 因为子树未必只有2个，所以需要用列表方式存储
     */
    private ArrayList<VisitTree> children = null;

    private int currentChildIndex = 0;

    /**
     * 创建非终端节点
     * @param dynamics 当前行为
     * @param father    父亲节点
     */
    public VisitTree(DiscreteWithContinuous dynamics, VisitTree father) {
        this(dynamics, false, father);
    }

    /**
     * 可用于创建终端节点
     * @param dynamics   当前行为
     * @param terminal 是否是终端节点
     * @param father 父亲节点
     */
    public VisitTree(DiscreteWithContinuous dynamics, boolean terminal, VisitTree father) {
        this.dynamics = dynamics;
        this.terminal = terminal;
        this.father = father;
        if (father!=null) {
            //layer的值可以在父亲的层次上加1
            this.layer = this.father.layer + 1;
        }
    }

    /**
     * 设置子树列表
     * @param children 子树
     */
    public void setChildren(ArrayList<VisitTree> children) {
        this.children = children;
    }



    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public void setFather(VisitTree father) {
        this.father = father;
    }

    public int getLayer() {
        return layer;
    }



    public boolean isTerminal() {
        return terminal;
    }

    public VisitTree getFather() {
        return father;
    }

    public ArrayList<VisitTree> getChildren() {
        return children;
    }

    /**
     * 增加一个分支
     * @param tree 某个子节点
     */
    public void addChild(VisitTree tree){
        if (children==null) {
            children = new ArrayList<VisitTree>(1);
        }
        children.add(tree);
    }

    /**
     *
     * @return 分支数目(children.size)
     */
    public int getSize() {
        return this.children.size();
    }

    public VisitTree getFirstChild() {
        return children.get(0);
    }

    public VisitTree getLastChild() {
        if (children.size()-1>=0) {
            return children.get(children.size() - 1);
        }
        return null;
    }

    public VisitTree getNextChild() {
        if (currentChildIndex>=0) {
            return children.get(currentChildIndex++);
        }
        return null;
    }

    public DiscreteWithContinuous getDynamics() {
        return dynamics;
    }

    public void setDynamics(DiscreteWithContinuous dynamics) {
        this.dynamics = dynamics;
    }
}
