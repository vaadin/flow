package com.vaadin.base.devserver.editor;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithStatements;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.vaadin.base.devserver.themeeditor.utils.StatementLineNumberVisitor;
import com.vaadin.flow.shared.util.SharedUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Editor {

    public static class Modification implements Comparable<Modification> {

        private enum Type {
            IMPORT, INSERT_AFTER, INSERT_BEFORE, INSERT_LINE_AFTER, INSERT_LINE_BEFORE, //
            REPLACE, INSERT_AT_END_OF_BLOCK, REMOVE_NODE
        };

        private Node referenceNode;
        private Type type;
        private Node node;
        private int sourceOffset;

        public void apply() {
            if (type == Type.IMPORT) {
                if (referenceNode instanceof CompilationUnit cu
                        && node instanceof ImportDeclaration id) {
                    cu.getImports().add(id);
                }
            } else if (type == Type.INSERT_LINE_AFTER) {
                if (node instanceof Statement stmt) {
                    Editor.addStatement(referenceNode, Where.AFTER, stmt);
                }
            } else if (type == Type.INSERT_AFTER) {
                Node parent = referenceNode.getParentNode().orElse(null);
                if (parent instanceof MethodCallExpr mce) {
                    mce.getArguments().addAfter((Expression) node,
                            (Expression) referenceNode);
                }
            } else if (type == Type.INSERT_LINE_BEFORE) {
                if (node instanceof Statement stmt) {
                    Editor.addStatement(referenceNode, Where.BEFORE, stmt);
                }
            } else if (type == Type.INSERT_BEFORE) {
                Node parent = referenceNode.getParentNode().orElse(null);
                if (parent instanceof MethodCallExpr mce) {
                    mce.getArguments().addBefore((Expression) node,
                            (Expression) referenceNode);
                }
            } else if (type == Type.REPLACE) {
                // comment need to be removed separately not to leave empty line
                // while using LexicalPreservingPrinter
                referenceNode.getComment().ifPresent(Node::remove);
                referenceNode.replace(node);
            } else if (type == Type.INSERT_AT_END_OF_BLOCK) {
                if (node instanceof Statement stmt) {
                    if (referenceNode instanceof NodeWithStatements block) {
                        block.addStatement(stmt);
                    } else if (referenceNode instanceof NodeWithBlockStmt block) {
                        block.getBody().addStatement(stmt);
                    }
                }
            } else if (type == Type.REMOVE_NODE) {
                // comment need to be removed separately not to leave empty line
                // while using LexicalPreservingPrinter
                referenceNode.getComment().ifPresent(Node::remove);
                referenceNode.remove();
            } else {
                throw new RuntimeException("Failed to perform: " + this);
            }
        }

        public int sourceOffset() {
            return sourceOffset;
        }

        public static Modification addImport(Node referenceNode, Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.IMPORT;
            mod.node = node;
            mod.sourceOffset = getLinesCount(node);
            return mod;
        }

        public static Modification insertAfter(Node referenceNode, Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.INSERT_AFTER;
            mod.node = node;
            mod.sourceOffset = 0; // modifies same line, no offset
            return mod;
        }

        public static Modification insertAtEndOfBlock(Node referenceNode,
                Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.INSERT_AT_END_OF_BLOCK;
            mod.node = node;
            mod.sourceOffset = getLinesCount(node);
            return mod;
        }

        public static Modification insertBefore(Node referenceNode, Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.INSERT_BEFORE;
            mod.node = node;
            mod.sourceOffset = 0; // modifies same line, no offset
            return mod;
        }

        public static Modification insertLineBefore(Node referenceNode,
                Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.INSERT_LINE_BEFORE;
            mod.node = node;
            mod.sourceOffset = getLinesCount(node);
            return mod;
        }

        public static Modification insertLineAfter(Node referenceNode,
                Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.INSERT_LINE_AFTER;
            mod.node = node;
            mod.sourceOffset = getLinesCount(node);
            return mod;
        }

        public static Modification replace(Node referenceNode, Node node) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.REPLACE;
            mod.node = node;
            mod.sourceOffset = getLinesCount(node)
                    - getLinesCount(referenceNode);
            return mod;
        }

        public static Modification remove(Node referenceNode) {
            Modification mod = new Modification();
            mod.referenceNode = referenceNode;
            mod.type = Type.REMOVE_NODE;
            mod.sourceOffset = -getLinesCount(referenceNode);
            return mod;
        }

        @Override
        public int compareTo(Modification o) {
            // Sort end to start so positions do not change while replacing

            Position aBegin = referenceNode.getRange().get().begin;
            int a = aBegin.line;
            Position bBegin = o.referenceNode.getRange().get().begin;
            int b = bBegin.line;
            if (a == b) {
                return Integer.compare(bBegin.column, aBegin.column);
            }
            return Integer.compare(b, a);
        }

        @Override
        public String toString() {
            if (type == Type.INSERT_LINE_AFTER) {
                return "Modification INSERT_LINE_AFTER at position "
                        + referenceNode.getEnd().get() + ": " + node;
            } else if (type == Type.INSERT_AFTER) {
                return "Modification INSERT_AFTER at position "
                        + referenceNode.getEnd().get() + ": " + node;
            } else if (type == Type.INSERT_BEFORE) {
                return "Modification INSERT_BEFORE at position "
                        + referenceNode.getBegin().get() + ": " + node;
            } else if (type == Type.REPLACE) {
                return "Modification REPLACE position "
                        + referenceNode.getBegin().get() + "-"
                        + referenceNode.getEnd().get() + ": " + node;
            } else if (type == Type.REMOVE_NODE) {
                return "Modification REMOVE position "
                        + referenceNode.getBegin().get() + "-"
                        + referenceNode.getEnd().get() + ": " + referenceNode;
            } else if (type == Type.INSERT_AT_END_OF_BLOCK) {
                return "Modification INSERT_AT_END_OF_BLOCK " + referenceNode;
            }
            return "Modification UNKNOWN TYPE";
        }

    }

    private List<Modification> modifyOrAddCall(CompilationUnit cu,
            int componentCreateLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParameter) {

        List<Modification> mods = new ArrayList<>();

        Statement node = findStatement(cu, componentCreateLineNumber);
        SimpleName localVariableOrField = findLocalVariableOrField(cu,
                componentCreateLineNumber);
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

    private List<Modification> addCall(CompilationUnit cu,
            int componentCreateLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParameter) {

        List<Modification> mods = new ArrayList<>();

        Statement node = findStatement(cu, componentCreateLineNumber);
        if (node == null) {
            throw new UnsupportedOperationException(
                    "Cannot add method call for given component.");
        }

        SimpleName localVariableOrField = findLocalVariableOrField(cu,
                componentCreateLineNumber);
        if (localVariableOrField == null) {
            modifyOrAddCallInlineConstructor(cu, node, componentType,
                    methodName, methodParameter, mods);
            return mods;
        }

        Expression exp = (Expression) localVariableOrField.getParentNode().get()
                .getParentNode().get();
        if (exp.isAssignExpr()) {
            addCall(node, localVariableOrField, methodName,
                    new StringLiteralExpr(methodParameter), mods);
        } else if (exp.isVariableDeclarationExpr()) {
            addCall(node, localVariableOrField, methodName,
                    new StringLiteralExpr(methodParameter), mods);
        }

        return mods;

    }

    private List<Modification> removeCall(CompilationUnit cu,
            int componentCreateLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParameter) {

        List<Modification> mods = new ArrayList<>();

        Statement node = findStatement(cu, componentCreateLineNumber);
        SimpleName localVariableOrField = findLocalVariableOrField(cu,
                componentCreateLineNumber);
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
                removeCall(codeBlock, node, localVariableOrField, methodName,
                        new StringLiteralExpr(methodParameter), mods);
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
                removeCall(codeBlock, node, localVariableOrField, methodName,
                        new StringLiteralExpr(methodParameter), mods);
            }
        }

        return mods;

    }

    private void modifyOrAddCallInlineConstructor(CompilationUnit cu,
            Statement componentNode, ComponentType componentType,
            String methodName, String methodParameter,
            List<Modification> mods) {
        if (!componentNode.isExpressionStmt() || componentType == null) {
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

    protected SimpleName findLocalVariableOrField(CompilationUnit cu,
            int componentCreateLineNumber) {
        Statement statement = findStatement(cu, componentCreateLineNumber);
        if (statement != null && statement.isExpressionStmt()) {
            ExpressionStmt expressionStmt = statement.asExpressionStmt();
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
            int componentCreateLineNumber, int componentAttachLineNumber,
            Where where, ComponentType componentType,
            String... constructorArguments) {
        List<Modification> mods = new ArrayList<>();

        if (!hasImport(cu, componentType.getClassName())) {
            mods.add(addImport(cu, componentType.getClassName()));
        }

        Statement createStatement = findStatement(cu,
                componentCreateLineNumber);
        if (createStatement == null || createStatement.isBlockStmt()) {
            if (where == Where.INSIDE) {
                // Potentially a @Route class
                mods.addAll(addComponentToClass(cu, componentCreateLineNumber,
                        componentType, constructorArguments));
            }
            return mods;
        }
        Statement attachStatement = findStatement(cu,
                componentAttachLineNumber);

        String localVariableName = getVariableName(componentType,
                constructorArguments);
        localVariableName = findUnusedVariableName(localVariableName,
                (BlockStmt) createStatement.getParentNode().get(), null);
        ExpressionStmt componentConstructCode = assignToLocalVariable(
                componentType, localVariableName,
                getConstructorCode(componentType, constructorArguments));
        Node componentAttachNode = new NameExpr(localVariableName);
        SimpleName referenceLocalVariableOrField = findLocalVariableOrField(cu,
                componentCreateLineNumber);

        mods.add(Modification.insertLineBefore(attachStatement,
                componentConstructCode));
        if (referenceLocalVariableOrField == null
                && attachStatement.equals(createStatement)
                && attachStatement.isExpressionStmt()) {
            // The reference component is created inline
            Expression attachNodeExpression = attachStatement.asExpressionStmt()
                    .getExpression();
            if (attachNodeExpression.isMethodCallExpr()) {
                ObjectCreationExpr referenceComponentAdd = findConstructorCallParameter(
                        attachNodeExpression.asMethodCallExpr(), componentType);
                MethodCallExpr methodCallExpr = attachNodeExpression
                        .asMethodCallExpr();
                NodeList<Expression> args = methodCallExpr.getArguments();
                for (int i = 0; i < args.size(); i++) {
                    if (referenceComponentAdd.equals(args.get(i))) {
                        if (where == Where.BEFORE) {
                            mods.add(Modification.insertBefore(args.get(i),
                                    componentAttachNode));
                        } else {
                            mods.add(Modification.insertAfter(args.get(i),
                                    componentAttachNode));
                        }
                        break;
                    }
                }
                return mods;
            }
        } else if (referenceLocalVariableOrField != null
                && attachStatement.isExpressionStmt()) {
            Expression expression = attachStatement.asExpressionStmt()
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
                    if (name.equals(referenceLocalVariableOrField)) {
                        // new ExpressionStmt(new Expression)
                        // ClassOrInterfaceDeclaration type =
                        // cu.getClassByName(componentType.getName()).get();
                        if (where == Where.BEFORE) {
                            mods.add(Modification.insertBefore(args.get(i),
                                    componentAttachNode));
                        } else {
                            mods.add(Modification.insertAfter(args.get(i),
                                    componentAttachNode));
                        }
                        break;
                    }
                }
            }
        }
        return mods;

    }

    private String findUnusedVariableName(String localVariableName,
            BlockStmt body, ClassOrInterfaceDeclaration classDefinition) {
        Set<String> usedLocalVariables = findLocalVariables(body);
        if (classDefinition == null) {
            classDefinition = body
                    .findAncestor(ClassOrInterfaceDeclaration.class).get();
        }
        Set<String> usedFieldNames = findFieldNames(classDefinition);

        String varName = localVariableName;
        int i = 2;
        while (usedLocalVariables.contains(varName)
                || usedFieldNames.contains(varName)) {
            varName = localVariableName + i;
            i++;
        }
        return varName;
    }

    private Set<String> findFieldNames(
            ClassOrInterfaceDeclaration classDefinition) {
        Set<String> names = new HashSet<>();
        for (FieldDeclaration field : classDefinition.getFields()) {
            for (VariableDeclarator varDecl : field.getVariables()) {
                names.add(varDecl.getNameAsString());
            }
        }
        return names;
    }

    private Set<String> findLocalVariables(BlockStmt body) {
        Set<String> names = new HashSet<>();
        if (body != null) {

            for (Statement statement : body.getStatements()) {
                if (statement.isExpressionStmt() && statement.asExpressionStmt()
                        .getExpression().isVariableDeclarationExpr()) {
                    NodeList<VariableDeclarator> vars = statement
                            .asExpressionStmt().getExpression()
                            .asVariableDeclarationExpr().getVariables();
                    for (VariableDeclarator varDecl : vars) {
                        names.add(varDecl.getNameAsString());
                    }
                }
            }
        }
        return names;
    }

    private ExpressionStmt assignToLocalVariable(ComponentType componentType,
            String variableName, Expression expression) {

        VariableDeclarationExpr localVariable = new VariableDeclarationExpr(
                new VariableDeclarator(getType(componentType), variableName));

        return new ExpressionStmt(
                new AssignExpr(localVariable, expression, Operator.ASSIGN));
    }

    private Modification addImport(CompilationUnit cu, String className) {
        return Modification.addImport(cu,
                new ImportDeclaration(className, false, false));
    }

    private boolean hasImport(CompilationUnit cu, String className) {
        for (ImportDeclaration importDecl : cu.getImports()) {
            if (importDecl.getNameAsString().equals(className)) {
                return true;
            }
        }
        return false;
    }

    private List<Modification> addComponentToClass(CompilationUnit cu,
            int componentCreateLineNumber, ComponentType componentType,
            String[] constructorArguments) {
        List<Modification> mods = new ArrayList<>();

        ClassOrInterfaceDeclaration classDefinition = findClassDefinition(cu,
                componentCreateLineNumber);
        ConstructorDeclaration constructor = findConstructorDeclaration(cu,
                componentCreateLineNumber);

        String variableName = getVariableName(componentType,
                constructorArguments);
        if (constructor != null) {
            variableName = findUnusedVariableName(variableName,
                    constructor.getBody(), null);
        } else if (classDefinition != null) {
            variableName = findUnusedVariableName(variableName, null,
                    classDefinition);
        }
        ExpressionStmt createComponent = assignToLocalVariable(componentType,
                variableName,
                getConstructorCode(componentType, constructorArguments));
        ExpressionStmt addComponent = addToLayout(variableName);

        if (constructor != null) {
            mods.add(Modification.insertAtEndOfBlock(constructor.getBody(),
                    createComponent));
            mods.add(Modification.insertAtEndOfBlock(constructor.getBody(),
                    addComponent));
        } else if (classDefinition != null) {
            if (!classDefinition.getConstructors().isEmpty()) {
                // This should not happen as create location refers to the class
                // when this is
                // called
                return mods;
            }

            // A class without any constructor

            ConstructorDeclaration defaultConstructor = classDefinition
                    .addConstructor(Keyword.PUBLIC);
            defaultConstructor.getBody().addStatement(createComponent);
            defaultConstructor.getBody().addStatement(addComponent);
        }
        return mods;

    }

    private static String indent(int amount, String string) {
        String indent = " ".repeat(amount);
        return Pattern.compile("^", Pattern.MULTILINE).matcher(string)
                .replaceAll(indent);
    }

    private List<Modification> addListener(CompilationUnit cu,
            int componentCreateLineNumber, int componentAttachLineNumber,
            String listenerType) {
        List<Modification> mods = new ArrayList<>();
        Statement createStatement = findStatement(cu,
                componentCreateLineNumber);
        SimpleName referenceLocalVariableOrField = findLocalVariableOrField(cu,
                componentCreateLineNumber);

        // ClassOrInterfaceType type =
        // StaticJavaParser.parseClassOrInterfaceType("ClickListener");
        // VoidType type = new VoidType();
        // LambdaExpr emptyCallback = new LambdaExpr(new Parameter(type, "e"),
        // new BlockStmt(new NodeList<>()));
        Parameter param = new Parameter();
        param.setName("e");
        LambdaExpr emptyCallback = new LambdaExpr(param,
                new BlockStmt(new NodeList<>()));
        MethodCallExpr listener = new MethodCallExpr(
                new NameExpr(referenceLocalVariableOrField), listenerType,
                new NodeList<>(emptyCallback));
        // Add an empty row where the code can be written
        Node listenerNode = new ExpressionStmt(listener)
                .setComment(new LineComment(" TODO: Implement listener"));
        Node parent = createStatement.getParentNode().get();
        if (parent instanceof BlockStmt) {
            // Find last method call for the local variable and add after that
            List<MethodCallExpr> methodCalls = findMethodCalls(
                    (BlockStmt) parent, referenceLocalVariableOrField);
            if (methodCalls.isEmpty()) {
                mods.add(Modification.insertLineAfter(createStatement,
                        listenerNode));
            } else {
                mods.add(Modification.insertLineAfter(methodCalls
                        .get(methodCalls.size() - 1).getParentNode().get(),
                        listenerNode));
            }

        } else {
            // Add after create. Not sure what the code looks like
            mods.add(Modification.insertAfter(createStatement, listenerNode));
        }
        return mods;
    }

    private String addNewLineToBody(String body) {
        int endOfBody = body.lastIndexOf("}");
        return body.substring(0, endOfBody) + "\n" + body.substring(endOfBody);
    }

    protected List<MethodCallExpr> findMethodCalls(BlockStmt parent,
            SimpleName variableName) {
        List<MethodCallExpr> methodCalls = new ArrayList<>();
        for (Statement s : parent.getStatements()) {
            if (!s.isExpressionStmt()) {
                continue;
            }
            ExpressionStmt expr = s.asExpressionStmt();
            if (expr.getExpression().isMethodCallExpr()) {
                MethodCallExpr methodCall = expr.getExpression()
                        .asMethodCallExpr();
                if (methodCall.getScope().isPresent()) {
                    Expression scope = methodCall.getScope().get();
                    if (scope.isNameExpr() && scope.asNameExpr().getName()
                            .equals(variableName)) {
                        methodCalls.add(methodCall);
                    }
                }
            }
        }

        return methodCalls;
    }

    private ExpressionStmt addToLayout(String variableName) {
        return new ExpressionStmt(
                new MethodCallExpr("add", new NameExpr(variableName)));
    }

    private String getVariableName(ComponentType type,
            String[] constructorArguments) {
        if (constructorArguments.length == 1) {
            return SharedUtil.firstToLower(SharedUtil.dashSeparatedToCamelCase(
                    constructorArguments[0].replaceAll(" ", "-")));
        }
        String simpleName = type.getClassName();
        simpleName = simpleName.substring(simpleName.lastIndexOf('.'));
        return simpleName;
    }

    private ClassOrInterfaceDeclaration findClassDefinition(CompilationUnit cu,
            int lineNumber) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (contains(type.getName(), lineNumber)) {
                return type.asClassOrInterfaceDeclaration();
            }
        }
        return null;
    }

    private ConstructorDeclaration findConstructorDeclaration(
            CompilationUnit cu, int lineNumber) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (contains(type, lineNumber)) {
                return findConstructorDeclaration(type, lineNumber);
            }
        }
        return null;
    }

    private ConstructorDeclaration findConstructorDeclaration(
            TypeDeclaration<?> type, int lineNumber) {
        for (ConstructorDeclaration constructor : type.getConstructors()) {
            if (contains(constructor, lineNumber)) {
                return constructor;
            }
        }
        return null;
    }

    private ObjectCreationExpr getConstructorCode(ComponentType componentType,
            String[] constructorArguments) {
        ClassOrInterfaceType type = getType(componentType);
        List<Expression> componentConstructorArgs = Arrays
                .stream(constructorArguments)
                .map(arg -> new StringLiteralExpr(arg))
                .collect(Collectors.toList());
        return new ObjectCreationExpr(null, type,
                new NodeList<>(componentConstructorArgs));
    }

    private ClassOrInterfaceType getType(ComponentType componentType) {
        ClassOrInterfaceType type = StaticJavaParser
                .parseClassOrInterfaceType(componentType.getClassName());
        type.setScope(null); // Remove package name
        return type;
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
            Modification mod = Modification.replace(existingCall, setTextCall);
            mods.add(mod);

        } else {
            Modification mod = Modification.insertLineAfter(afterThisNode,
                    setTextCall);
            mods.add(mod);
        }

    }

    private void removeCall(BlockStmt codeBlock, Node afterThisNode,
            SimpleName variableName, String methodName,
            Expression methodArgument, List<Modification> mods) {
        NodeList<Expression> arguments = new NodeList<Expression>();
        arguments.add(methodArgument);

        ExpressionStmt setTextCall = new ExpressionStmt(new MethodCallExpr(
                new NameExpr(variableName), methodName, arguments));

        ExpressionStmt existingCall = findMethodCall(codeBlock, afterThisNode,
                variableName, methodName);
        if (existingCall != null) {
            Modification mod = Modification.remove(existingCall);
            mods.add(mod);

        }

    }

    private void addCall(Node afterThisNode, SimpleName variableName,
            String methodName, Expression methodArgument,
            List<Modification> mods) {
        NodeList<Expression> arguments = new NodeList<>();
        arguments.add(methodArgument);

        ExpressionStmt setTextCall = new ExpressionStmt(new MethodCallExpr(
                new NameExpr(variableName), methodName, arguments));

        Modification mod = Modification.insertLineAfter(afterThisNode,
                setTextCall);
        mods.add(mod);

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

    private static int getLinesCount(Node node) {
        int nodeLines = node.getRange().map(r -> r.getLineCount())
                .orElseGet(() -> node.toString().split("\n").length);
        if (node.getComment().isPresent()) {
            Comment comment = node.getComment().get();
            nodeLines += comment.getRange().map(Range::getLineCount).orElse(0);
        }
        return nodeLines;
    }

    protected ExpressionStmt findMethodCall(BlockStmt codeBlock, Node afterThis,
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

            }
        }
        return null;
    }

    protected Statement findStatement(CompilationUnit cu, int lineNumber) {
        return cu.accept(new StatementLineNumberVisitor(), lineNumber);
    }

    private boolean contains(Node node, int lineNumber) {
        if (node.getBegin().get().line <= lineNumber
                && node.getEnd().get().line >= lineNumber) {
            return true;
        }
        return false;
    }

    private static void addStatement(Node referenceNode, Where where,
            Statement stmt) {
        if (referenceNode.getParentNode()
                .orElse(null) instanceof NodeWithStatements nws) {
            if (where == null) {
                nws.addStatement(stmt);
            } else {
                int index = nws.getStatements().indexOf(referenceNode)
                        + (Where.AFTER.equals(where) ? 1 : 0);
                nws.addStatement(index, stmt);
            }
        }
    }

    protected String readFile(File file) throws IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    public int addComponent(File f, int referenceComponentCreateLineNumber,
            int referenceComponentAttachLineNumber, Where where,
            ComponentType componentType, String... constructorArguments) {
        return modifyClass(f,
                (cu) -> addComponent(cu, referenceComponentCreateLineNumber,
                        referenceComponentAttachLineNumber, where,
                        componentType, constructorArguments));
    }

    // public void addComponentAfter(Class<?> cls, int
    // componentCreateLineNumber,
    // int componentAttachLineNumber, ComponentType componentType,
    // String... constructorArguments) {
    // addComponentAfter(getSourceFile(cls), componentCreateLineNumber,
    // componentAttachLineNumber, componentType, constructorArguments);
    // }

    // public void addComponentInside(Class<?> cls,
    // int parentcomponentCreateLineNumber,
    // int parentComponentAttachLineNumber, ComponentType componentType,
    // String... constructorArguments) {
    // addComponentInside(getSourceFile(cls), parentcomponentCreateLineNumber,
    // parentComponentAttachLineNumber, componentType,
    // constructorArguments);
    // }

    public int addListener(File f, int componentCreateLineNumber,
            int componentAttachLineNumber, String listenerType) {
        return modifyClass(f, (cu) -> addListener(cu, componentCreateLineNumber,
                componentAttachLineNumber, listenerType));
    }

    public int modifyClass(File f,
            Function<CompilationUnit, List<Modification>> modifier) {
        try {
            String source = readFile(f);
            CompilationUnit cu = parseSource(source);

            List<Modification> mods = modifier.apply(cu);
            Collections.sort(mods);
            int sourceOffset = 0;
            for (Modification mod : mods) {
                mod.apply();
                sourceOffset += mod.sourceOffset();
            }

            String newSource = LexicalPreservingPrinter.print(cu);
            if (newSource.equals(source)) {
                throw new UnsupportedOperationException("Unable to edit file");
            }

            try (FileWriter fw = new FileWriter(f)) {
                fw.write(newSource);
            }
            return sourceOffset;
        } catch (IOException e1) {
            throw new UnsupportedOperationException(e1);
        }

    }

    public int setComponentAttribute(String className,
            int componentCreateLineNumber, int componentAttachLineNumber,
            ComponentType componentType, String methodName,
            String methodParam) {
        return setComponentAttribute(getSourceFile(className),
                componentCreateLineNumber, componentAttachLineNumber,
                componentType, methodName, methodParam);
    }

    public int setComponentAttribute(File f, int componentCreateLineNumber,
            int componentAttachLineNumber, ComponentType componentType,
            String methodName, String methodParam) {
        return modifyClass(f,
                cu -> modifyOrAddCall(cu, componentCreateLineNumber,
                        componentAttachLineNumber, componentType, methodName,
                        methodParam));
    }

    public int addComponentAttribute(File f, int componentCreateLineNumber,
            int componentAttachLineNumber, ComponentType componentType,
            String methodName, String methodParam) {
        return modifyClass(f,
                cu -> addCall(cu, componentCreateLineNumber,
                        componentAttachLineNumber, componentType, methodName,
                        methodParam));
    }

    public int removeComponentAttribute(File f, int componentCreateLineNumber,
            int componentAttachLineNumber, ComponentType componentType,
            String methodName, String methodParam) {
        return modifyClass(f,
                cu -> removeCall(cu, componentCreateLineNumber,
                        componentAttachLineNumber, componentType, methodName,
                        methodParam));
    }

    protected CompilationUnit parseSource(String source) {
        return LexicalPreservingPrinter.setup(StaticJavaParser.parse(source));
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