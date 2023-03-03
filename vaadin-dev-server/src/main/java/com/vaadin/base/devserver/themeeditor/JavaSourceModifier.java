package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaSourceModifier {

    private VaadinContext context;

    public static class ComponentMetadata {
        private boolean accessible;

        public boolean isAccessible() {
            return accessible;
        }

        public void setAccessible(boolean accessible) {
            this.accessible = accessible;
        }
    }

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
            Component component = getComponent(session, uiId, nodeId);
            ComponentTracker.Location location = getComponentLocation(
                    component);

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
                if (findExpressionStmtInBlockStmt(parentBlock, methodCall)
                        .isEmpty()) {
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
            Component component = getComponent(session, uiId, nodeId);
            ComponentTracker.Location location = getComponentLocation(
                    component);

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
                findExpressionStmtInBlockStmt(parentBlock, methodCall)
                        .ifPresent(parentBlock.getStatements()::remove);
            });

            writeFile(cu, sourceFile);
        });
    }

    /**
     * Returns metadata for picked component.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @return component metadata
     */
    public ComponentMetadata getMetadata(Integer uiId, Integer nodeId) {
        assert uiId != null && nodeId != null;

        try {
            ComponentMetadata metadata = new ComponentMetadata();
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location location = getComponentLocation(
                        component);

                File sourceFolder = getSourceFolder(location);
                SourceRoot root = new SourceRoot(sourceFolder.toPath());
                CompilationUnit cu = LexicalPreservingPrinter
                        .setup(root.parse("", location.filename()));

                try {
                    getVariableDeclarationExpressionStmt(cu, location);
                    metadata.setAccessible(true);
                } catch (Exception ex) {
                    metadata.setAccessible(false);
                }
            }).get(5, TimeUnit.SECONDS);
            return metadata;
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot generate metadata.", e);
        }
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
            throw new ThemeEditorException(
                    "Cannot modify className of selected component. Only single declaration in code block is supported currently. "
                            + "Cannot apply changes at: "
                            + toStackTraceElement(location));
        }
        return (ExpressionStmt) node;
    }

    protected Component getComponent(VaadinSession session, int uiId,
            int nodeId) {
        Element element = session.findElement(uiId, nodeId);
        Optional<Component> c = element.getComponent();
        if (!c.isPresent()) {
            throw new ThemeEditorException(
                    "Only component locations are tracked. The given node id refers to an element and not a component.");
        }
        return c.get();
    }

    protected ComponentTracker.Location getComponentLocation(Component c) {
        ComponentTracker.Location location = ComponentTracker.findCreate(c);
        if (location == null) {
            throw new ThemeEditorException(
                    "Unable to find the location where the component "
                            + c.getClass().getName() + " was created");
        }
        return location;
    }

    protected void writeFile(CompilationUnit cu, File sourceFile) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(sourceFile);
            IOUtils.write(LexicalPreservingPrinter.print(cu), writer);
        } catch (IOException e) {
            throw new ThemeEditorException(
                    "Cannot update file: " + sourceFile.getPath(), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    protected VaadinSession getSession() {
        return VaadinSession.getCurrent();
    }

    protected Optional<ExpressionStmt> findExpressionStmtInBlockStmt(
            BlockStmt n, ExpressionStmt stmt) {
        return n.getStatements().stream()
                .filter(ExpressionStmt.class::isInstance)
                .map(ExpressionStmt.class::cast).filter(e -> Objects
                        .equals(e.getExpression(), stmt.getExpression()))
                .findFirst();
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
