package org.apache.log4j;

/**
 * A stubbed mockup of log4j, so we can run tests using log4j without actually
 * including the full library.
 */
public class Logger {

    public static Logger getLogger(Class clazz) {
        // empty on purpose.
        return null;
    }

    public static Logger getLogger(String name) {
     // empty on purpose.
        return null;
    }

    public void info(String string) {
     // empty on purpose.
        
    }

}
