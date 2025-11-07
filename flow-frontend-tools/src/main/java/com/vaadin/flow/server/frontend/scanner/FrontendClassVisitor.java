/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend.scanner;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * A class visitor for Flow components.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
final class FrontendClassVisitor extends ClassVisitor {

    private static final Pattern SIGNATURE_PATTERN = Pattern.compile(
            "(^\\([\\[ZBFDJICL]*|^[\\[ZBFDJICL]+|;?\\)[\\[ZBFDJICLV]*|;[\\[ZBFDJICL]*)");
    private static final String VARIANT = "variant";
    private static final String LAYOUT = "layout";
    static final String VALUE = "value";
    static final String THEME_CLASS = "themeClass";
    static final String VERSION = "version";
    static final String ASSETS = "assets";
    static final String DEV = "dev";
    static final String ID = "id";
    static final String INCLUDE = "include";
    static final String THEME_FOR = "themeFor";

    private final MethodVisitor methodVisitor;
    private final AnnotationVisitor annotationVisitor;
    private final AnnotationVisitor routeVisitor;
    private final AnnotationVisitor themeVisitor;
    private final AnnotationVisitor jsModuleVisitor;
    private final AnnotationVisitor jScriptVisitor;
    private ClassInfo classInfo;

    private static final class JSAnnotationVisitor
            extends RepeatedAnnotationVisitor {

        boolean currentDevOnly = false;
        private String currentModule;

        private LinkedHashSet<String> target;
        private LinkedHashSet<String> targetDevelopmentOnly;

        public JSAnnotationVisitor(LinkedHashSet<String> target,
                LinkedHashSet<String> targetDevelopmentOnly) {
            this.target = target;
            this.targetDevelopmentOnly = targetDevelopmentOnly;
        }

        @Override
        public void visit(String name, Object value) {
            if (name.equals("developmentOnly")) {
                Boolean developmentOnly = (Boolean) value;
                if (developmentOnly != null && developmentOnly) {
                    currentDevOnly = true;
                }
            } else if (name.equals("value")) {
                currentModule = value.toString();
            }
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (currentModule != null) {
                // This visitor is called also for the $Container annotation
                if (currentDevOnly) {
                    targetDevelopmentOnly.add(currentModule);
                } else {
                    target.add(currentModule);
                }
            }
            currentModule = null;
            currentDevOnly = false;
        }

    }

    private final class FrontendMethodVisitor extends MethodVisitor {
        public FrontendMethodVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor,
                boolean visible) {
            addSignatureToClasses(classInfo.children, descriptor);
            return annotationVisitor;
        }

        // We are interested in the new instances created inside the method
        @Override
        public void visitTypeInsn(int opcode, String type) {
            addSignatureToClasses(classInfo.children, type);
        }

        // We are interested in method instructions like
        // Notification.show('bla')
        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String descriptor, boolean isInterface) {
            addSignatureToClasses(classInfo.children, owner);
            addSignatureToClasses(classInfo.children, descriptor);
        }

        // Visit instructions that stores something in a field inside the
        // method
        @Override
        public void visitFieldInsn(int opcode, String owner, String name,
                String descriptor) {
            addSignatureToClasses(classInfo.children, owner);
            addSignatureToClasses(classInfo.children, descriptor);
        }

        // Visit arguments, we only care those arguments that are Types,
        // e.g dynamic-routes #5509, or factory-beans #5658
        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Type) {
                addSignatureToClasses(classInfo.children, value.toString());
            }
        }

        // Visit dynamic invocations and method references. In particular, we
        // are interested in the case Supplier<Component> s = MyComponent::new;
        // flow #6524
        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor,
                Handle bootstrapMethodHandle,
                Object... bootstrapMethodArguments) {
            addSignatureToClasses(classInfo.children, descriptor);
            addSignatureToClasses(classInfo.children,
                    bootstrapMethodHandle.getOwner());
            addSignatureToClasses(classInfo.children,
                    bootstrapMethodHandle.getDesc());
            for (Object obj : bootstrapMethodArguments) {
                if (obj instanceof Type) {
                    addSignatureToClasses(classInfo.children, obj.toString());
                } else if (obj instanceof Handle) {
                    // The owner of the Handle is the reference information
                    addSignatureToClasses(classInfo.children,
                            ((Handle) obj).getOwner());
                    // the descriptor for the Handle won't be scanned, as it
                    // adds from +10% to 40% to the execution time and does not
                    // affect the fix in itself
                }
                // the case for ConstantDynamic is also skipped for
                // performance reasons. It does not directly affect the fix
                // and slows down the execution.
            }
        }
    }

    /**
     * Create a new {@link ClassVisitor} that will be used for visiting a
     * specific class.
     *
     * @param className
     *            the class to visit
     * @param classInfo
     *            data object where discovered information is stored
     */
    FrontendClassVisitor(ClassInfo classInfo) {
        super(Opcodes.ASM9);
        this.classInfo = classInfo;
        // Visitor for each method in the class.
        methodVisitor = new FrontendMethodVisitor();
        // Visitor for each annotation in the class.
        routeVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (LAYOUT.equals(name)) {
                    classInfo.layout = ((Type) value).getClassName();
                    classInfo.children.add(classInfo.layout);
                }
                if (VALUE.equals(name)) {
                    classInfo.route = value.toString();
                }
            }
        };
        // Visitor for @Theme annotations in classes annotated with @Route
        themeVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (VALUE.equals(name)) {
                    classInfo.theme.themeName = (String) value;
                } else if (THEME_CLASS.equals(name)) {
                    classInfo.theme.themeClass = ((Type) value).getClassName();
                    classInfo.children.add(classInfo.theme.themeClass);
                } else if (VARIANT.equals(name)) {
                    classInfo.theme.variant = value.toString();
                }
            }
        };
        // Visitor for @JsModule annotations
        jsModuleVisitor = new JSAnnotationVisitor(classInfo.modules,
                classInfo.modulesDevelopmentOnly);
        // Visitor for @JavaScript annotations
        jScriptVisitor = new JSAnnotationVisitor(classInfo.scripts,
                classInfo.scriptsDevelopmentOnly);
        // Visitor all other annotations
        annotationVisitor = new RepeatedAnnotationVisitor() {
            @Override
            public void visit(String name, Object value) {
                if (value != null && !value.getClass().isPrimitive()
                        && !value.getClass().equals(String.class)) {
                    addSignatureToClasses(classInfo.children, value.toString());
                }
            }
        };
    }

    // Executed for the class definition info.
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        addSignatureToClasses(classInfo.children, superName);

        for (String implementedInterface : interfaces) {
            addSignatureToClasses(classInfo.children, implementedInterface);
        }
    }

    // Executed for each method defined in the class.
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        addSignatureToClasses(classInfo.children, descriptor);
        return methodVisitor;
    }

    // Executed for each annotation in the class.
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor,
            boolean visible) {
        addSignatureToClasses(classInfo.children, descriptor);

        // We return different visitor implementations depending on the
        // annotation
        String annotationClassName = descriptor.replace("/", ".");
        if (annotationClassName.contains(Route.class.getName())) {
            return routeVisitor;
        }
        if (annotationClassName.contains(JsModule.class.getName())) {
            return jsModuleVisitor;
        }
        if (annotationClassName.contains(JavaScript.class.getName())) {
            return jScriptVisitor;
        }
        if (annotationClassName.contains(NoTheme.class.getName())) {
            classInfo.theme.notheme = true;
            return null;
        }
        if (annotationClassName.contains(Theme.class.getName())) {
            return themeVisitor;
        }
        if (annotationClassName.contains(CssImport.class.getName())) {
            return new CssAnnotationVisitor(classInfo.css);
        }
        // default visitor
        return annotationVisitor;
    }

    // Executed for each field defined in the class.
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
            String signature, Object value) {
        addSignatureToClasses(classInfo.children, descriptor);
        return null;
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
        String[] tmp = SIGNATURE_PATTERN.split(signature.replace("/", "."));
        for (String cls : tmp) {
            if (!cls.isBlank()) {
                classes.add(cls);
            }
        }
    }
}
