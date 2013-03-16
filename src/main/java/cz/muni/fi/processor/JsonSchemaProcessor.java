package cz.muni.fi.processor;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import cz.muni.fi.annotation.Namespace;
import cz.muni.fi.annotation.SourceNamespace;
import cz.muni.fi.ast.MethodInvocationInfo;
import cz.muni.fi.ast.MethodInvocationScanner;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class JsonSchemaProcessor extends AbstractProcessor {
    
    //TODO vycistit + komentare
    
    private Filer filer;
    private Messager messager;
    
    private static final String EVENTS_BASE_PKG = "EVENTS";
    private String configPath = "src" + File.separatorChar + "main" + File.separatorChar + "resources" + File.separatorChar + "config.properties";
    
    private long lastBuildTime = 0;
    private boolean firstRound = true;
    private List<Element> schemasToGenerate = new ArrayList<>();
    private List<String> newClasses = new ArrayList<>();

    private Trees trees;
    private Elements elements;
    
    @Override
    public void init(ProcessingEnvironment env) {
        filer = env.getFiler();
        messager = env.getMessager();
        
        trees = Trees.instance(env);
        elements = env.getElementUtils();
        
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(configPath));
            lastBuildTime = Long.parseLong(properties.getProperty("lastBuildTime"));
        } catch (IOException ex) {
        }
    }

    @Override
    public boolean process(Set annotations, RoundEnvironment env) {
        if (! env.processingOver()) {
            if (firstRound) {
                List<String> namespaces = new ArrayList<>();
                for (Element element : env.getElementsAnnotatedWith(Namespace.class)) {
                    String elementFQN = getFQN(element);
                    namespaces.add(elementFQN);
                    //process only classes changed since the last build
                    String path = "src" + File.separatorChar + "main" + File.separatorChar + "java" + File.separatorChar
                        + elementFQN.replace('.', File.separatorChar) + ".java";
                    Path p = FileSystems.getDefault().getPath(path);
                    long lastModified;
                    try {
                        lastModified = Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException ex) {
                        lastModified = new Date().getTime();
                    }
                    
                    if (lastModified > lastBuildTime) {
                        //TODO opravit to kontrolovanie pretazenych metod nizsie, aby sme to nemuseli prechadzat zbytocne aj tu?
                        Set<String> methods = new HashSet<>();
                        boolean add = true;
                        for (Element elem : element.getEnclosedElements()) {
                            if (elem.getKind() == ElementKind.METHOD) {
                                ExecutableElement method = (ExecutableElement) elem;
                                String methodName = method.getSimpleName().toString();

                                //forbid method overloading
                                if (methods.contains(methodName)) {
                                    messager.printMessage(Diagnostic.Kind.ERROR, "Method overloading not allowed here", elem);
                                    add = false;
                                    break;
                                } else {
                                    methods.add(methodName);
                                }
                            }
                        }

                        if (add) {
                            schemasToGenerate.add(element);
                        }
                    }
                }
                
                for (Element element : env.getRootElements()) {
                    CompilationUnitTree compUnit = trees.getPath(element).getCompilationUnit();
                    List<MethodInvocationInfo> methodsInfo = new ArrayList<>();
                    for (Element e : element.getEnclosedElements()) {
                        Tree methodAST = trees.getTree(e);
                        MethodInvocationScanner scanner = new MethodInvocationScanner();
                        scanner.scan(methodAST, e);
                        methodsInfo.addAll(scanner.getMethodsInvocationInfo());
                    }

                    @SuppressWarnings("unchecked")
                    List<ImportTree> classImports = (List<ImportTree>) compUnit.getImports();
                    String classFQN = getFQN(element);

                    for (MethodInvocationInfo methodInfo : methodsInfo) {
                        if (methodInfo.getObject().endsWith(")") || methodInfo.getArgObject().endsWith(")")) {
                            continue;
                        }
                        
                        String argObjSimpleName;
                        if (methodInfo.getArgObject().lastIndexOf('.') == -1) {
                            argObjSimpleName = methodInfo.getArgObject();
                        } else {
                            argObjSimpleName = methodInfo.getArgObject().substring(methodInfo.getArgObject().lastIndexOf('.') + 1);
                        }
                        if (! containsName(namespaces, argObjSimpleName)) {
                            continue;
                        }
                        
                        //get fqn of methodInfo.object and methodInfo.argObject
                        String fqnObject = "";
                        String fqnArgObject = "";
                        
                        if (methodInfo.getObject().contains(".")) {
                            fqnObject = methodInfo.getObject();
                        }
                        if (methodInfo.getArgObject().contains(".")) {
                            fqnArgObject = methodInfo.getArgObject();
                        }
                        
                        if (fqnObject.equals("") || fqnArgObject.equals("")) {
                            List<String> asteriskImports = new ArrayList<>();
                            //check for direct imports
                            for (ImportTree imp : classImports) {
                                String importt = imp.getQualifiedIdentifier().toString();
                                
                                if (importt.endsWith(methodInfo.getObject())) {
                                    fqnObject = importt;
                                }
                                
                                if (importt.endsWith(methodInfo.getArgObject())) {
                                    fqnArgObject = importt;
                                }
                                
                                if (importt.endsWith("*")) {
                                    asteriskImports.add(importt);
                                }
                            }
                            
                            if (fqnObject.equals("") || fqnArgObject.equals("")) {
                                if (asteriskImports.isEmpty()) { //no direct import AND no package imports -> the same package
                                    if (fqnObject.equals("")) {
                                        fqnObject = classFQN.substring(0, classFQN.lastIndexOf('.')) + "." + methodInfo.getObject();
                                    }
                                    if (fqnArgObject.equals("")) {
                                        fqnArgObject = classFQN.substring(0, classFQN.lastIndexOf('.')) + "." + methodInfo.getArgObject();
                                    }
                                } else {
                                    boolean oneAlreadyFound = false;
                                    for (String importt : asteriskImports) {
                                        String path = "src" + File.separator + "main" + File.separator + "java" + File.separator
                                            + importt.substring(0, importt.length() - 1).replace('.', File.separatorChar);
                                        if (fqnObject.equals("")) {
                                            if (Files.exists(FileSystems.getDefault().getPath(path + methodInfo.getObject() + ".java"))) {
                                                fqnObject = importt.substring(0, importt.length() - 1) + methodInfo.getObject();
                                                if (oneAlreadyFound) {
                                                    break;
                                                } else {
                                                    oneAlreadyFound = true;
                                                }
                                            }
                                        }
                                        if (fqnArgObject.equals("")) {
                                            if (Files.exists(FileSystems.getDefault().getPath(path + methodInfo.getArgObject() + ".java"))) {
                                                fqnArgObject = importt.substring(0, importt.length() - 1) + methodInfo.getArgObject();
                                                if (oneAlreadyFound) {
                                                    break;
                                                } else {
                                                    oneAlreadyFound = true;
                                                }
                                            }
                                        }
                                    }
                                    
                                    //still not found -> the same package
                                    if (fqnObject.equals("")) {
                                        fqnObject = classFQN.substring(0, classFQN.lastIndexOf('.')) + "." + methodInfo.getObject();
                                    }
                                    
                                    if (fqnArgObject.equals("")) {
                                        fqnArgObject = classFQN.substring(0, classFQN.lastIndexOf('.')) + "." + methodInfo.getArgObject();
                                    }
                                }
                            }
                        }
                        
                        //check if class fqnObject has enum with @SourceNS(fqnArgObject) that contains the method in question
                        TypeElement entityClass = elements.getTypeElement(fqnObject);
                        boolean supported = true;
                        for (Element elem : entityClass.getEnclosedElements()) {
                            if (elem.getKind() == ElementKind.ENUM) {
                                if (elem.getAnnotation(SourceNamespace.class) != null) {
                                    String sourceNS = elem.getAnnotation(SourceNamespace.class).value();
                                    if (sourceNS.equals(fqnArgObject)) {
                                        boolean found = false;
                                        for (Element el : elem.getEnclosedElements()) {
                                            if (el.toString().equals(methodInfo.getArgMethodName())) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            supported = false;
                                        }
                                    }
                                }
                            }
                        }

                        if (!supported) {
                            compUnit = trees.getPath(methodInfo.getMethodElement()).getCompilationUnit();
                            long start = trees.getSourcePositions().getStartPosition(compUnit, methodInfo.getMethodTree());
                            LineMap lineMap = compUnit.getLineMap();
                            messager.printMessage(Diagnostic.Kind.ERROR, "Line " + lineMap.getLineNumber(start) + ": Method '" + methodInfo.getArgMethodName() + "' ("
                                    + fqnArgObject + ") not supported by '" + methodInfo.getObject() + "'", methodInfo.getMethodElement());
                        }
                    }
                }
                
                generateNamespaces(namespaces);
                firstRound = false;
            }
        } else { //last round - generate schemas for all classes (except for those that have just been created)
            for (Element element : schemasToGenerate) {
                String pack = element.getEnclosingElement().toString();
                String schemaName = element.getSimpleName().toString();
                
                if (newClasses.contains(pack + "." + schemaName)) {
                    continue;
                }
                
                try {
                    String path = "src" + File.separatorChar + "main" + File.separatorChar + "resources" + File.separatorChar + 
                            EVENTS_BASE_PKG + File.separatorChar + pack.replace('.', File.separatorChar) + File.separatorChar + 
                            schemaName + ".json";
                    Path file = FileSystems.getDefault().getPath(path);
                    
                    //create non-existing directories
                    Files.createDirectories(file.getParent());
                    
                    JsonFactory jsonFactory = new JsonFactory();
                    try (JsonGenerator schema = jsonFactory.createGenerator(file.toFile(), JsonEncoding.UTF8)) {
                        schema.useDefaultPrettyPrinter();
                        
                        schema.writeStartObject();
                        schema.writeStringField("$schema", "http://json-schema.org/schema#");
                        schema.writeStringField("title", schemaName);
                        schema.writeStringField("type", "object");
                        
                        schema.writeArrayFieldStart("oneOf");
                        
                        Map<String, Map<String,String>> methods = new HashMap<>();
                        for (Element e : element.getEnclosedElements()) {
                            if (e.getKind() == ElementKind.METHOD) {
                                ExecutableElement method = (ExecutableElement) e;
                                String methodName = method.getSimpleName().toString();
                                
                                schema.writeStartObject();
                                schema.writeStringField("$ref", "#/definitions/" + methodName);
                                schema.writeEndObject(); //end {"$ref":"#/definitions/GET"}
                                
                                //get parameter names and types
                                Map<String, String> params = new HashMap<>();
                                for (VariableElement param : method.getParameters()) {
                                    String typeFull = param.asType().toString();
                                    String type = typeFull.substring(typeFull.lastIndexOf(".") + 1);
                                    switch (type) {
                                        case "String":
                                            type = "string";
                                            break;
                                        case "int":
                                            type = "integer";
                                            break;
                                        case "double":
                                        case "float":
                                            type = "number";
                                            break;
                                        case "boolean":
                                            type = "boolean";
                                            break;
                                        default:
                                            type = "object";
                                    }
                                    
                                    params.put(param.getSimpleName().toString(), type);
                                }
                                
                                methods.put(methodName, params);
                            }
                        }
                        
                        schema.writeEndArray(); //end oneOf
                        schema.writeObjectFieldStart("definitions");
                        
                        for (Map.Entry<String, Map<String, String>> method : methods.entrySet()) {
                            schema.writeObjectFieldStart(method.getKey());
                            schema.writeObjectFieldStart("properties");
                            for (Map.Entry<String, String> param : method.getValue().entrySet()) {
                                schema.writeObjectFieldStart(param.getKey());
                                schema.writeStringField("type", param.getValue());
                                schema.writeEndObject(); //end param
                            }
                            schema.writeEndObject(); //end properties
                            schema.writeArrayFieldStart("required");
                            //TODO neprechadzat zbytocne znovu, ale poznacit si to uz pri vypisovani properties?
                            for (String param : method.getValue().keySet()) {
                                schema.writeString(param);
                            }
                            schema.writeEndArray(); //end required
                            schema.writeBooleanField("additionalProperties", false);
                            schema.writeEndObject(); //end method
                        }
                        
                        schema.writeEndObject(); //end of definitions
                        schema.writeEndObject(); //end of schema
                        
                    }
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Error writing file " + schemaName + ".json");
                }
            }
            
            //update lastBuildTime (used for tracking changes in class files)
            Properties properties = new Properties();
            properties.setProperty("lastBuildTime", String.valueOf(new Date().getTime()));
            try {
                Files.createDirectories(FileSystems.getDefault().getPath(configPath).getParent());
                properties.store(new FileWriter(configPath), null);
            } catch (IOException ex) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Error writing file " + configPath);
            }
        }
        
        return true;
    }
    
    private String getFQN(Element classElement) {
        return classElement.getEnclosingElement().toString() + "." + classElement.getSimpleName().toString();
    }

    private boolean containsName(List<String> namespaces, String argObjSimpleName) {
        for (String ns : namespaces) {
            if ((ns.equals(argObjSimpleName)) || (ns.endsWith("." + argObjSimpleName))) {
                return true;
            }
        }
        
        return false;
    }

    private void generateNamespaces(final List<String> existingNSs) {
        final String schemasDir = "src" + File.separator + "main" + File.separator + "resources" + File.separator + EVENTS_BASE_PKG;
        try {
            Files.walkFileTree(FileSystems.getDefault().getPath(schemasDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    String filename = path.getFileName().toString();
                    
                    if (filename.toLowerCase().endsWith(".json")) {
                        String classPackage = path.getParent().toString().substring(schemasDir.length() + 1).replace(File.separatorChar, '.');
                        
                        if (existingNSs.contains(classPackage + "." + filename.substring(0, filename.length()-5))) {
                            return FileVisitResult.CONTINUE;
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        
                        try {
                            JsonNode schemaRoot = mapper.readTree(path.toFile());

                            String className = filename.substring(0, 1).toUpperCase() + filename.substring(1, filename.length() - 5);
                            
                            StringBuilder classBeginning = new StringBuilder();
                            if (! classPackage.equals("")) {
                                classBeginning.append("package ").append(classPackage).append(";\n\n");
                            }
                            
                            classBeginning.append("import cz.muni.fi.annotation.Namespace;\n")
                                    .append("import cz.muni.fi.json.JSONer;\n");
                            
                            StringBuilder classContent = new StringBuilder();
                            classContent.append("\n@Namespace\npublic class ").append(className).append(" {\n")
                                    .append("\n    private static final String FQN = ").append(className).append(".class.getCanonicalName();\n");
                            
                            JsonNode definitions = schemaRoot.get("definitions");
                            //delete schemas with no methods
                            if (definitions.size() == 0) {
                                Files.delete(path);
                                return FileVisitResult.CONTINUE;
                            }

                            //generate methods
                            Iterator<String> methodsIterator = definitions.fieldNames();
                            while (methodsIterator.hasNext()) {
                                String methodName = methodsIterator.next();
                                classContent.append("\n    public static String ").append(methodName).append("(");
                                JsonNode method = definitions.get(methodName);
                                JsonNode parameters = method.get("properties");
                                Iterator<String> paramsIterator = parameters.fieldNames();
                                boolean putComma = false;
                                List<String> paramNames = new ArrayList<>();
                                while (paramsIterator.hasNext()) {
                                    if (putComma) {
                                        classContent.append(", ");
                                    } else {
                                        putComma = true;
                                    }
                                    String paramName = paramsIterator.next();
                                    paramNames.add(paramName);
                                    JsonNode param = parameters.get(paramName);
                                    switch (param.get("type").textValue()) {
                                        case "string":
                                            classContent.append("String ");
                                            break;
                                        case "integer":
                                            classContent.append("int ");
                                            break;
                                        case "number":
                                            classContent.append("double ");
                                            break;
                                        case "boolean":
                                            classContent.append("boolean ");
                                            break;
                                        default:
                                            classContent.append("Object ");
                                    }
                                    classContent.append(paramName);
                                }
                                classContent.append(") {\n        return JSONer.getEventJson(FQN, \"").append(methodName)
                                        .append("\", new String[]{");
                                putComma = false;
                                for (int k = 0; k < paramNames.size(); k++) {
                                    if (putComma) {
                                        classContent.append(",");
                                    } else {
                                        putComma = true;
                                    }

                                    classContent.append("\"").append(paramNames.get(k)).append("\"");
                                }
                                classContent.append("}"); //end String[]{}
                                //method params:
                                for (int k = 0; k < paramNames.size(); k++) {
                                    classContent.append(", ").append(paramNames.get(k));
                                }
                                classContent.append(");\n    }\n"); //end method
                            }
                            
                            classContent.append("}\n"); //end class

                            JavaFileObject file = filer.createSourceFile(classPackage + "." + className);

                            file.openWriter().append(classBeginning).append(classContent).close();

                            newClasses.add(classPackage + "." + className);
                        } catch (IOException ex) {
                            messager.printMessage(Diagnostic.Kind.ERROR, filename + ": unexpected format of schema");
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            //TODO
        }
    }
}