/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * A class visitor for Flow components.
 */
class FrontendClassVisitor extends ClassVisitor {

    private static final String VARIANT = "variant";
    private static final String LAYOUT = "layout";
    static final String VALUE = "value";
    static final String VERSION = "version";

    /**
     * An annotation visitor implementation that enables repeated annotations.
     */
    static class RepeatedAnnotationVisitor extends AnnotationVisitor {
        public RepeatedAnnotationVisitor() {
            super(Opcodes.ASM7);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return this;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name,
                String descriptor) {
            return this;
        }
    }

    /**
     * A simple container with the information related to an application
     * end-point, i.e. those classes annotated with the {@link Route}
     * annotation.
     */
    static class EndPointData implements Serializable {
        private final String name;
        private String route = "";
        private String layout;
        private ThemeData theme = new ThemeData();
        private final HashSet<String> modules = new HashSet<>();
        private final HashSet<String> scripts = new HashSet<>();
        private final HashSet<String> classes = new HashSet<>();

        EndPointData(Class<?> clazz) {
            this.name = clazz.getName();
        }

        // For debugging
        @Override
        public String toString() {
            return String.format(
                    "%n view: %s%n route: %s%n%s%n layout: %s%n modules: %s%n scripts: %s%n",
                    name, route, theme, layout, col2Str(modules),
                    col2Str(scripts));
        }

        Set<String> getModules() {
            return modules;
        }

        Set<String> getScripts() {
            return scripts;
        }

        Set<String> getClasses() {
            return classes;
        }

        ThemeData getTheme() {
            return theme;
        }

        String getRoute() {
            return route;
        }

        String getLayout() {
            return layout;
        }

        String getName() {
            return name;
        }

        private String col2Str(Collection<String> s) {
            return String.join("\n          ", s);
        }
    }

    /**
     * A container for Theme information when scanning the class path. It
     * overrides equals and hashCode in order to use HashSet to eliminate
     * duplicates.
     */
    static class ThemeData implements Serializable {
        private String name;
        private String variant = "";
        private boolean notheme;

        String getName() {
            return name;
        }

        String getVariant() {
            return variant;
        }

        boolean isNotheme() {
            return notheme;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof ThemeData)) {
                return false;
            }
            ThemeData that = (ThemeData) other;
            return notheme == that.notheme && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            // We might need to add variant when we wanted to fail in the
            // case of
            // same theme class with different variant, which was right in
            // v13
            return Objects.hash(name, notheme);
        }

        @Override
        public String toString() {
            return " theme.notheme: " + notheme + "\n theme.name: " + name + "\n theme.variant: "
                    + variant;
        }
    }

    private final String className;
    private final EndPointData endPoint;
    private final MethodVisitor methodVisitor;
    private final AnnotationVisitor annotationVisitor;
    private final AnnotationVisitor routeVisitor;
    private final AnnotationVisitor themeRouteVisitor;
    private final AnnotationVisitor themeLayoutVisitor;
    private final AnnotationVisitor jsModuleVisitor;
    private final AnnotationVisitor jScriptVisitor;
    private final Set<String> children = new HashSet<>();

    /**
     * Create a new {@link ClassVisitor} that will be used for visiting a
     * specific class.
     *
     * @param className
     *            the class to visit
     * @param endPoint
     *            the end-point object that will be updated during the visit
     */
    FrontendClassVisitor(String className, EndPointData endPoint) { // NOSONAR
        super(Opcodes.ASM7);
        this.className = className;
        this.endPoint = endPoint;

        // Visitor for each method in the class.
        methodVisitor = new MethodVisitor(Opcodes.ASM7) {
            // We are interested in the new instances created inside the method
            @Override
            public void visitTypeInsn(int opcode, String type) {
                addSignatureToClasses(children, type);
            }

            // We are interested in method instructions like
            // Notification.show('bla')
            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                    String descriptor, boolean isInterface) {
                addSignatureToClasses(children, owner);
                addSignatureToClasses(children, descriptor);
            }

            // Visit instructions that stores something in a field inside the
            // method
            @Override
            public void visitFieldInsn(int opcode, String owner, String name,
                    String descriptor) {
                addSignatureToClasses(children, owner);
                addSignatureToClasses(children, descriptor);
            }
        };
        // Visitor for each annotation in the class.
        routeVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (LAYOUT.equals(name)) {
                    endPoint.layout = ((Type) value).getClassName();
                    children.add(endPoint.layout);
                }
                if (VALUE.equals(name)) {
                    endPoint.route = value.toString();
                }
            }
        };
        // Visitor for @Theme annotations in classes annotated with @Route
        themeRouteVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (VALUE.equals(name)) {
                    endPoint.theme.name = ((Type) value).getClassName();
                    children.add(endPoint.theme.name);
                } else if (VARIANT.equals(name)) {
                    endPoint.theme.variant = value.toString();
                }
            }
        };
        // Visitor for @Theme annotations in classes extending RouterLayout
        themeLayoutVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (VALUE.equals(name) && endPoint.theme.name == null) {
                    themeRouteVisitor.visit(name, value);
                } else if (VARIANT.equals(name)
                        && endPoint.theme.variant.isEmpty()) {
                    themeRouteVisitor.visit(name, value);
                }
            }
        };
        // Visitor for @JsModule annotations
        jsModuleVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                endPoint.modules.add(value.toString());
            }
        };
        // Visitor for @JavaScript annotations
        jScriptVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                endPoint.scripts.add(value.toString());
            }
        };
        // Visitor all other annotations
        annotationVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (value != null && !value.getClass().isPrimitive()
                        && !value.getClass().equals(String.class)) {
                    addSignatureToClasses(children, value.toString());
                }
            }
        };

    }

    // Executed for the class definition info.
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        addSignatureToClasses(children, superName);
    }

    // Executed for each method defined in the class.
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        addSignatureToClasses(children, descriptor);
        return methodVisitor;
    }

    // Executed for each annotation in the class.
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor,
            boolean visible) {
        addSignatureToClasses(children, descriptor);

        // We return different visitor implementations depending on the
        // annotation
        String cname = descriptor.replace("/", ".");
        if (className.equals(endPoint.name)
                && cname.contains(Route.class.getName())) {
            return routeVisitor;
        }
        if (cname.contains(JsModule.class.getName())) {
            return jsModuleVisitor;
        }
        if (cname.contains(JavaScript.class.getName())) {
            return jScriptVisitor;
        }
        if (cname.contains(NoTheme.class.getName())) {
            if (className.equals(endPoint.name)) {
                endPoint.theme.notheme = true;
            }
            return null;
        }
        if (cname.contains(Theme.class.getName())) {
            if (className.equals(endPoint.name)) {
                return themeRouteVisitor;
            }
            if (className.equals(endPoint.layout)) {
                return themeLayoutVisitor;
            }
        }
        // default visitor
        return annotationVisitor;
    }

    // Executed for each field defined in the class.
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
            String signature, Object value) {
        addSignatureToClasses(children, descriptor);
        return null;
    }

    /**
     * Return all discovered classes in the visit.
     *
     * @return used classes
     */
    public Set<String> getChildren() {
        return children;
    }

    /**
     * Inspects the type description of a java field or a method type visited by
     * the class visitor. It extracts all class names referenced in the
     * signature and add updates the class collection passed as argument.
     *
     * Typical description formats are:
     * <code>([Lcom/vaadin/flow/component/Component;)V</code>
     *
     * For more info about the format visit the {@link Type} class or the test.
     *
     * @param classes
     *            collection to update with the classes present in the signature
     * @param signature
     *            the java signature to analyze
     */
    void addSignatureToClasses(Set<String> classes, String signature) {
        if (signature == null || signature.isEmpty()) {
            return;
        }
        // This regular expression is able to split the signature and remove
        // primitive and other mark symbols, see test for more info.
        String[] tmp = signature.replace("/", ".").split(
                "(^\\([\\[ZBFDJICL]*|^[\\[ZBFDJICL]+|;?\\)[\\[ZBFDJICLV]*|;[\\[ZBFDJICL]*)");
        classes.addAll(Arrays.asList(tmp));
    }
}
