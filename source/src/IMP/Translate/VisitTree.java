package IMP.Translate;

import IMP.Merge.PathsMerge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static Logger logger = LogManager.getLogger(VisitTree.class.getName());
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
        if (children==null) {
            leaves.add(this);
        }
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
        if (children==null || children.size()==0){
            //logger.debug("children is empty");
            if (father==null) return;
            List<VisitTree> terminals =  getTerminalChildren(father);
            //logger.debug("terminal size: " + terminals.size());
            for (int i=0; i<terminals.size(); i++){

                VisitTree Va = terminals.get(i);
                HashSet<String> set = new HashSet<String>();
                boolean flag = false;
                for (int j=i+1; j<terminals.size(); j++){
                    VisitTree Vb = terminals.get(j);
                    if (match(Va.getCurrentDynamicList(), Vb.getCurrentDynamicList())){
                        //if the current dynamic list of va and vb have the same continuous dynamics
                        //we can merge the last discrete dynamics
                        Dynamic lastVa = Va.getLastDynamic();
                        Dynamic lastVb = Vb.getLastDynamic();
                        set.add(lastVa.getDiscreteDynamics());
                        set.add(lastVb.getDiscreteDynamics());
                        //logger.debug("remove child");
                        father.removeChild(Vb);
                        terminals.remove(Vb);
                        j--;
                        flag = true;
                    }
                }
                if (flag) {
                    merge(Va, set);
                } else {
                    //logger.debug("No merge");
                }

            }

            if (father.children.size()<2){
                VisitTree only_one = father.children.get(0);

                VisitTree newFather = father.father;
                if (newFather==null) return;
                newFather.removeChild(father);
                father = newFather;
                only_one.father = father;
                //logger.debug("Add child");

                father.children.add(only_one);
            }




        }
        else if (children!=null && children.size()>0) {
            //logger.debug("child is not empty: " + children.size());
            List<VisitTree> visited = new ArrayList<VisitTree>();
            while (true) {
                VisitTree tree = null;
                for (VisitTree v : children) {
                    if (!visited.contains(v)){
                        tree = v;
                        visited.add(v);
                        break;
                    }
                }
                if (tree==null) break;

                tree.merge();

            }

        }

    }

    private List<VisitTree> copyList(List<VisitTree> from) {
        List<VisitTree> l = new ArrayList<VisitTree>();
        for (VisitTree s : from) {
            l.add(s);
        }
        return l;
    }

    private void merge(VisitTree visitTree, HashSet<String> set){
        //logger.debug("Merge discrete dynamics");
        List<String> dynamics = new ArrayList<String>();
        dynamics.addAll(set);
        String res = PathsMerge.mergeDynamics(dynamics);
        visitTree.getLastDynamic().setDiscreteDynamics(res);
    }


    /**
     *
     * @param pa One Dynamic List
     * @param pb Another Dynamic List
     * @return if pa and pb have the same continuous dynamics, then return true, otherwise false
     */
    private boolean match(List<Dynamic> pa, List<Dynamic> pb) {

        if (pa==null || pb==null) return false;
        if (pa.size() != pb.size()) return false;
        for (int i=0; i<pa.size(); i++) {
            if (!pa.get(i).getContinuous().equals(pb.get(i).getContinuous())){
                return false;
            }
        }
        //logger.debug("Dynamics Matched");
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

    public void addDiscrete(ContextWithVarLink contextWithVarLink){
        currentDynamics.addDiscrete(contextWithVarLink);
    }
    public void addContinuous(ContextWithVarLink contextWithVarLink) {
        currentDynamics.addContinuous(contextWithVarLink);
    }

    public void addDynamics(Dynamic dynamic){
        currentDynamicList.add(dynamic);
    }
}
