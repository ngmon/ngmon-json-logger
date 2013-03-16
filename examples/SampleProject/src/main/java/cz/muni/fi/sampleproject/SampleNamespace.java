package cz.muni.fi.sampleproject;

import cz.muni.fi.annotation.Namespace;
import cz.muni.fi.json.JSONer;

@Namespace
public class SampleNamespace {

    private static final String FQN = SampleNamespace.class.getCanonicalName();

    public static String event1(String param1, int param2) {
        return JSONer.getEventJson(FQN, "event1", new String[]{"param1","param2"}, param1, param2);
    }

    public static String event2(double a, double b, boolean c) {
        return JSONer.getEventJson(FQN, "event2", new String[]{"a","b","c"}, a, b, c);
    }
}
