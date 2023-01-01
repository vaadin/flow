package com.vaadin.base.devserver.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class Editor {

    public static class Modification implements Comparable<Modification> {

        private enum Type {
            INSERT_AFTER, INSERT_BEFORE, INSERT_LINE_AFTER, REPLACE
        };

        private Node referenceNode;
        private Type type;
        private String code;

        public String apply(String source) {
            if (type == Type.INSERT_LINE_AFTER) {
                int insertPoint = sourcePosition(source,
                        referenceNode.getEnd().get().nextLine());
                return source.substring(0, insertPoint) + code
                        + source.substring(insertPoint);
            } else if (type == Type.INSERT_AFTER) {
                int insertPoint = sourcePosition(source,
                        referenceNode.getEnd().get().right(1));
                return source.substring(0, insertPoint) + code
                        + source.substring(insertPoint);
            } else if (type == Type.INSERT_BEFORE) {
                int insertPoint = sourcePosition(source,
                        referenceNode.getBegin().get());
                return source.substring(0, insertPoint) + code
                        + source.substring(insertPoint);
            } else if (type == Type.REPLACE) {
                int nodeStart = sourcePosition(source,
                        referenceNode.getRange().get().begin);
                int nodeEnd = sourcePosition(source,
                        referenceNode.getRange().get().end);
                return source.substring(0, nodeStart) + code
                        + source.substring(nodeEnd + 1);
            }
            throw new RuntimeException("Unknown type");
        }

        private static int sourcePosition(String source, Position pos) {
            // javaparse lines are 1-based
            int lines = pos.line - 1;
            int sourcePos = 0;
            while (lines > 0) {
                sourcePos = source.indexOf("\n", sourcePos + 1);
                lines--;
            }
            sourcePos += pos.column;
            return sourcePos;
        }

        public static Modification insertAfter(Node node, String code) {
            Modification mod = new Modification();
            mod.referenceNode = node;
            mod.type = Type.INSERT_AFTER;
            mod.code = code;
            return mod;
        }

        public static Modification insertBefore(Node node, String code) {
            Modification mod = new Modification();
            mod.referenceNode = node;
            mod.type = Type.INSERT_BEFORE;
            mod.code = code;
            return mod;
        }

        public static Modification insertLineAfter(Node node, String code) {
            Modification mod = new Modification();
            mod.referenceNode = node;
            mod.type = Type.INSERT_LINE_AFTER;
            mod.code = code;
            return mod;
        }

        public static Modification replace(Node node, String code) {
            Modification mod = new Modification();
            mod.referenceNode = node;
            mod.type = Type.REPLACE;
            mod.code = code;
            return mod;
        }

        public static Modification replace(Node node, Node code) {
            return replace(node, code.toString());
        }

        @Override
        public int compareTo(Modification o) {
            // Sort end to start so positions do not change while replacing

            int a = referenceNode.getRange().get().begin.line;
            int b = o.referenceNode.getRange().get().begin.line;
            return Integer.compare(b, a);
        }

        @Override
        public String toString() {
            if (type == Type.INSERT_LINE_AFTER) {
                return "Modification INSERT_LINE_AFTER at position "
                        + referenceNode.getEnd().get() + ": " + code;
            } else if (type == Type.INSERT_AFTER) {
                return "Modification INSERT_AFTER at position "
                        + referenceNode.getEnd().get() + ": " + code;
            } else if (type == Type.INSERT_BEFORE) {
                return "Modification INSERT_BEFORE at position "
                        + referenceNode.getBegin().get() + ": " + code;
            } else if (type == Type.REPLACE) {
                return "Modification REPLACE position "
                        + referenceNode.getBegin().get() + "-"
                        + referenceNode.getEnd().get() + ": " + code;
            }
            return "Modification UNKNOWN TYPE";
        }

    }

    private List<Modification> modifyOrAddCall(CompilationUnit cu,
            int componentInstantiationLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParameter) {

        List<Modification> mods = new ArrayList<>();

        Statement node = findNode(cu, componentInstantiationLineNumber);
        SimpleName localVariableOrField = findLocalVariableOrField(cu,
                componentInstantiationLineNumber);
        if (localVariableOrField == null) {
            modifyOrAddCallInlineConstructor(cu, node, componentType,
                    methodName, methodParameter, mods);
            return mods;
        }
        BlockStmt codeBlock = (BlockStmt) node.getParentNode().get();
        boolean handled = false;
        Expression exp = (Expression) localVariableOrField.getParentNode().get()
                .getParentNode().get();
        if (exp.isAssignExpr()) {
            AssignExpr assignExpr = exp.asAssignExpr();
            if (assignExpr.getValue().isObjectCreationExpr()) {
                handled = modifyConstructorCall(
                        assignExpr.getValue().asObjectCreationExpr(),
                        methodName, methodParameter, mods);
            }

            if (!handled) {
                addOrReplaceCall(codeBlock, node, localVariableOrField,
                        methodName, new StringLiteralExpr(methodParameter),
                        mods);
            }
        } else if (exp.isVariableDeclarationExpr()) {
            VariableDeclarationExpr varDeclaration = exp
                    .asVariableDeclarationExpr();
            VariableDeclarator varDeclarator = varDeclaration.getVariable(0);
            Optional<Expression> initializer = varDeclarator.getInitializer();
            if (initializer.isPresent()
                    && initializer.get().isObjectCreationExpr()) {
                ObjectCreationExpr constructorCall = initializer.get()
                        .asObjectCreationExpr();
                if (modifyConstructorCall(constructorCall, methodName,
                        methodParameter, mods)) {
                    handled = true;
                }
            }
            if (!handled) {
                addOrReplaceCall(codeBlock, node, localVariableOrField,
                        methodName, new StringLiteralExpr(methodParameter),
                        mods);
            }
        }

        return mods;

    }

    private void modifyOrAddCallInlineConstructor(CompilationUnit cu,
            Statement componentNode, ComponentType componentType,
            String methodName, String methodParameter,
            List<Modification> mods) {
        if (!componentNode.isExpressionStmt()) {
            return;
        }
        Expression expression = componentNode.asExpressionStmt()
                .getExpression();
        if (expression.isMethodCallExpr()) {
            ObjectCreationExpr constructorCall = findConstructorCallParameter(
                    expression.asMethodCallExpr(), componentType);
            if (constructorCall != null) {
                modifyConstructorCall(constructorCall, methodName,
                        methodParameter, mods);
            }
        }

    }

    private ObjectCreationExpr findConstructorCallParameter(
            MethodCallExpr methodCallExpr, ComponentType componentType) {
        // e.g. add(new Button("foo"))
        List<ObjectCreationExpr> constructorCalls = methodCallExpr
                .getArguments().stream()
                .filter(arg -> arg.isObjectCreationExpr())
                .map(arg -> arg.asObjectCreationExpr())
                .filter(objectCreate -> isConstructorFor(objectCreate,
                        componentType))
                .collect(Collectors.toList());
        if (constructorCalls.size() == 1) {
            // Only one new Button();
            return constructorCalls.get(0);
        } else {
            // e.g. add(new Button("foo"), new Button("bar"));
            // throw new IllegalStateException("Unable to modify");
        }
        return null;
    }

    private SimpleName findLocalVariableOrField(CompilationUnit cu,
            int componentInstantiationLineNumber) {
        Statement node = findNode(cu, componentInstantiationLineNumber);

        if (node.isExpressionStmt()) {
            ExpressionStmt expressionStmt = node.asExpressionStmt();
            Expression expression = expressionStmt.getExpression();
            if (expression.isVariableDeclarationExpr()) {
                VariableDeclarationExpr varDeclaration = expression
                        .asVariableDeclarationExpr();
                VariableDeclarator varDeclarator = varDeclaration
                        .getVariable(0);
                return varDeclarator.getName();
            } else if (expression.isAssignExpr()) {
                AssignExpr assignExpr = expression.asAssignExpr();
                Expression target = assignExpr.getTarget();
                if (target.isNameExpr()) {
                    return target.asNameExpr().getName();
                }
            }
        }
        return null;
    }

    private List<Modification> addComponent(CompilationUnit cu,
            int componentInstantiationLineNumber, int componentAttachLineNumber,
            BeforeOrAfter beforeOrAfter, ComponentType componentType,
            String... constructorArguments) {
        List<Modification> mods = new ArrayList<>();
        Statement attachNode = findNode(cu, componentAttachLineNumber);
        Statement instantiationNode = findNode(cu,
                componentInstantiationLineNumber);

        String code = getConstructorCode(componentType, constructorArguments);

        SimpleName localVariableOrField = findLocalVariableOrField(cu,
                componentInstantiationLineNumber);
        if (localVariableOrField == null && attachNode.equals(instantiationNode)
                && attachNode.isExpressionStmt()) {
            // The reference component is created inline
            Expression attachNodeExpression = attachNode.asExpressionStmt()
                    .getExpression();
            if (attachNodeExpression.isMethodCallExpr()) {
                ObjectCreationExpr referenceComponentAdd = findConstructorCallParameter(
                        attachNodeExpression.asMethodCallExpr(), componentType);
                MethodCallExpr methodCallExpr = attachNodeExpression
                        .asMethodCallExpr();
                NodeList<Expression> args = methodCallExpr.getArguments();
                for (int i = 0; i < args.size(); i++) {
                    if (referenceComponentAdd.equals(args.get(i))) {
                        if (beforeOrAfter == BeforeOrAfter.BEFORE) {
                            mods.add(Modification.insertBefore(args.get(i),
                                    code + ", "));
                        } else {
                            mods.add(Modification.insertAfter(args.get(i),
                                    ", " + code));
                        }
                        break;
                    }
                }
                return mods;
            }
        } else if (localVariableOrField != null
                && attachNode.isExpressionStmt()) {
            Expression expression = attachNode.asExpressionStmt()
                    .getExpression();
            if (expression.isMethodCallExpr()) {
                // e.g. add(foo, bar, baz)
                NodeList<Expression> args = expression.asMethodCallExpr()
                        .getArguments();
                for (int i = 0; i < args.size(); i++) {
                    if (!args.get(i).isNameExpr()) {
                        continue;
                    }
                    SimpleName name = args.get(i).asNameExpr().getName();
                    if (name.equals(localVariableOrField)) {
                        // new ExpressionStmt(new Expression)
                        // ClassOrInterfaceDeclaration type =
                        // cu.getClassByName(componentType.getName()).get();
                        if (beforeOrAfter == BeforeOrAfter.BEFORE) {
                            mods.add(Modification.insertBefore(args.get(i),
                                    code + ", "));
                        } else {
                            mods.add(Modification.insertAfter(args.get(i),
                                    ", " + code));
                        }
                        break;
                    }
                }
            }
        }
        return mods;

    }

    private String getConstructorCode(ComponentType componentType,
            String[] constructorArguments) {
        ClassOrInterfaceType type = StaticJavaParser
                .parseClassOrInterfaceType(componentType.getClassName());
        List<Expression> componentConstructorArgs = Arrays
                .stream(constructorArguments)
                .map(arg -> new StringLiteralExpr(arg))
                .collect(Collectors.toList());
        return new ObjectCreationExpr(null, type,
                new NodeList<>(componentConstructorArgs)).toString();
    }

    private void addOrReplaceCall(BlockStmt codeBlock, Node afterThisNode,
            SimpleName variableName, String methodName,
            Expression methodArgument, List<Modification> mods) {
        NodeList<Expression> arguments = new NodeList<Expression>();
        arguments.add(methodArgument);

        ExpressionStmt setTextCall = new ExpressionStmt(new MethodCallExpr(
                new NameExpr(variableName), methodName, arguments));

        ExpressionStmt existingCall = findMethodCall(codeBlock, afterThisNode,
                variableName, methodName);
        if (existingCall != null) {
            Modification mod = Modification.replace(existingCall,
                    setTextCall.toString());
            mods.add(mod);

        } else {
            String indent = " "
                    .repeat(afterThisNode.getRange().get().begin.column - 1);
            Modification mod = Modification.insertLineAfter(afterThisNode,
                    indent + setTextCall.toString() + ";\n");
            mods.add(mod);
        }

    }

    private boolean modifyConstructorCall(ObjectCreationExpr constructorCall,
            String methodName, String newText, List<Modification> mods) {
        if (methodName.equals("setText")) {
            // Button constructor with a string argument -> replace
            StringLiteralExpr param = findConstructorParameter(constructorCall,
                    ComponentType.BUTTON, 0);
            if (param != null) {
                Modification mod = Modification.replace(param,
                        new StringLiteralExpr(newText));
                mods.add(mod);
                return true;
            }
        }
        if (methodName.equals("setLabel")) {
            StringLiteralExpr param = findConstructorParameter(constructorCall,
                    ComponentType.TEXTFIELD, 0);
            if (param != null) {
                Modification mod = Modification.replace(param,
                        new StringLiteralExpr(newText));
                mods.add(mod);
                return true;
            }
        }
        return false;

    }

    private StringLiteralExpr findConstructorParameter(
            ObjectCreationExpr objectCreationExpression, ComponentType type,
            int parameterIndex) {
        if (isConstructorFor(objectCreationExpression, type)) {
            if (objectCreationExpression.getArguments().size() == 1) {
                if (objectCreationExpression.getArguments()
                        .size() >= parameterIndex - 1) {
                    Expression param = objectCreationExpression
                            .getArgument(parameterIndex);
                    if (param.isStringLiteralExpr()) {
                        return param.asStringLiteralExpr();
                    }
                }
            }
        }
        return null;
    }

    private boolean isConstructorFor(
            ObjectCreationExpr objectCreationExpression, ComponentType type) {
        // FIXME should resolve name
        String constructorType = objectCreationExpression.getType().getName()
                .asString();
        return constructorType.equals(type.getClassName())
                || constructorType.equals(getSimpleName(type));
    }

    private String getSimpleName(ComponentType type) {
        String className = type.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private ExpressionStmt findMethodCall(BlockStmt codeBlock, Node afterThis,
            SimpleName leftHandSide, String string) {
        boolean refFound = false;
        for (Statement s : codeBlock.getStatements()) {
            if (s == afterThis) {
                refFound = true;
            } else if (refFound) {
                if (!s.isExpressionStmt()) {
                    continue;
                }
                Expression expression = s.asExpressionStmt().getExpression();
                if (!expression.isMethodCallExpr()) {
                    continue;
                }
                MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
                if (!methodCallExpr.getScope().isPresent()) {
                    continue;
                }
                Expression scope = methodCallExpr.getScope().get();
                if (!scope.isNameExpr()) {
                    continue;
                }
                String variableName = scope.asNameExpr().getNameAsString();
                String methodName = methodCallExpr.getNameAsString();

                if (string.equals(methodName)
                        && variableName.equals(leftHandSide.getIdentifier())) {
                    return s.asExpressionStmt();
                }

                System.out.println(s);
            }
        }
        return null;
    }

    private Statement findNode(CompilationUnit cu, int lineNumber) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (contains(type, lineNumber)) {
                return findNode(type, lineNumber);
            }
        }
        return null;
    }

    private boolean contains(Node node, int lineNumber) {
        if (node.getBegin().get().line <= lineNumber
                && node.getEnd().get().line >= lineNumber) {
            return true;
        }
        return false;
    }

    private Statement findNode(TypeDeclaration<?> type, int lineNumber) {
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (contains(member, lineNumber)) {
                if (member instanceof NodeWithBlockStmt) {
                    return findNode((NodeWithBlockStmt<?>) member, lineNumber);
                }
            }
        }
        return null;
    }

    private Statement findNode(NodeWithBlockStmt<?> hasBlock, int lineNumber) {
        for (Statement statement : hasBlock.getBody().getStatements()) {
            if (contains(statement, lineNumber)) {
                return statement;
            }
        }
        return null;
    }

    private String readFile(File file) throws IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    public void addComponentAfter(File f, int componentInstantiationLineNumber,
            int componentAttachLineNumber, ComponentType componentType,
            String... constructorArguments) {
        modifyClass(f,
                (cu) -> addComponent(cu, componentInstantiationLineNumber,
                        componentAttachLineNumber, BeforeOrAfter.AFTER,
                        componentType, constructorArguments));
    }

    public void addComponentBefore(File f, int componentInstantiationLineNumber,
            int componentAttachLineNumber, ComponentType componentType,
            String... constructorArguments) {
        modifyClass(f,
                (cu) -> addComponent(cu, componentInstantiationLineNumber,
                        componentAttachLineNumber, BeforeOrAfter.BEFORE,
                        componentType, constructorArguments));
    }

    public void addComponentAfter(Class<?> cls,
            int componentInstantiationLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String... constructorArguments) {
        addComponentAfter(getSourceFile(cls), componentInstantiationLineNumber,
                componentAttachLineNumber, componentType, constructorArguments);
    }

    public void modifyClass(File f,
            Function<CompilationUnit, List<Modification>> modifier) {
        try {
            String source = readFile(f);
            CompilationUnit cu = parseSource(source);

            List<Modification> mods = modifier.apply(cu);
            Collections.sort(mods);
            String newSource = source;
            for (Modification mod : mods) {
                newSource = mod.apply(newSource);
            }

            if (newSource.equals(source)) {
                System.err.println("Unable to edit file");
                return;
            }

            try (FileWriter fw = new FileWriter(f)) {
                fw.write(newSource);
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

    public void setComponentAttribute(String className,
            int componentInstantiationLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParam) {
        setComponentAttribute(getSourceFile(className),
                componentInstantiationLineNumber, componentAttachLineNumber,
                componentType, methodName, methodParam);
    }

    public void setComponentAttribute(File f,
            int componentInstantiationLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParam) {
        modifyClass(f,
                cu -> modifyOrAddCall(cu, componentInstantiationLineNumber,
                        componentAttachLineNumber, componentType, methodName,
                        methodParam));
    }

    private CompilationUnit parseSource(String source) throws IOException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());

        // Configure JavaParser to use type resolution
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(
                combinedTypeSolver);
        StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(symbolSolver);
        return StaticJavaParser.parse(source);
    }

    public File getSourceFile(Class<?> cls) {
        return getSourceFile(cls.getName());
    }

    public File getSourceFile(String className) {
        String classFileName = className.replace(".", File.separator) + ".java";

        File src = new File("").getAbsoluteFile();// VaadinService.getCurrent().getDeploymentConfiguration().getProjectFolder();
        src = new File(src, "src");
        File f = new File(src, "main");
        f = new File(f, "java");
        f = new File(f, classFileName);
        if (f.exists()) {
            return f;
        }
        f = new File(src, "test");
        f = new File(f, "java");
        f = new File(f, classFileName);
        return f;
    }

}
