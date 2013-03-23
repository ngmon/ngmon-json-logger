package cz.muni.fi.sampleproject;

import cz.muni.fi.logger.Logger;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        Logger.debug(EntityXY.class, SampleNamespace.event1("abc", 1));
        Logger.debug(EntityXY.class, SampleNamespace.event2(4, 5, true)); //will raise an error: 
            //EntityXY does not have any enum annotated with @SourceNamespace("cz.muni.fi.sampleproject.SampleNamespace")
            //  s.t. it contains "event2"
        Logger.debug(null, SampleNamespace.event1("abc", 1)); //if (entity == null) do not log anything
    }
}
