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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * A class visitor for Flow components.
 */
class FrontendClassVisitor extends ClassVisitor {

    private class FrontendAnnotationVisitor extends AnnotationVisitor {
        public FrontendAnnotationVisitor() {
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

    /**
     * A simple container with the information related to an application end-point,
     * i.e. those classes annotated with the {@link Route} annotation.
     */
    static class EndPointData {
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

        public EndPointData(Class<?> clazz) {
            this.name = clazz.getName();
        }

        // Used for debugging
        @Override
        public String toString() {
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

    private final String className;
    private final FrontendClassVisitor.EndPointData endPoint;
    private final MethodVisitor methodVisitor;
    private final AnnotationVisitor annotationVisitor;
    private final AnnotationVisitor routeVisitor;
    private final AnnotationVisitor themeRouteVisitor;
    private final AnnotationVisitor themeLayoutVisitor;
    private final AnnotationVisitor htmlImportVisitor;
    private final AnnotationVisitor packageVisitor;
    private final AnnotationVisitor jsModuleVisitor;
    private final AnnotationVisitor jScriptVisitor;
    private final Set<String> children = new HashSet<>();

    /**
     * Constructor.
     *
     * @param className
     *            the class to visit
     * @param endPoint
     *            the end-point object that will be updated in the visit
     */
    FrontendClassVisitor(String className, FrontendClassVisitor.EndPointData endPoint) {
        super(Opcodes.ASM7);
        this.className = className;
        this.endPoint = endPoint;
        methodVisitor = new MethodVisitor(Opcodes.ASM7) {
            @Override
            public void visitTypeInsn(int opcode, String type) {
                addSignatureToClasses(children, type);
            }
        };
        annotationVisitor = new FrontendAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (value != null && !value.getClass().isPrimitive() && !value.getClass().equals(String.class)) {
                    addSignatureToClasses(children, value.toString());
                }
            }
        };
        routeVisitor = new FrontendAnnotationVisitor() {
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
        themeRouteVisitor = new FrontendAnnotationVisitor() {
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
        themeLayoutVisitor = new FrontendAnnotationVisitor() {
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
        htmlImportVisitor = new FrontendAnnotationVisitor() {
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
        packageVisitor = new FrontendAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                endPoint.packages.add(value.toString());
            }
        };
        jsModuleVisitor = new FrontendAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                endPoint.modules.add(value.toString());
            }
        };
        jScriptVisitor = new FrontendAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                endPoint.scripts.add(value.toString());
            }
        };
    }

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

    /**
     * Return all discovered classes in the visit.
     *
     * @return used classes
     */
    public Set<String> getChildren() {
        return children;
    }

    /**
     * Inspects a java signature string to extract all class names in it.
     *
     * @param classes
     *            collection to update with the classes present in the signature
     * @param signature
     *            the java signature to analyze
     */
    void addSignatureToClasses(Set<String> classes, String signature) {
        // This regular expression is able to split the signature and remove
        // primitive and other mark symbols, see test for more info.
        String[] tmp = signature.replace("/", ".")
                .split("(^\\([\\[ZBFDJICL]*|^[\\[ZBFDJICL]+|;?\\)[\\[ZBFDJICLV]*|;[\\[ZBFDJICL]*)");
        classes.addAll(Arrays.asList(tmp));
    }
}
