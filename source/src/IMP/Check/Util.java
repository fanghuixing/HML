package IMP.Check;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * HML IMP.Check
 * Created by fofo on 14-10-22.
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

}
