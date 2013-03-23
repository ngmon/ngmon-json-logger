package cz.muni.fi.sampleproject;

import cz.muni.fi.annotation.Namespace;
import cz.muni.fi.logger.AbstractNamespace;

@Namespace
public class SampleNamespace extends AbstractNamespace {

    public static String event1(String param1, int param2) {
        return AbstractNamespace.getJson(param1, param2);
    }

    public static String event2(double a, double b, boolean c) {
        return AbstractNamespace.getJson(a, b, c);
    }
}
