package cz.muni.fi.logger;

import cz.muni.fi.json.JSONer;
import org.apache.logging.log4j.LogManager;

public abstract class Logger {
    
    private static org.apache.logging.log4j.Logger LOG = LogManager.getLogger("");
    
    public enum LEVEL {
        FATAL, ERROR, WARN, INFO, DEBUG, TRACE
    }
    
    protected static void log(LEVEL level, String entity, String json) {
        String eventJson = JSONer.addEntityToEventJson(entity, json);
        
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
