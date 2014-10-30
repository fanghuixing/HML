package IMP.Check;

import DataSet.ParseJSONData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.net.URI;


/**
 * HML IMP.Check
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-10-22.
 */
public class Util {
    private static Logger logger = LogManager.getLogger(Util.class.getName());

    public static void viewDataInBrowser(String path) {
        File f = new File(path+".json");
        //change name and transfer into ODE_visualization
        f.renameTo(new File("/home/fofo/work/ODE_visualization/data.json"));
        URI uri= null;
        try {
            uri = new URI("http://localhost:9801/");
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    public static void viewDataInWindow(String path) throws Exception {

        ParseJSONData.showData(path+".json");



    }


}
