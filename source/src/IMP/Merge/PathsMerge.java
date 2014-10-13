package IMP.Merge;


import IMP.Translate.Dynamic;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * HML-IMP.Merge
 * Created by fofo on 2014/10/7.
 */
public class PathsMerge {

    private static Logger  logger = LogManager.getLogger(PathsMerge.class.getName());
    private List<List<Dynamic>> result;
    private List<HashSet<String>> sets;


    public void mergePaths(List<List<Dynamic>> preMerge) {
        logger.info("The size of paths before merge:" + preMerge.size());
        List<List<Dynamic>> postMerge = new ArrayList<List<Dynamic>>();

        for (int i = 0; i < preMerge.size(); i++) {
            List<Dynamic> pre1 = preMerge.get(i);
            int max = pre1.size();
            createSet(max);
            //对path列表中每一个path进行判断

            boolean flag = false;
            for (int j = i + 1; j < preMerge.size(); j++) {
                List<Dynamic> pre2 = preMerge.get(j);
                if (pre1 == pre2) continue;
                if (match(pre1, pre2)) {//如果可以合并
                    save(pre1, pre2, sets);
                    preMerge.remove(pre2);
                    j--;
                    flag = true;
                }
            }
            if (flag) merge(pre1);
            preMerge.remove(pre1);
            i--;
            postMerge.add(pre1);
        }



        result = postMerge;

    }

    private void createSet(int max) {
        sets=new ArrayList<HashSet<String>>();
        for( int k = 0; k<max;k++)
            sets.add(new HashSet<String>());
    }




    public static boolean match(List<Dynamic> pa, List<Dynamic> pb) {
        if (pa==null || pb==null) return false;
        if (pa.size() != pb.size()) return false;
        for (int i=0; i<pa.size(); i++) {
            if (!pa.get(i).getContinuousDynamics().equals(pb.get(i).getContinuousDynamics())){
                return false;
            }
        }
        return true;
    }

    public void save(List<Dynamic> pa, List<Dynamic> pb, List<HashSet<String>> sets) {
        for (int i=0; i<pa.size(); i++) {
            sets.get(i).add(pa.get(i).getDiscreteDynamics());
            sets.get(i).add(pb.get(i).getDiscreteDynamics());
        }
    }

    public void merge(List<Dynamic> path) {
        for (int i=0; i<path.size(); i++) {
            List<String> dynamics = new ArrayList<String>();
            dynamics.addAll(sets.get(i));
            path.get(i).setDiscreteDynamics(mergeDynamics(dynamics));
        }
    }

    public static String mergeDynamics(List<String> dynamics){
        StringBuilder sb = new StringBuilder();
        if (dynamics.size()==1) return dynamics.get(0);
        else if (dynamics.size()==0) return null;
        else {
            sb.append(String.format("(or (and %s) (and %s))", dynamics.remove(0), mergeDynamics(dynamics)));
            return sb.toString();
        }
    }

    public List<List<Dynamic>> getMergeResult(){

        return result;
    }

}
