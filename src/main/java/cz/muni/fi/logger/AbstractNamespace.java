package cz.muni.fi.logger;

import cz.muni.fi.json.JSONer;

public abstract class AbstractNamespace {
    
    protected static String getJson(Object... paramValues) {
        //toto volanie je tu len pre istotu - nemalo by sa to sem nikdy dostat, lebo to volanie kompletne prekryje aspect
        return JSONer.getEventJson("", "", new String[]{}, paramValues);
    }
}
