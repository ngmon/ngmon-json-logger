package cz.muni.fi.logger;

import cz.muni.fi.json.JSONer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class LogMethodInfoAspect {
    
    @Pointcut("within(cz.muni.fi.logger.AbstractNamespace+) && ! within(cz.muni.fi.logger.AbstractNamespace) && execution(* *(..))")
    public void allMethodsInNamespaces() {}
    
    @Around("allMethodsInNamespaces()")
    public String aroundAllMethodsInNamespaces(ProceedingJoinPoint thisJoinPoint) {
        MethodSignature method = (MethodSignature) thisJoinPoint.getSignature();
        return JSONer.getEventJson(method.getDeclaringTypeName(), method.getName(), method.getParameterNames(), thisJoinPoint.getArgs());
    }
}
