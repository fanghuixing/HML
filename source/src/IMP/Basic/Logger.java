package IMP.Basic;

/**
 * Created by fofo on 2014/9/26.
 */
public class Logger {
    private static Logger ourInstance = new Logger();

    public static Logger getInstance() {
        return ourInstance;
    }

    private Logger() {
    }

    public void log(String msg){
        System.out.println(msg);
    }
}
