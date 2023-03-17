package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.SourceRoot;
import com.vaadin.base.devserver.editor.ComponentType;
import com.vaadin.base.devserver.editor.Editor;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaSourceModifier extends Editor {

    public static final String UNIQUE_CLASSNAME_PREFIX = "te-";

    private VaadinContext context;

    private static class ClassNameHolder {
        String className;
    }

    private static class AccessibleHolder {
        boolean accessible;
    }

    public JavaSourceModifier(VaadinContext context) {
        this.context = context;
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
            Collection<String> classNames) {
        assert uiId != null && nodeId != null && classNames != null;

        VaadinSession session = getSession();
        getSession().access(() -> {
            Component component = getComponent(session, uiId, nodeId);
            ComponentTracker.Location createLocation = getCreateLocation(
                    component);
            ComponentTracker.Location attachLocation = getAttachLocation(
                    component);
            File sourceFolder = getSourceFolder(createLocation);
            File sourceFile = new File(sourceFolder, createLocation.filename());

            try {
                List<String> existingClassNames = getClassNames(sourceFile,
                        createLocation.lineNumber());
                for (String className : classNames) {
                    if (!existingClassNames.contains(className)) {
                        addComponentAttribute(sourceFile,
                                createLocation.lineNumber(),
                                attachLocation.lineNumber(),
                                ComponentType.BUTTON, "addClassName",
                                className);
                    }
                }
            } catch (UnsupportedOperationException ex) {
                throw new ThemeEditorException(ex);
            }

        });
    }

    /**
     * Gets instance unique classname if exists otherwise generates and sets
     * new.
     *
     * @param uiId
     *            uiId of target component's UI
     * @param nodeId
     *            nodeIf of target component
     * @param createIfNotPresent
     *            append classname if not present yet
     * @return component unique classname
     */
    public String getUniqueClassName(Integer uiId, Integer nodeId,
            boolean createIfNotPresent) {
        assert uiId != null && nodeId != null;

        try {
            ClassNameHolder holder = new ClassNameHolder();
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location createLocation = getCreateLocation(
                        component);
                ComponentTracker.Location attachLocation = getAttachLocation(
                        component);
                File sourceFolder = getSourceFolder(createLocation);
                File sourceFile = new File(sourceFolder,
                        createLocation.filename());

                try {
                    List<String> existingClassNames = getClassNames(sourceFile,
                            createLocation.lineNumber());
                    Optional<String> className = existingClassNames.stream()
                            .filter(s -> s.startsWith(UNIQUE_CLASSNAME_PREFIX))
                            .findFirst();
                    if (className.isPresent()) {
                        holder.className = className.get();
                    } else if (createIfNotPresent) {
                        holder.className = generateUniqueClassName();
                        addComponentAttribute(sourceFile,
                                createLocation.lineNumber(),
                                attachLocation.lineNumber(),
                                ComponentType.BUTTON, "addClassName",
                                holder.className);
                    }
                } catch (UnsupportedOperationException ex) {
                    throw new ThemeEditorException(ex);
                }

            }).get(5, TimeUnit.SECONDS);
            return holder.className;
        } catch (Exception e) {
            throw new ThemeEditorException(
                    "Cannot get or set unique class name.", e);
        }
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
            Collection<String> classNames) {
        assert uiId != null && nodeId != null && classNames != null;

        VaadinSession session = getSession();
        getSession().access(() -> {
            Component component = getComponent(session, uiId, nodeId);
            ComponentTracker.Location createLocation = getCreateLocation(
                    component);
            ComponentTracker.Location attachLocation = getAttachLocation(
                    component);
            File sourceFolder = getSourceFolder(createLocation);
            File sourceFile = new File(sourceFolder, createLocation.filename());

            try {
                for (String className : classNames) {
                    removeComponentAttribute(sourceFile,
                            createLocation.lineNumber(),
                            attachLocation.lineNumber(), ComponentType.BUTTON,
                            "addClassName", className);
                }
            } catch (UnsupportedOperationException ex) {
                throw new ThemeEditorException(ex);
            }
        });
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

        AccessibleHolder holder = new AccessibleHolder();
        try {
            VaadinSession session = getSession();
            getSession().access(() -> {
                Component component = getComponent(session, uiId, nodeId);
                ComponentTracker.Location createLocation = getCreateLocation(
                        component);

                File sourceFolder = getSourceFolder(createLocation);
                SourceRoot root = new SourceRoot(sourceFolder.toPath());
                CompilationUnit cu = LexicalPreservingPrinter
                        .setup(root.parse("", createLocation.filename()));

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

    protected ComponentTracker.Location getCreateLocation(Component c) {
        ComponentTracker.Location location = ComponentTracker.findCreate(c);
        if (location == null) {
            throw new ThemeEditorException(
                    "Unable to find the location where the component "
                            + c.getClass().getName() + " was created");
        }
        return location;
    }

    protected ComponentTracker.Location getAttachLocation(Component c) {
        ComponentTracker.Location location = ComponentTracker.findAttach(c);
        if (location == null) {
            throw new ThemeEditorException(
                    "Unable to find the location where the component "
                            + c.getClass().getName() + " was attached");
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

    protected String generateUniqueClassName() {
        return UNIQUE_CLASSNAME_PREFIX + UUID.randomUUID();
    }

    public List<String> getClassNames(File f, int componentCreateLineNumber) {

        try {
            String source = readFile(f);
            CompilationUnit cu = parseSource(source);
            Statement node = findStatement(cu, componentCreateLineNumber);
            if (node == null) {
                throw new UnsupportedOperationException(
                        "Cannot add method call for given component.");
            }
            SimpleName localVariableOrField = findLocalVariableOrField(cu,
                    componentCreateLineNumber);
            BlockStmt codeBlock = (BlockStmt) node.getParentNode().get();

            List<MethodCallExpr> existingCalls = findMethodCalls(codeBlock,
                    localVariableOrField);
            if (existingCalls.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> classNames = new ArrayList<>();
            for (MethodCallExpr methodCallExpr : existingCalls) {
                if (methodCallExpr.getName().asString()
                        .equals("addClassName")) {
                    existingCalls.forEach(
                            m -> m.getArguments().forEach(a -> classNames
                                    .add(a.asStringLiteralExpr().asString())));
                }
            }
            return classNames;

        } catch (IOException e1) {
            throw new ThemeEditorException(e1);
        }

    }

}
