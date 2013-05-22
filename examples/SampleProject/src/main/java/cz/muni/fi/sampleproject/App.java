package cz.muni.fi.sampleproject;

import cz.muni.fi.logger.Logger;
import logger.EntityA;
import logger.x.y.EntityC;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        Logger.initLoggers("EntityA","x.y.EntityC");
//        Logger.initAll();
        
        EntityA entityA = new EntityA();
        entityA.method1("abc", true).error();
        
        EntityC entityC = new EntityC();
        entityC.method3("abc", "def", "ghi").log();
    }
}
