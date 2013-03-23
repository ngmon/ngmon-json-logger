package cz.muni.fi.ast;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;

public class MethodInvocationScanner extends TreeScanner<Void, Element> {
    
    private List<MethodInvocationInfo> methodsInfo = new ArrayList<>();
    
    public List<MethodInvocationInfo> getMethodsInvocationInfo() {
        return this.methodsInfo;
    }
    
    @Override
    public Void visitMethodInvocation(MethodInvocationTree methodTree, Element element) {
        MethodInvocationInfo methodInfo = new MethodInvocationInfo();
        ExpressionTree methodSelect = methodTree.getMethodSelect();
        if (methodSelect.getKind() == Tree.Kind.MEMBER_SELECT) {
            String methodName = ((MemberSelectTree) methodSelect).getIdentifier().toString();
            String object = ((MemberSelectTree) methodSelect).getExpression().toString();
            if (methodTree.getArguments().size() == 2) {
                //Entity.class
                ExpressionTree arg0 = methodTree.getArguments().get(0); //Entity.class
                
                //Namespace.eventType(args)
                ExpressionTree arg1 = methodTree.getArguments().get(1);
                if (arg1.getKind() == Tree.Kind.METHOD_INVOCATION) {
                    MethodInvocationTree argMethodTree = (MethodInvocationTree) arg1;
                    ExpressionTree argMethodSelect = argMethodTree.getMethodSelect();
                    if (argMethodSelect.getKind() == Tree.Kind.MEMBER_SELECT) {
                        methodInfo.setMethodName(methodName);
                        methodInfo.setObject(object);
                        methodInfo.setArg0Entity(arg0.toString());
                        String argMethodName = ((MemberSelectTree) argMethodSelect).getIdentifier().toString();
                        String argObject = ((MemberSelectTree) argMethodSelect).getExpression().toString();
                        methodInfo.setArg1MethodName(argMethodName);
                        methodInfo.setArg1Object(argObject);
                        methodInfo.setMethodElement(element);
                        methodInfo.setMethodTree(methodTree);
                        methodsInfo.add(methodInfo);
                    }
                } //inak nas to nezaujima zatial
            }
        } //else volanie lokalnej metody, nezaujima ma
        
        return super.visitMethodInvocation(methodTree, element); //zostup aj na uzly potomkov
    }
}
