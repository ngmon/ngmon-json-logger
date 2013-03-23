package cz.muni.fi.ast;

import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.Element;

public class MethodInvocationInfo {
    
    private String object; //Logger
    private String methodName; //[fatal|error|warn|info|debug|trace|log]
    private String arg0Entity; //e.g. Entity
    private String arg1Object; //e.g. SampleNamespace
    private String arg1MethodName; //e.g. eventType1
    private Element methodElement;
    private MethodInvocationTree methodTree;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }
    
    public String getArg0Entity() {
        return arg0Entity;
    }

    public void setArg0Entity(String arg0Entity) {
        if (arg0Entity.endsWith(".class")) {
            this.arg0Entity = arg0Entity.substring(0, arg0Entity.length() - 6);
        } else {
            this.arg0Entity = arg0Entity;
        }
    }

    public String getArg1Object() {
        return arg1Object;
    }

    public void setArg1Object(String argObject) {
        this.arg1Object = argObject;
    }

    public String getArg1MethodName() {
        return arg1MethodName;
    }

    public void setArg1MethodName(String argMethodName) {
        this.arg1MethodName = argMethodName;
    }
    
    public Element getMethodElement() {
        return methodElement;
    }

    public void setMethodElement(Element methodElement) {
        this.methodElement = methodElement;
    }
    
    public MethodInvocationTree getMethodTree() {
        return methodTree;
    }

    public void setMethodTree(MethodInvocationTree methodTree) {
        this.methodTree = methodTree;
    }
    
    @Override
    public String toString() {
        return object + "." + methodName + "(" + arg0Entity + ", " + arg1Object + "." + arg1MethodName + "())";
    }
}
