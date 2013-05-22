package cz.muni.fi.logger;

import cz.muni.fi.json.JSONer;

public abstract class AbstractNamespace {
    
    protected static String getJson(Object... paramValues) {
        //the following will never be called if the aspect overrides this method the way it should; just to be safe
        return JSONer.getEventJson("", "", new String[]{}, paramValues);
    }
}
