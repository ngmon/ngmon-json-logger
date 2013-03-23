package cz.muni.fi.logger;

import cz.muni.fi.json.JSONer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

public abstract class Logger {
    
    private static org.apache.logging.log4j.Logger LOG = LogManager.getLogger("");
    
    public static void fatal(Class entityClass, String json) {
        log(Level.FATAL, entityClass, json);
    }
    
    public static void error(Class entityClass, String json) {
        log(Level.ERROR, entityClass, json);
    }
    
    public static void warn(Class entityClass, String json) {
        log(Level.WARN, entityClass, json);
    }
    
    public static void info(Class entityClass, String json) {
        log(Level.INFO, entityClass, json);
    }
    
    public static void debug(Class entityClass, String json) {
        log(Level.DEBUG, entityClass, json);
    }
    
    public static void trace(Class entityClass, String json) {
        log(Level.TRACE, entityClass, json);
    }
    
    public static void log(Class entityClass, String json) {
        debug(entityClass, json);
    }
    
    private static void log(Level level, Class entityClass, String json) {
        if (entityClass != null) {
            String eventJson = JSONer.addEntityToEventJson(entityClass.getSimpleName(), json);
            
            switch (level) {
                case FATAL: LOG.fatal(eventJson); break;
                case ERROR: LOG.error(eventJson); break;
                case WARN:  LOG.warn(eventJson);  break;
                case INFO:  LOG.info(eventJson);  break;
                case DEBUG: LOG.debug(eventJson); break;
                case TRACE: LOG.trace(eventJson); break;
            }
        }
    }
}
