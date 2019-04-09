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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;

/**
 * @author Vaadin Ltd
 *
 */
public class WebComponentModulesGenerator extends ClassPathIntrospector {

    private static final String GENERATE_MODULE_METHOD = "generateModule";

    private static final String ABSENT_METHOD_ERROR = String.format(
            "There is no method '%s' in the class '%s', consider updating flow-server dependency",
            GENERATE_MODULE_METHOD, WebComponentExporter.class.getName());

    /**
     * Prepares the class to find web component exporters from the project
     * classes specified.
     *
     * @param introspector
     *            another introspector whose reflection tools will be reused to
     *            find the web component exporters
     */
    public WebComponentModulesGenerator(ClassPathIntrospector introspector) {
        super(introspector);
    }

    /**
     * Discovers all non-abstract web component exporters in the classpath.
     *
     * @return stream of all non abstract web component exporters found in the
     *         class path
     */
    @SuppressWarnings("unchecked")
    public Stream<Class<? extends WebComponentExporter<? extends Component>>> getExporters() {
        return getSubtypes(WebComponentExporter.class)
                .filter(clazz -> !clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers()))
                .map(clazz -> (Class<? extends WebComponentExporter<? extends Component>>) clazz);
    }

    /**
     * Generate a file with web component html/JS content for given exporter
     * class in the given {@code outputFolder}
     *
     * @param clazz
     *            web component exporter class
     * @return the generated module content
     */
    public File generateModuleFile(
            Class<? extends WebComponentExporter<? extends Component>> clazz,
            File outputFolder) {
        String tag = getTag(clazz);

        String fileName = tag;
        int index = 1;
        Path generatedFile;
        do {
            generatedFile = outputFolder.toPath().resolve(fileName + ".html");
            index++;
            fileName = tag + index;
        } while (generatedFile.toFile().exists());
        try {
            Files.write(generatedFile,
                    Collections.singletonList(generateModule(clazz)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private String generateModule(
            Class<? extends WebComponentExporter<? extends Component>> clazz) {
        Class<?> generatorClass = loadClassInProjectClassLoader(
                WebComponentGenerator.class.getName());
        Method generateMethod = Stream.of(generatorClass.getDeclaredMethods())
                .filter(method -> method.getParameters().length == 2
                        && method.getName().equals(GENERATE_MODULE_METHOD))
                .findFirst().orElseThrow(
                        () -> new IllegalStateException(ABSENT_METHOD_ERROR));

        try {
            return (String) generateMethod.invoke(null, clazz, "");
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException exception) {
            throw new RuntimeException(String.format(
                    "Unable to invoke '%s::%s' for the exporter type '%s'",
                    WebComponentGenerator.class.getName(),
                    GENERATE_MODULE_METHOD, clazz.getName()), exception);
        }
    }

    private String getTag(
            Class<? extends WebComponentExporter<? extends Component>> clazz) {
        Annotation tagAnnotation = clazz.getAnnotation(
                loadClassInProjectClassLoader(Tag.class.getName()));
        return invokeAnnotationMethod(tagAnnotation, "value");
    }
}
