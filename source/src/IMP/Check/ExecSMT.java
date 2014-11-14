package IMP.Check;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ExecSMT {
    private static String dReal = "/home/fofo/dReal21408/bin/dReal";
    private static Logger logger = LogManager.getLogger(ExecSMT.class.getName());
    public static boolean exec(String precision, String modelFilePath){
        StringBuffer sb = new StringBuffer();
        sb.append(dReal);
        if (precision!=null && precision.length()>0)
            sb.append(" --precision=").append(precision);

        if (modelFilePath!=null && modelFilePath.length()>0)
            sb.append(" ").append(modelFilePath);

        sb.append(" --ode_parallel --ode_cache  --delta_heuristic --short_sat --ode_grid=1024");
        //--ode_parallel --ode_cache  --delta_heuristic --short_sat
        //--precision=0.1 --visualize --ode_parallel --ode_cache  --delta_heuristic --short_sat --ode_grid=1024

        logger.info("Exec " + sb.toString());
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        InputStream in = null;
        InputStream err = null;
        try {
            proc = runtime.exec(sb.toString());


            in = proc.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String result = br.readLine();

            if (result!=null && result.equals("sat")) {
                logger.info("The result is : " + result);
                return true;
            }
            else if (result!=null && result.equals("unsat")) {
                logger.info("The result is : " + result);
            }
            else {
                logger.error("Error in dReal Running");
                err = proc.getErrorStream();
                BufferedReader brErr = new BufferedReader(new InputStreamReader(err));
                String error = brErr.readLine();
                if (error == null) logger.debug("No error info ");
                while (error != null) {
                    logger.info("The errors : " + error);
                    error = brErr.readLine();
                }

            }
            return false;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        } finally {
            if (proc != null) proc.destroy();
            try {
                if (in != null) in.close();
                if (err != null) err.close();
            }catch (IOException e) {
                logger.error("Error when closing input/error stream: " + e.getMessage());
            }
        }
    }


}