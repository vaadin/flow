package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.SourceRoot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaSourceModifier {

    private VaadinContext context;

    public JavaSourceModifier(VaadinContext context) {
        this.context = context;
    }

    public boolean isEnabled() {
        return true;
    }

    /**
     * Modifies class names of given component by adding addClassName method
     * calls.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @param classNames
     *            list of classes to be added
     */
    public void setClassNames(Integer uiId, Integer nodeId,
            List<String> classNames) {
        assert uiId != null && nodeId != null && classNames != null;

        VaadinSession session = getSession();
        getSession().access(() -> {
            ComponentTracker.Location location = getComponentLocation(session,
                    uiId, nodeId);

            File sourceFolder = getSourceFolder(location);
            File sourceFile = new File(sourceFolder, location.filename());
            SourceRoot root = new SourceRoot(sourceFolder.toPath());
            CompilationUnit cu = LexicalPreservingPrinter
                    .setup(root.parse("", location.filename()));

            ExpressionStmt node = getVariableDeclarationExpressionStmt(cu,
                    location);

            BlockStmt parentBlock = getParentBlockStmt(node).get();
            AtomicInteger index = new AtomicInteger(
                    getIndexOfNode(parentBlock, node));
            String variableName = getVariableName(node);
            classNames.forEach(className -> {
                ExpressionStmt methodCall = createMethodCallExprStmt(
                        variableName, "addClassName", className);
                if (!parentBlock.getStatements().contains(methodCall)) {
                    parentBlock.addStatement(index.incrementAndGet(),
                            methodCall);
                }
            });

            writeFile(cu, sourceFile);
        });
    }

    /**
     * Modifies class names of given component by removing addClassName method
     * calls.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @param classNames
     *            list of classes to be removed
     */
    public void removeClassNames(Integer uiId, Integer nodeId,
            List<String> classNames) {
        assert uiId != null && nodeId != null && classNames != null;

        VaadinSession session = getSession();
        getSession().access(() -> {
            ComponentTracker.Location location = getComponentLocation(session,
                    uiId, nodeId);

            File sourceFolder = getSourceFolder(location);
            File sourceFile = new File(sourceFolder, location.filename());
            SourceRoot root = new SourceRoot(sourceFolder.toPath());
            CompilationUnit cu = LexicalPreservingPrinter
                    .setup(root.parse("", location.filename()));

            ExpressionStmt node = getVariableDeclarationExpressionStmt(cu,
                    location);

            BlockStmt parentBlock = getParentBlockStmt(node).get();
            String variableName = getVariableName(node);
            classNames.forEach(className -> {
                ExpressionStmt methodCall = createMethodCallExprStmt(
                        variableName, "addClassName", className);
                parentBlock.getStatements().stream()
                        .filter(s -> simpleMethodCallExprStmtFilter(s,
                                methodCall))
                        .findFirst()
                        .ifPresent(parentBlock.getStatements()::remove);
            });

            writeFile(cu, sourceFile);
        });
    }

    protected String getVariableName(ExpressionStmt node) {
        return node.getExpression().asVariableDeclarationExpr().getVariables()
                .get(0).getNameAsString();
    }

    // finds variable declaration on given location line
    protected ExpressionStmt getVariableDeclarationExpressionStmt(
            CompilationUnit cu, ComponentTracker.Location location) {
        Node node = cu.accept(new LineNumberVisitor(), location.lineNumber());
        if (!nodeIsSingleVariableDeclaration(node)) {
            throw new ModifierException(
                    "Cannot modify className of selected component. Only single declaration in code block is supported currently. "
                            + "Cannot apply changes at: "
                            + toStackTraceElement(location));
        }
        return (ExpressionStmt) node;
    }

    protected ComponentTracker.Location getComponentLocation(
            VaadinSession session, int uiId, int nodeId) {
        Element element = session.findElement(uiId, nodeId);
        Optional<Component> c = element.getComponent();
        if (!c.isPresent()) {
            throw new ModifierException(
                    "Only component locations are tracked. The given node id refers to an element and not a component");
        }

        ComponentTracker.Location location = ComponentTracker
                .findCreate(c.get());
        if (location == null) {
            throw new ModifierException(
                    "Unable to find the location where the component "
                            + c.get().getClass().getName() + " was created");
        }
        return location;
    }

    protected void writeFile(CompilationUnit cu, File sourceFile) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(sourceFile);
            IOUtils.write(LexicalPreservingPrinter.print(cu), writer);
        } catch (IOException e) {
            throw new ModifierException(
                    "Cannot update file: " + sourceFile.getPath(), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public void addThemeAnnotation() {

    }

    protected VaadinSession getSession() {
        return VaadinSession.getCurrent();
    }

    // Compares two MethodCalls statements comparing scope, name and arguments
    protected boolean simpleMethodCallExprStmtFilter(Statement e1,
            Statement e2) {
        if (!e1.getClass().equals(e2.getClass())) {
            return false;
        }

        if (e1 instanceof ExpressionStmt exprE1
                && e2 instanceof ExpressionStmt exprE2) {
            if (exprE1.getExpression() instanceof MethodCallExpr mcExpr1
                    && exprE2
                            .getExpression() instanceof MethodCallExpr mcExpr2) {
                return mcExpr1.getScope()
                        .filter(mcExpr2.getScope().get()::equals).isPresent()
                        && mcExpr1.getName().equals(mcExpr2.getName())
                        && mcExpr1.getArguments()
                                .equals(mcExpr2.getArguments());
            }
        }

        return false;
    }

    protected File getSourceFolder(ComponentTracker.Location location) {
        Path javaSourceFolder = ApplicationConfiguration.get(context)
                .getJavaSourceFolder().toPath();
        String[] splitted = location.className().split("\\.");
        return Path.of(javaSourceFolder.toString(),
                Arrays.copyOf(splitted, splitted.length - 1)).toFile();
    }

    protected Optional<BlockStmt> getParentBlockStmt(Node n) {
        return n.getParentNode().filter(BlockStmt.class::isInstance)
                .map(BlockStmt.class::cast);
    }

    protected int getIndexOfNode(BlockStmt blockStmt, Node n) {
        for (int i = 0; i < blockStmt.getStatements().size(); ++i) {
            if (n.equals(blockStmt.getStatement(i))) {
                return i;
            }
        }
        return -1;
    }

    protected ExpressionStmt createMethodCallExprStmt(String variableName,
            String methodName, String className) {
        return new ExpressionStmt(new MethodCallExpr(methodName)
                .setScope(new NameExpr(variableName)).addArgument(
                        new StringLiteralExpr().setEscapedValue(className)));
    }

    protected boolean nodeIsSingleVariableDeclaration(Node n) {
        if (n instanceof ExpressionStmt expr
                && expr.getExpression().isVariableDeclarationExpr()
                && expr.getExpression().asVariableDeclarationExpr()
                        .getVariables().size() == 1
                && n.getParentNode().filter(BlockStmt.class::isInstance)
                        .isPresent()) {
            return true;
        }

        return false;
    }

    protected StackTraceElement toStackTraceElement(
            ComponentTracker.Location location) {
        return new StackTraceElement("", "", "", location.className(),
                location.methodName(), location.filename(),
                location.lineNumber());

    }

}
