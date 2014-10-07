package IMP.Translate;

import IMP.Merge.PathsMerge;
import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;

import java.util.ArrayList;
import java.util.HashSet;
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
                father.getChildren().remove(this);
                father.delete();
            }
        }
    }

    public void merge(){
        System.out.println("Merge ... ...");
        if (children==null || children.size()==0){
            if (father==null) return;
            List<VisitTree> terminals =  getTerminalChildren(father);
            for (int i=0; i<terminals.size(); i++){
                System.out.println("In loop ...");
                VisitTree Va = terminals.get(i);
                HashSet<String> set = new HashSet<String>();
                boolean flag = false;
                for (int j=i+1; j<terminals.size(); j++){
                    VisitTree Vb = terminals.get(j);
                    if (match(Va.getCurrentDynamicList(), Vb.getCurrentDynamicList())){
                        Dynamic lastVa = Va.getLastDynamic();
                        Dynamic lastVb = Vb.getLastDynamic();
                        System.out.println(lastVa);
                        System.out.println(lastVb);
                        set.add(lastVa.getDiscreteDynamics());
                        set.add(lastVb.getDiscreteDynamics());
                        father.removeChild(Vb);
                        terminals.remove(Vb);
                        j--;
                        flag = true;
                        System.out.println(" in if currentDynamicList size :" + currentDynamicList.size());
                    }
                }
                if (flag) merge(Va, set);

            }

            if (father.children.size()<2){
                VisitTree newFather = father.father;
                if (newFather==null) return;
                newFather.removeChild(father);
                father = newFather;
                father.children.add(this);
            }


        }
        else if (children!=null && children.size()>0) {
            for (VisitTree v : children){
                System.out.println("Merge...");
                v.merge();
            }
        }

    }

    private void merge(VisitTree visitTree, HashSet<String> set){
        System.out.println(" size :" + visitTree.currentDynamicList.size());
        List<String> dynamics = new ArrayList<String>();
        dynamics.addAll(set);
        visitTree.getLastDynamic().setDiscreteDynamics(PathsMerge.mergeDynamics(dynamics));
        System.out.println("--"+visitTree.getLastDynamic().getDiscreteDynamics());
        System.out.println(" size :" + visitTree.currentDynamicList.size());
    }



    private boolean match(List<Dynamic> pa, List<Dynamic> pb) {
        if (pa==null || pb==null) return false;
        if (pa.size() != pb.size()) return false;
        for (int i=0; i<pa.size(); i++) {
            if (!pa.get(i).getContinuous().equals(pb.get(i).getContinuous())){
                return false;
            }
        }
        return true;
    }


    private List<VisitTree> getTerminalChildren(VisitTree visitTree){
        List<VisitTree> ret = visitTree.children;
        List<VisitTree> res = new ArrayList<VisitTree>();
        if (ret!=null) {
            for (VisitTree vt : ret) {
                if (vt.children==null || vt.children.size()==0){
                    res.add(vt);
                }
            }
        }
        return res;
    }

    public void removeChild(VisitTree visitTree){
        children.remove(visitTree);
    }

    public Dynamic getLastDynamic(){
        return currentDynamicList.get(currentDynamicList.size()-1);
    }
}
