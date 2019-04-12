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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Represents the class dependency tree of the application.
 */
public class FrontendDependencies {

    /**
     * A simple bean with information representing an application end-point,
     * i.e. those classes annotated with the {@link Route} annotation.
     */
    private static class EndPoint {
        final String name;
        String route = "";
        boolean notheme = false;
        String theme;
        String variant;
        String layout;
        Set<String> classes = new HashSet<>();
        Set<String> packages = new HashSet<>();
        Set<String> modules = new HashSet<>();
        HashMap<String, Set<String>> imports = new HashMap<>();
        Set<String> scripts = new HashSet<>();
        Set<String> npmClasses = new HashSet<>();

        public EndPoint(Class<?> clazz) {
            this.name = clazz.getName();
        }

        @Override
        public String toString() {
            // Used for debugging.
            return String.format(
                    "%n view: %s%n route: %s%n notheme: %b%n theme: %s%n variant: %s%n layout: %s%n imports: %s%n pckages: %s%n modules: %s%n scripts: %s%n classes: %s%n npmclzs: %s%n",
                    name, route, notheme, theme, variant, layout, hash2Str(imports), col2Str(packages), col2Str(modules),
                    col2Str(scripts), col2Str(classes), col2Str(npmClasses));
        }

        private String col2Str(Collection<String> s) {
            return new ArrayList<String>(s).toString().replaceAll("^\\[|\\]$|,", "\n         ").trim();
        }

        private String hash2Str(HashMap<String, Set<String>> h) {
            String r = "";
            for (Entry<String, Set<String>> e : h.entrySet()) {
                r += "\n    " + e.getKey() + "\n          " + col2Str(e.getValue());
            }
            return r;
        }
    }

    private final ClassFinder finder;
    private final HashMap<String, EndPoint> endPoints = new HashMap<>();

    /**
     * Default constructor.
     *
     * @param finder
     *            the class finder used in the application
     */
    public FrontendDependencies(ClassFinder finder) {
        this.finder = finder;
        try {
            for (Class<?> route : finder.getAnnotatedClasses(Route.class) ) {
                String className = route.getName();
                endPoints.put(className, visitClass(className, new EndPoint(route)));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Set<String> getAllPackages() {
        Set<String> all = new HashSet<>();
        for (EndPoint r : endPoints.values()) {
            all.addAll(r.packages);
        }
        return all;
    }
    public Set<String> getAllModules() {
        Set<String> all = new HashSet<>();
        for (EndPoint r : endPoints.values()) {
            all.addAll(r.modules);
        }
        return all;
    }
    public Set<String> getAllScripts() {
        Set<String> all = new HashSet<>();
        for (EndPoint r : endPoints.values()) {
            all.addAll(r.scripts);
        }
        return all;
    }
    public Set<String> getAllImports() {
        Set<String> all = new HashSet<>();
        for (EndPoint r : endPoints.values()) {
            for (Entry<String, Set<String>> e : r.imports.entrySet()) {
                if (!r.npmClasses.contains(e.getKey())) {
                    all.addAll(e.getValue());
                }
            }
        }
        return all;
    }

    public Class<?> getTheme() {
        for (EndPoint r : endPoints.values()) {
            if (r.route.isEmpty() && !r.notheme) {
                try {
                    return finder.loadClass(r.theme != null ? r.theme : "");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private boolean isVisitable(String className) throws ClassNotFoundException {
        if (className == null || className.isEmpty()) {
            return false;
        }
        Class<?> clazz = finder.loadClass(className);
        // We only visit components and themes
        return HasElement.class.isAssignableFrom(clazz) || AbstractTheme.class.isAssignableFrom(clazz);
    }

    private URL getUrl(String className) {
        return this.getClass().getClassLoader().getResource(className.replace(".", "/") + ".class");
    }

    // Inspect a java signature string to extract all class names in it.
    static void addSignatureToClasses(Set<String> classes, String signature) {
        // This regular expression is able to split the signature and remove
        // primitive and other mark symbols, see test for more info.
        String[] tmp = signature.replace("/", ".").split("(^\\([\\[ZBFDJICL]*|^[\\[ZBFDJICL]+|;?\\)[\\[ZBFDJICLV]*|;[\\[ZBFDJICL]*)");
        classes.addAll(Arrays.asList(tmp));
    }

    /**
     * Recursive method for visiting class names using bytecode inspection.
     *
     * @param className
     * @param endPoint
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private EndPoint visitClass(String className, EndPoint endPoint)
                    throws IOException, ClassNotFoundException {

        if (endPoint.classes.contains(className)) {
            return endPoint;
        }
        endPoint.classes.add(className);

        URL url = getUrl(className);
        if (url == null) {
            return endPoint;
        }

        Set<String> children = new HashSet<>();

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM7) {

            class FlowAnnotationVisitor extends AnnotationVisitor {
                public FlowAnnotationVisitor() {
                    super(Opcodes.ASM7);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return this;
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return this;
                }
            }

            MethodVisitor methodVisitor = new MethodVisitor(Opcodes.ASM7) {
                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addSignatureToClasses(children, type);
                }
            };

            AnnotationVisitor annotationVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    if (value != null && !value.getClass().isPrimitive() && !value.getClass().equals(String.class)) {
                        addSignatureToClasses(children, value.toString());
                    }
                }
            };

            AnnotationVisitor routeVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    if ("layout".equals(name)) {
                        endPoint.layout = ((Type) value).getClassName();
                        children.add(endPoint.layout);
                    }
                    if ("value".equals(name)) {
                        endPoint.route = value.toString();
                    }
                }
            };
            AnnotationVisitor themeRouteVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        endPoint.theme = ((Type) value).getClassName();
                        children.add(endPoint.theme);
                        endPoint.variant = "";
                    }
                    if ("variant".equals(name)) {
                        endPoint.variant = value.toString();
                    }
                }
            };
            AnnotationVisitor themeLayoutVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        if (endPoint.theme == null) {
                            endPoint.theme = ((Type) value).getClassName();
                        }
                    }
                    if ("variant".equals(name)) {
                        if (endPoint.variant == null) {
                            endPoint.variant = value.toString();
                        }
                    }
                }
            };
            AnnotationVisitor htmlImportVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    Set<String> set = endPoint.imports.get(className);
                    if (set == null) {
                        set = new HashSet<>();
                        endPoint.imports.put(className, set);
                    }
                    set.add(value.toString());
                }
            };
            AnnotationVisitor packageVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    endPoint.packages.add(value.toString());
                }
            };
            AnnotationVisitor jsModuleVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    endPoint.modules.add(value.toString());
                }
            };
            AnnotationVisitor jScriptVisitor = new FlowAnnotationVisitor() {
                @Override
                public void visit(String name, Object value) {
                    endPoint.scripts.add(value.toString());
                }
            };

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                addSignatureToClasses(children, superName);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                addSignatureToClasses(children, descriptor);
                return methodVisitor;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                addSignatureToClasses(children, descriptor);
                String cname = descriptor.replace("/", ".");
                if (className.equals(endPoint.name) && cname.contains(Route.class.getName())) {
                    return routeVisitor;
                }
                if (cname.contains(HtmlImport.class.getName())) {
                    return htmlImportVisitor;
                }
                if (cname.contains(NpmPackage.class.getName())) {
                    endPoint.npmClasses.add(className);
                    return packageVisitor;
                }
                if (cname.contains(JsModule.class.getName())) {
                    endPoint.npmClasses.add(className);
                    return jsModuleVisitor;
                }
                if (cname.contains(JavaScript.class.getName())) {
                    return jScriptVisitor;
                }
                if (cname.contains(NoTheme.class.getName())) {
                    if (className.equals(endPoint.name)) {
                        endPoint.notheme = true;
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
                return annotationVisitor;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                addSignatureToClasses(children, descriptor);
                return null;
            }
        };

        ClassReader cr = new ClassReader(url.openStream());
        cr.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        for (String s : children) {
            if (isVisitable(s)) {
                visitClass(s, endPoint);
            }
        }

        if (className.equals(endPoint.name)) {
            if (!endPoint.notheme && endPoint.route.isEmpty() && endPoint.theme != null) {
                visitClass(endPoint.theme, endPoint);
            }
        }

        return endPoint;
    }

    @Override
    public String toString() {
        return endPoints.toString();
    }
}
