import java.awt.event.ActionEvent;

import org.apache.log4j.Logger;


public class Log4jBugs {

    private static Logger l1 = Logger.getLogger(Log4jBugs.class);

    private static Logger l2 = Logger.getLogger("Log4jBugs");

    private static Logger l3 = Logger.getLogger(Log4jBugs.class.getName());

    public Log4jBugs() {
        l1.info("foo");
        l2.info("bar");
        l3.info("baz");
    }
}
