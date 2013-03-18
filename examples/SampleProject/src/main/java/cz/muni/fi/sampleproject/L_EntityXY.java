package cz.muni.fi.sampleproject;

import cz.muni.fi.annotation.SourceNamespace;
import cz.muni.fi.logger.Logger;

public class L_EntityXY extends Logger {
    
    @SourceNamespace("cz.muni.fi.sampleproject.SampleNamespace")
    private enum SampleNamespaceEvents {
        event1, abc
    }
    
    @SourceNamespace("a.b.c.d.NamespaceX")
    private enum NoNamespaceEvents { //will not have any effect, since this @SourceNamespace does not exist
        eventX
    }
    
    private enum MyEnum { //no @SourceNamespace - ignored by the processor
        X, Y, Z
    }
    
    
    private static final String ENTITY_NAME = "EntityXY";
    
    public static void log(LEVEL level, String json) {
        log(level, ENTITY_NAME, json);
    }
}
