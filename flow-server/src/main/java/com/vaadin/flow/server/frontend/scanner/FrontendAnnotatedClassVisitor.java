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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.jfr.StackTrace;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class visitor for annotated classes. It's used to visit multiple classes
 * and extract all the properties of an specific annotation defined in the
 * constructor.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
final class FrontendAnnotatedClassVisitor extends ClassVisitor {
    private final Map<String, Map<String, Object>> annotationDefaults = new HashMap<>();
    private final String annotationName;
    private final List<HashMap<String, Object>> data = new ArrayList<>();
    private final ClassFinder finder;

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
        super(Opcodes.ASM9);
        this.finder = finder;
        this.annotationName = annotationName;
        if (!annotationDefaults.containsKey(annotationName)) {
            annotationDefaults.put(annotationName,
                    readAnnotationDefaultValues(annotationName));
        }
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
     * @throws UncheckedIOException
     *             when the class name is not found
     */
    public void visitClass(String name, ClassVisitor visitor) {
        if (name == null) {
            return;
        }
        try {
            URL url = finder.getResource(name.replace(".", "/") + ".class");
            try (InputStream is = url.openStream()) {
                ClassReader cr = new ClassReader(is);
                cr.accept(visitor, 0);
            }
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
        String cname = descriptor.replaceFirst("^L(.*);$", "$1").replace("/",
                ".");
        if (cname.equals(annotationName)) {
            return new DataAnnotationVisitor(data, false);
        } else if (cname.equals(annotationName + "$Container")) {
            return new DataAnnotationVisitor(data, true);
        }
        return null;
    }

    /**
     * Return all explicitly defined values of a repeated annotation parameter
     * in the occurrence order, ignoring attribute default values. For instance
     * `getValues("value")` will return 'Bar' and 'Baz' when we have the
     * following code:
     *
     * <pre>
     * <code>
     * &#64;interface MyAnnotation {
     *    String value() default "Foo";
     *    String other();
     * }
     *
     * &#64;MyAnnotation(value = "Bar", other = "aa")
     * &#64;MyAnnotation(value = "Baz", other = "bb")
     * &#64;MyAnnotation(other = "cc")
     * class Foo {
     * }
     * </code>
     * </pre>
     *
     *
     * @param parameter
     *            the annotation parameter used for getting values
     * @return an ordered set of all values found
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getValues(String parameter) {
        return (Set<T>) data.stream().filter(h -> h.containsKey(parameter))
                .map(h -> h.get(parameter))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Return all parameter values of a repeated annotation when they share the
     * same value for a key parameter in the occurrence order. Key parameter
     * must be explicitly defined, otherwise the annotation is ignored. For
     * example `getValuesForKey("value", "foo", "other")` will return 'aa' and
     * 'bb' when we have the following code:
     *
     * <pre>
     * <code>
     * &#64;interface MyAnnotation {
     *    String value() default "foo";
     *    String other();
     * }
     *
     * &#64;MyAnnotation(value = "foo", other = "aa")
     * &#64;MyAnnotation(value = "foo", other = "bb")
     * &#64;MyAnnotation(other = "cc")
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
     * @return an ordered set of all values found
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getValuesForKey(String key, String value,
            String property) {
        return (Set<T>) data.stream()
                .filter(h -> h.containsKey(key) && h.get(key).equals(value))
                .map(h -> h.get(property))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
            getLogger().debug("No value for {} using default: {}", parameter,
                    getDefault(parameter));
            return getDefault(parameter);
        }
        return values.iterator().next();
    }

    private <T> T getDefault(String parameter) {
        return (T) annotationDefaults.get(annotationName).get(parameter);
    }

    private Map<String, Object> readAnnotationDefaultValues(
            String annotationName) {
        getLogger().debug("Reading default values for {}", annotationName);
        Map<String, Object> defaults = new HashMap<>();

        visitClass(annotationName,
                new DefaultsAnnotationClassVisitor(api, defaults));

        getLogger().info("Default values for {}: {}", annotationName, defaults);

        return defaults;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(FrontendAnnotatedClassVisitor.class);
    }

    /**
     * Class visitor for collecting default annotation values.
     */
    private static class DefaultsAnnotationClassVisitor extends ClassVisitor {
        private final Map<String, Object> defaults;

        public DefaultsAnnotationClassVisitor(int api,
                Map<String, Object> defaults) {
            super(api);
            this.defaults = defaults;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName,
                String descriptor, String signature, String[] exceptions) {
            return new DefaultsAnnotationMethodVisitor(api, methodName,
                    defaults);
        }
    }

    /**
     * Method visitor for collecting default annotation values.
     */
    private static class DefaultsAnnotationMethodVisitor extends MethodVisitor {
        private final String methodName;
        private final Map<String, Object> defaults;

        public DefaultsAnnotationMethodVisitor(int api, String methodName,
                Map<String, Object> defaults) {
            super(api);
            this.methodName = methodName;
            this.defaults = defaults;
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new DefaultsAnnotationVisitor(api, methodName, defaults);
        }

    }

    /**
     * Collects default annotation values.
     */
    private static class DefaultsAnnotationVisitor extends AnnotationVisitor {
        private final String methodName;
        private final Map<String, Object> defaults;

        public DefaultsAnnotationVisitor(int api, String methodName,
                Map<String, Object> defaults) {
            super(api);
            this.methodName = methodName;
            this.defaults = defaults;
        }

        @Override
        public void visit(String name, Object value) {
            defaults.put(methodName, value);
        }

        @Override
        public AnnotationVisitor visitArray(String arrayName) {
            List<?> values = new ArrayList<>();
            defaults.put(methodName, values);

            return new ArrayAnnotationVisitor(api, values);
        }
    }

    /**
     * Collects data from possibly repeated annotations.
     */
    private static class DataAnnotationVisitor
            extends RepeatedAnnotationVisitor {
        private final List<HashMap<String, Object>> data;
        private final boolean isRepeatableContainer;
        // initialize for non repeated annotations
        private HashMap<String, Object> info = new HashMap<>();

        DataAnnotationVisitor(List<HashMap<String, Object>> data,
                boolean isRepeatableContainer) {
            this.data = data;
            this.isRepeatableContainer = isRepeatableContainer;
            data.add(info);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if (isRepeatableContainer && !"assets".equals(name)) {
                // For repeatable container annotations, skip array values
                // but use this instance for visiting items
                return this;
            }

            List values = new ArrayList<>();
            info.put(name, values);

            return new ArrayAnnotationVisitor(api, values);
        }

        // Visited on each annotation attribute
        @Override
        public void visit(String name, Object value) {
            info.put(name, value);
        }

        // Only visited when annotation is repeated
        @Override
        public AnnotationVisitor visitAnnotation(String name,
                String descriptor) {
            // initialize in each repeated annotation occurrence
            info = new HashMap<>();
            data.add(info);
            return this;
        }
    }

    /**
     * Collects a list of annotation array values.
     */
    private static class ArrayAnnotationVisitor extends AnnotationVisitor {
        private final List values;

        public ArrayAnnotationVisitor(int api, List values) {
            super(api);
            this.values = values;
        }

        @Override
        public void visit(String name, Object value) {
            values.add(value);
        }
    }
}
