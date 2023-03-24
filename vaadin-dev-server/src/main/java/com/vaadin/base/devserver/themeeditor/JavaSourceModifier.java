package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.SourceRoot;
import com.vaadin.base.devserver.editor.Editor;
import com.vaadin.base.devserver.themeeditor.utils.LocalClassNameVisitor;
import com.vaadin.base.devserver.themeeditor.utils.LocalClassNamesVisitor;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class JavaSourceModifier extends Editor {

    public static final LineComment LOCAL_CLASSNAME_COMMENT = new LineComment(
            "<theme-editor-local-classname>");

    private VaadinContext context;

    private static class FinalsHolder {
        boolean accessible;
        String className;
        String suggestedClassName;
        String tagName;
    }

    public JavaSourceModifier(VaadinContext context) {
        this.context = context;
    }

    /**
     * Adds local component class name if not already present, updates value
     * otherwise.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @param className
     *            className to be set
     */
    public void setLocalClassName(Integer uiId, Integer nodeId,
            String className) {
        assert uiId != null && nodeId != null && className != null;
        VaadinSession session = getSession();
        getSession().access(() -> {
            try {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location createLocation = getCreateLocation(
                        component);
                File sourceFile = getSourceFile(createLocation);
                int sourceOffset = modifyClass(sourceFile, cu -> {
                    Statement ref = findStatement(cu,
                            createLocation.lineNumber());
                    SimpleName scope = findLocalVariableOrField(cu,
                            createLocation.lineNumber());
                    if (scope == null) {
                        throw new ThemeEditorException(
                                "Variable not accessible.");
                    }

                    Node newNode = createAddClassNameStatement(scope.asString(),
                            className);
                    Modification mod;
                    ExpressionStmt stmt = findLocalClassNameStmt(cu, component);
                    if (stmt == null) {
                        mod = Modification.insertLineAfter(ref, newNode);
                    } else {
                        mod = Modification.replace(stmt, newNode);
                    }
                    return Collections.singletonList(mod);
                });

                if (sourceOffset != 0) {
                    ComponentTracker.refreshLocation(createLocation,
                            sourceOffset);
                }

            } catch (UnsupportedOperationException ex) {
                throw new ThemeEditorException(ex);
            }
        });
    }

    /**
     * Gets tag name of given component.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @return
     */
    public String getTag(Integer uiId, Integer nodeId) {
        assert uiId != null && nodeId != null;
        try {
            FinalsHolder holder = new FinalsHolder();
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                holder.tagName = component.getElement().getTag();
            }).get(5, TimeUnit.SECONDS);
            return holder.tagName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets component local classname if exists.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @return component local classname
     */
    public String getLocalClassName(Integer uiId, Integer nodeId) {
        assert uiId != null && nodeId != null;
        try {
            FinalsHolder holder = new FinalsHolder();
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                CompilationUnit cu = getCompilationUnit(component);
                ExpressionStmt localClassNameStmt = findLocalClassNameStmt(cu,
                        component);
                if (localClassNameStmt != null) {
                    holder.className = localClassNameStmt.getExpression()
                            .asMethodCallExpr().getArgument(0)
                            .asStringLiteralExpr().asString();
                }
            }).get(5, TimeUnit.SECONDS);
            return holder.className;
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot get local classname.", e);
        }
    }

    /**
     * Removes local class name of given component.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     */
    public void removeLocalClassName(Integer uiId, Integer nodeId) {
        assert uiId != null && nodeId != null;
        try {
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location createLocation = getCreateLocation(
                        component);
                File sourceFile = getSourceFile(createLocation);
                int sourceOffset = modifyClass(sourceFile, cu -> {
                    ExpressionStmt localClassNameStmt = findLocalClassNameStmt(
                            cu, component);
                    if (localClassNameStmt != null) {
                        return Collections.singletonList(
                                Modification.remove(localClassNameStmt));
                    }
                    throw new ThemeEditorException(
                            "Local classname not present.");
                });

                if (sourceOffset != 0) {
                    ComponentTracker.refreshLocation(createLocation,
                            sourceOffset);
                }

            }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot remove local classname.", e);
        }
    }

    /**
     * Checks if component can be accessed within source code.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @return true if component is accessible, false otherwise
     */
    public boolean isAccessible(Integer uiId, Integer nodeId) {
        assert uiId != null && nodeId != null;

        FinalsHolder holder = new FinalsHolder();
        try {
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location createLocation = getCreateLocation(
                        component);
                CompilationUnit cu = getCompilationUnit(component);

                Statement stmt = findStatement(cu, createLocation.lineNumber());
                if (stmt != null && stmt instanceof ExpressionStmt exp) {
                    holder.accessible = exp.getExpression().isAssignExpr()
                            || exp.getExpression().isVariableDeclarationExpr();
                } else {
                    holder.accessible = false;
                }

            }).get(5, TimeUnit.SECONDS);
            return holder.accessible;
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot generate metadata.", e);
        }
    }

    /**
     * Creates suggested local classname based on component tag.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @return suggested local classname
     */
    public String getSuggestedClassName(Integer uiId, Integer nodeId) {
        assert uiId != null && nodeId != null;

        FinalsHolder holder = new FinalsHolder();
        try {
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location createLocation = getCreateLocation(
                        component);
                String fileName = createLocation.filename().substring(0,
                        createLocation.filename().indexOf("."));
                String tagName = component.getElement().getTag()
                        .replace("vaadin-", "");

                CompilationUnit cu = getCompilationUnit(component);
                LocalClassNamesVisitor visitor = new LocalClassNamesVisitor();
                cu.accept(visitor, null);
                List<String> existingClassNames = visitor.getArguments();
                String suggestion = fileName + "-" + tagName + "-";
                // suggest classname "filename-tagname-" + (1 : 99)
                holder.suggestedClassName = IntStream.range(1, 100)
                        .mapToObj(i -> suggestion + i)
                        .filter(i -> !existingClassNames.contains(i))
                        .findFirst().orElse(null);

            }).get(5, TimeUnit.SECONDS);
            return holder.suggestedClassName;
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot generate metadata.", e);
        }
    }

    protected ComponentTracker.Location getCreateLocation(Component c) {
        ComponentTracker.Location location = ComponentTracker.findCreate(c);
        if (location == null) {
            throw new ThemeEditorException(
                    "Unable to find the location where the component "
                            + c.getClass().getName() + " was created");
        }
        return location;
    }

    protected VaadinSession getSession() {
        return VaadinSession.getCurrent();
    }

    protected File getSourceFolder(ComponentTracker.Location location) {
        Path javaSourceFolder = ApplicationConfiguration.get(context)
                .getJavaSourceFolder().toPath();
        String[] splitted = location.className().split("\\.");
        return Path.of(javaSourceFolder.toString(),
                Arrays.copyOf(splitted, splitted.length - 1)).toFile();
    }

    protected Statement createAddClassNameStatement(String scope,
            String className) {
        MethodCallExpr methodCallExpr = new MethodCallExpr("addClassName");
        methodCallExpr.setScope(new NameExpr(scope));
        methodCallExpr.getArguments().add(new StringLiteralExpr(className));
        Statement statement = new ExpressionStmt(methodCallExpr);
        statement.setComment(LOCAL_CLASSNAME_COMMENT);
        return statement;
    }

    protected File getSourceFile(ComponentTracker.Location createLocation) {
        File sourceFolder = getSourceFolder(createLocation);
        return new File(sourceFolder, createLocation.filename());
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

    protected CompilationUnit getCompilationUnit(Component component) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        File sourceFolder = getSourceFolder(createLocation);
        SourceRoot root = new SourceRoot(sourceFolder.toPath());
        return LexicalPreservingPrinter
                .setup(root.parse("", createLocation.filename()));
    }

    protected ExpressionStmt findLocalClassNameStmt(CompilationUnit cu,
            Component component) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        SimpleName scope = findLocalVariableOrField(cu,
                createLocation.lineNumber());
        return cu.accept(new LocalClassNameVisitor(), scope.getIdentifier());
    }

}
