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
package com.vaadin.flow.server.webcomponent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;

/**
 * @author Vaadin Ltd
 * @since
 */
public final class WebComponentModulesWriter implements Serializable {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebComponentModulesWriter.class);

    private WebComponentModulesWriter() {
    }

    /**
     * @param outputFolder
     * @return
     */
    public static Set<File> writeWebComponentsToDirectory(
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporterClasses,
            File outputFolder, boolean bowerMode) {
        return filterConcreteExporters(exporterClasses)
                .map(clazz -> writeWebComponentToDirectory(clazz,
                        outputFolder, bowerMode)).collect(Collectors.toSet());
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Stream<Class<? extends WebComponentExporter<? extends Component>>> filterConcreteExporters(
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporterClasses) {
        return exporterClasses.stream()
                .filter(clazz -> WebComponentExporter.class
                        .isAssignableFrom(clazz) && !clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers()));
    }

    /**
     * Generate a file with web component html/JS content for given exporter
     * class in the given {@code outputFolder}.
     *
     * @param clazz
     *            web component exporter class
     * @param outputFolder
     *            folder into which the generate file is written
     * @return the generated module content
     */
    private static File writeWebComponentToDirectory(
            Class<? extends WebComponentExporter<? extends Component>> clazz,
            File outputFolder, boolean bowerMode) {
        String tag = getTag(clazz);

        String fileName = bowerMode ? tag + ".html" : tag + ".js";
        Path generatedFile = outputFolder.toPath().resolve(fileName);
        if (generatedFile.toFile().exists()) {
            LOGGER.debug("File '{}' already exists in the '{}' directory."
                    + "It might be a previously generated web component "
                    + "module file "
                    + "or it's an imported dependency. The file will be overwritten.",
                    generatedFile.getFileName(), outputFolder.getPath());
        }
        try {
            FileUtils.forceMkdir(generatedFile.getParent().toFile());
            Files.write(generatedFile,
                    Collections.singletonList(generateModule(clazz, bowerMode)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private static String generateModule(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass,
            boolean bowerMode) {
        return WebComponentGenerator.generateModule(exporterClass, "../",
                bowerMode);
    }

    private static String getTag(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        WebComponentExporterTagExtractor exporterTagExtractor = new WebComponentExporterTagExtractor();
        return exporterTagExtractor.apply(exporterClass);
    }

    /**
     *
     */
    public static class ReflectionUsage implements Serializable {
        private static final String WRITE_MODULES_METHOD = "writeWebComponentsToDirectory";

        public static Set<File> reflectiveWriteWebComponentsToDirectory(
                Class<?> writer, Object exporterClasses, Object outputFolder,
                Object bowerMode) {
            Objects.requireNonNull(writer, "Writer cannot be null!");
            if (!WebComponentModulesWriter.class.getName()
                    .equals(writer.getName())) {
                // very bad
            }
            return generateWebComponentModules(writer, exporterClasses,
                    outputFolder,
                    bowerMode);
        }

        @SuppressWarnings("unchecked")
        private static Set<File> generateWebComponentModules(Class<?> writerClass,
                Object exporterClasses, Object outputFolder, Object bowerMode) {
            Method writeMethod = getMethod(writerClass, WRITE_MODULES_METHOD)
                    .orElseThrow(() -> new IllegalStateException(
                            "We went to hell!"));
            try {
                return ((Set<File>) writeMethod.invoke(null, exporterClasses,
                        outputFolder, bowerMode));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(
                        "Could not write exported web " + "component modules!",
                        e);
            }
        }

        private static Optional<Method> getMethod(Class<?> writerClass,
                String methodName) {
            return Stream.of(writerClass.getDeclaredMethods())
                    .filter(method -> method.getName().equals(methodName))
                    .findFirst();
        }
    }
}
