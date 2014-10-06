package IMP.Translate;

import java.util.ArrayList;
import java.util.List;


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
     * 存储当前dynamic
     */
    private Dynamic currentDynamics = null;

    private int currentDepth = 0;

    private List<Dynamic> currentDynamicList = null;

    /**
     * 当达到最大深度或者该节点没有子树可以展开时，terminal为true
     */
    private boolean terminal = false;

    /**
     * 因为子树未必只有2个，所以需要用列表方式存储
     */
    private ArrayList<VisitTree> children = null;

    private int currentChildIndex = 0;

    private VisitTree father = null;



    /**
     * 创建非终端节点
     * @param dynamics 当前行为
     *
     */
    public VisitTree(Dynamic dynamics, List<Dynamic> dynamicList) {
        this.currentDynamics = dynamics;
        this.currentDynamicList = dynamicList;
    }


    public VisitTree(VisitTree father, Dynamic currentDynamics, List<Dynamic> currentDynamicList) {
        this.father = father;
        this.currentDynamics = currentDynamics;
        this.currentDynamicList = currentDynamicList;
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



    public int getLayer() {
        return layer;
    }



    public boolean isTerminal() {
        return terminal;
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

    public void collectLeaves(List<VisitTree> leaves){
        if (children==null)
            leaves.add(this);
        else {
            for (VisitTree e : children) e.collectLeaves(leaves);
        }
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

    public Dynamic getCurrentDynamics() {
        return currentDynamics;
    }

    public void setCurrentDynamics(Dynamic currentDynamics) {
        this.currentDynamics = currentDynamics;
    }

    public List<Dynamic> getCurrentDynamicList() {
        return currentDynamicList;
    }

    public void setCurrentDynamicList(List<Dynamic> currentDynamicList) {
        this.currentDynamicList = currentDynamicList;
    }

    public int getCurrentDepth() {
        return currentDynamicList.size();
    }


    //删除已经达到最大深度的节点路径，免得下次加子树的时候又加一次
    public void delete(){
        if (father!=null){
            if (children==null || children.size()==0) {
                System.out.println("Delete one path in the tree "+father.getChildren().size());
                father.getChildren().remove(this);
                System.out.println("After Delete : " + father.getChildren().size());
                father.delete();
            }
        }
    }
}
