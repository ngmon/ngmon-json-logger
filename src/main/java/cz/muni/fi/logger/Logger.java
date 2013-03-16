package cz.muni.fi.logger;

import cz.muni.fi.json.JSONer;
import org.apache.logging.log4j.LogManager;

public abstract class Logger {
    
    private static org.apache.logging.log4j.Logger LOG = LogManager.getLogger("");
    
    public static void log(String entity, String json) {
        String eventJson = JSONer.addEntityToEventJson(entity, json);
        
        LOG.debug(eventJson); //TODO Level?
    }
}
