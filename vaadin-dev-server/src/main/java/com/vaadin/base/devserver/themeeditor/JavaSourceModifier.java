package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithExpression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.SourceRoot;
import com.vaadin.base.devserver.editor.Editor;
import com.vaadin.base.devserver.editor.Where;
import com.vaadin.base.devserver.themeeditor.utils.LineNumberVisitor;
import com.vaadin.base.devserver.themeeditor.utils.LocalClassNameVisitor;
import com.vaadin.base.devserver.themeeditor.utils.LocalClassNamesVisitor;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.util.SharedUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final VaadinContext context;

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
            Component component = getComponent(session, uiId, nodeId);
            setLocalClassName(component, className, false);
            if (hasOverlay(component)) {
                setLocalClassName(component, className, true);
            }
        });
    }

    protected void setLocalClassName(Component component, String className,
            boolean overlay) {
        try {
            ComponentTracker.Location createLocation = getCreateLocation(
                    component);
            File sourceFile = getSourceFile(createLocation);
            int sourceOffset = modifyClass(sourceFile, cu -> {
                SimpleName scope = findLocalVariableOrField(cu,
                        createLocation.lineNumber());
                Node newNode = createAddClassNameStatement(scope, className,
                        overlay);
                Modification mod;
                ExpressionStmt stmt = findLocalClassNameStmt(cu, component,
                        overlay);
                if (stmt == null) {
                    Node node = findNode(cu, component);
                    Where where = findModificationWhere(cu, component);
                    mod = switch (where) {
                    case AFTER -> Modification.insertLineAfter(node, newNode);
                    case INSIDE ->
                        Modification.insertAtEndOfBlock(node, newNode);
                    case BEFORE -> Modification.insertLineBefore(node, newNode);
                    };
                } else {
                    mod = Modification.replace(stmt, newNode);
                }
                return Collections.singletonList(mod);
            });

            if (sourceOffset != 0) {
                ComponentTracker.refreshLocation(createLocation, sourceOffset);
            }

        } catch (UnsupportedOperationException ex) {
            throw new ThemeEditorException(ex);
        }
    }

    /**
     * Gets tag name of given component.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @return tag name of given element
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
            throw new ThemeEditorException("Cannot get tag of component.", e);
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
                        component, false);
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
        VaadinSession session = getSession();
        try {
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                removeLocalClassName(component, false);
                if (hasOverlay(component)) {
                    removeLocalClassName(component, true);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot remove local classname.", e);
        }
    }

    public void removeLocalClassName(Component component, boolean overlay) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        File sourceFile = getSourceFile(createLocation);
        int sourceOffset = modifyClass(sourceFile, cu -> {
            ExpressionStmt localClassNameStmt = findLocalClassNameStmt(cu,
                    component, overlay);
            if (localClassNameStmt != null) {
                return Collections
                        .singletonList(Modification.remove(localClassNameStmt));
            }
            throw new ThemeEditorException("Local classname not present.");
        });

        if (sourceOffset != 0) {
            ComponentTracker.refreshLocation(createLocation, sourceOffset);
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
                try {
                    Component component = getComponent(session, uiId, nodeId);
                    CompilationUnit cu = getCompilationUnit(component);
                    findModificationWhere(cu, component);
                    holder.accessible = true;
                } catch (Exception ex) {
                    getLogger().warn(ex.getMessage(), ex);
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
                String fileName = SharedUtil
                        .upperCamelCaseToDashSeparatedLowerCase(createLocation
                                .filename().substring(0, createLocation
                                        .filename().indexOf(".")));
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

    protected Statement createAddClassNameStatement(SimpleName scope,
            String className, boolean overlay) {
        MethodCallExpr methodCallExpr = new MethodCallExpr(
                overlay ? "setOverlayClassName" : "addClassName");
        if (scope != null) {
            methodCallExpr.setScope(new NameExpr(scope));
        }
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
        if (c.isEmpty()) {
            throw new ThemeEditorException(
                    "Only component locations are tracked. The given node id refers to an element and not a component.");
        }
        return c.get();
    }

    protected CompilationUnit getCompilationUnit(Component component) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        File sourceFolder = getSourceFolder(createLocation);
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        SourceRoot root = new SourceRoot(sourceFolder.toPath(),
                parserConfiguration);
        return LexicalPreservingPrinter
                .setup(root.parse("", createLocation.filename()));
    }

    protected ExpressionStmt findLocalClassNameStmt(CompilationUnit cu,
            Component component, boolean overlay) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        SimpleName scope = findLocalVariableOrField(cu,
                createLocation.lineNumber());
        Node parentBlockNode = findParentBlockNode(cu, component);
        return parentBlockNode.accept(new LocalClassNameVisitor(overlay),
                scope != null ? scope.getIdentifier() : null);
    }

    protected Node findParentBlockNode(CompilationUnit cu,
            Component component) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        Node node = cu.accept(new LineNumberVisitor(),
                createLocation.lineNumber());
        if (node instanceof BlockStmt) {
            return node;
        }
        while (node.getParentNode().isPresent()) {
            node = node.getParentNode().get();
            if (node instanceof BlockStmt blockStmt) {
                return blockStmt;
            }
        }
        // fallback to CU
        return cu;
    }

    protected Where findModificationWhere(CompilationUnit cu,
            Component component) {
        Node node = findNode(cu, component);
        if (node instanceof NodeWithBlockStmt<?>) {
            return Where.INSIDE;
        }
        if (node instanceof NodeWithExpression<?> expr
                && (expr.getExpression().isAssignExpr()
                        || expr.getExpression().isVariableDeclarationExpr())) {
            return Where.AFTER;
        }
        throw new ThemeEditorException("Cannot apply classname for " + node);
    }

    protected Node findNode(CompilationUnit cu, Component component) {
        ComponentTracker.Location createLocation = getCreateLocation(component);
        Node node = cu.accept(new LineNumberVisitor(),
                createLocation.lineNumber());
        if (node == null) {
            throw new ThemeEditorException("Cannot find component.");
        }
        return node;
    }

    protected boolean hasOverlay(Component component) {
        try {
            // HasOverlayClassName interface is not part of flow-server
            component.getClass().getMethod("setOverlayClassName", String.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JavaSourceModifier.class);
    }

}
