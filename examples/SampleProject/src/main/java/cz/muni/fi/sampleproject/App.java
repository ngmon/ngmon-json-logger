package cz.muni.fi.sampleproject;

import cz.muni.fi.logger.Logger.LEVEL;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        L_EntityXY.log(LEVEL.INFO, SampleNamespace.event1("abc", 1));
        L_EntityXY.log(LEVEL.ERROR, SampleNamespace.event2(1.2, 3.4f, true)); //will raise an error: 
            //L_EntityXY does not have any enum annotated with @SourceNamespace("cz.muni.fi.sampleproject.SampleNamespace")
            //  s.t. it contains "event2"
    }
}
