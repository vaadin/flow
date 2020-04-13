/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.Attribute;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

/**
 * A class visitor for annotated classes. It's used to visit multiple classes
 * and extract all the properties of an specific annotation defined in the
 * constructor.
 *
 * @since 2.0
 */
final class FrontendAnnotatedClassVisitor extends ClassVisitor {
    private static final int API = Opcodes.ASM7;
    private final String annotationName;
    private final List<HashMap<String, Object>> data = new ArrayList<>();
    private final ClassFinder finder;
    private static Map<String, Map<String, Object>> annotationDefaults = new HashMap<>();

    /**
     * Create a new {@link ClassVisitor} that will be used for visiting a
     * specific class to get the data of an annotation.
     *
     * @param finder
     *            The class finder to use
     * 
     * @param annotationName
     *            The annotation class name to visit
     */
    FrontendAnnotatedClassVisitor(ClassFinder finder, String annotationName) {
        super(API);
        this.finder = finder;
        this.annotationName = annotationName;
        if (!annotationDefaults.containsKey(annotationName)) {
            annotationDefaults.put(annotationName,
                    readAnnotationDefaultValues(annotationName));
        }
    }

    private Map<String, Object> readAnnotationDefaultValues(
            String annotationName) {
        System.err.println("Reading default values for " + annotationName);
        Map<String, Object> defaults = new HashMap<>();

        visitClass(annotationName, new ClassVisitor(api) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName,
                    String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(api) {
                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return new AnnotationVisitor(api) {
                            @Override
                            public void visit(String name, Object value) {
                                defaults.put(methodName, value);
                            }

                            @Override
                            public AnnotationVisitor visitArray(
                                    String arrayName) {
                                return new AnnotationVisitor(api) {
                                    @Override
                                    public void visit(String name,
                                            Object value) {
                                        ((List) defaults.computeIfAbsent(
                                                methodName,
                                                methodName -> new ArrayList<>()))
                                                        .add(value);
                                    }
                                };
                            }
                        };
                    }
                };
            }
        });
        System.err.println(
                "Default values for " + annotationName + ": " + defaults);

        return defaults;
    }

    /**
     * Visit recursively a class to find annotations.
     *
     * @param name
     *            the class name
     * @throws IOException
     *             when the class name is not found
     */
    public void visitClass(String name) {
        visitClass(name, this);
    }

    /**
     * Visit recursively a class to find annotations.
     *
     * @param name
     *            the class name
     * @param visitor
     *            the visitor to use
     * @throws IOException
     *             when the class name is not found
     */
    public void visitClass(String name, ClassVisitor visitor) {
        if (name == null) {
            return;
        }
        try {
            ClassReader cr;
            URL url = finder.getResource(name.replace(".", "/") + ".class");
            cr = new ClassReader(url.openStream());
            cr.accept(visitor, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Executed for the class definition info.
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        visitClass(superName, this);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor,
            boolean visible) {
        String cname = descriptor.replace("/", ".");
        if (cname.contains(annotationName)) {
            return new RepeatedAnnotationVisitor() {
                // initialize for non repeated annotations
                HashMap<String, Object> info = new HashMap<>();

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return new AnnotationVisitor(API) {
                        @Override
                        public void visit(String dummy, Object value) {
                            if (data.indexOf(info) < 0) {
                                data.add(info);
                            }
                            ((List) info.computeIfAbsent(name,
                                    key -> new ArrayList<>())).add(value);
                        }
                    };
                }

                // Visited on each annotation attribute
                @Override
                public void visit(String name, Object value) {
                    if (data.indexOf(info) < 0) {
                        data.add(info);
                    }
                    info.put(name, value);
                }

                // Only visited when annotation is repeated
                @Override
                public AnnotationVisitor visitAnnotation(String name,
                        String descriptor) {
                    // initialize in each repeated annotation occurrence
                    info = new HashMap<>();
                    return this;
                }
            };
        }
        return null;
    }

    /**
     * Return all values of a repeated annotation parameter. For instance
     * `getValues("value")` will return 'Bar' and 'Baz' when we have the
     * following code:
     *
     * <pre>
     * <code>
     * &#64;MyAnnotation(value = "Bar", other = "aa")
     * &#64;MyAnnotation(value = "Baz", other = "bb")
     * class Foo {
     * }
     * </code>
     * </pre>
     *
     *
     * @param parameter
     *            the annotation parameter used for getting values
     * @return a set of all values found
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getValues(String parameter) {
        return (Set<T>) data.stream().filter(h -> h.containsKey(parameter))
                .map(h -> h.get(parameter)).collect(Collectors.toSet());
    }

    /**
     * Return the values of a an annotation parameter.
     *
     * @throws IllegalArgumentException
     *             if there is not one single annotation
     * @param parameter
     *            the annotation parameter used for getting values
     * @return the value from the annotation
     */
    public <T> T getValue(String parameter) {
        if (data.size() != 1) {
            throw new IllegalArgumentException(
                    "getValue can only be used when there is one annotation. There are "
                            + data.size() + " instances of " + annotationName);
        }
        Set<T> values = getValues(parameter);
        if (values.isEmpty()) {
            System.err.println("No value for " + parameter + " using default: "
                    + getDefault(parameter));
            return (T) getDefault(parameter);
        }
        return values.iterator().next();
    }

    private <T> T getDefault(String parameter) {
        return (T) annotationDefaults.get(annotationName).get(parameter);
    }

    /**
     * Return all parameter values of a repeated annotation when they share the
     * same value for a key parameter. For example `getValuesForKey("value",
     * "foo", "other")` will return 'aa' and 'bb' if we have the following code:
     *
     * <pre>
     * <code>
     * &#64;MyAnnotation(value = "foo", other = "aa")
     * &#64;MyAnnotation(value = "foo", other = "bb")
     * class Bar {
     * }
     * </code>
     * </pre>
     *
     * @param key
     *            the parameter name which all annotations share the same value
     * @param value
     *            the shared value
     * @param property
     *            the parameter name of the value to return
     * @return a set of all values found
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getValuesForKey(String key, String value,
            String property) {
        return (Set<T>) data.stream()
                .filter(h -> h.containsKey(key) && h.get(key).equals(value))
                .map(h -> h.get(property)).collect(Collectors.toSet());
    }
}
