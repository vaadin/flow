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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.Template;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Copies template files to the target folder so as to be available for parsing
 * at runtime in production mode.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskCopyTemplateFiles implements FallibleCommand {

    private final ClassFinder classFinder;
    private final Options options;

    TaskCopyTemplateFiles(ClassFinder classFinder, Options options) {
        this.classFinder = classFinder;
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        Set<Class<?>> classes = new HashSet<>(
                classFinder.getSubTypesOf(Template.class));
        Class<? extends Annotation> jsModuleAnnotationClass;
        try {
            jsModuleAnnotationClass = classFinder
                    .loadClass(JsModule.class.getName());
        } catch (ClassNotFoundException e) {
            throw new ExecutionFailedException(e);
        }

        for (Class<?> clazz : classes) {
            for (Annotation jsmAnnotation : clazz
                    .getAnnotationsByType(jsModuleAnnotationClass)) {
                String path = getJsModuleAnnotationValue(jsmAnnotation);
                File source = FrontendUtils.resolveFrontendPath(
                        options.getNpmFolder(), path,
                        options.getFrontendDirectory());
                if (source == null) {
                    throw new ExecutionFailedException(
                            "Unable to locate file " + path);
                }

                File templateDirectory;
                if (options.isDevBundleBuild()) {
                    templateDirectory = new File(
                            DevBundleUtils.getDevBundleFolder(
                                    options.getNpmFolder(),
                                    options.getBuildDirectoryName()),
                            Constants.TEMPLATE_DIRECTORY);
                } else {

                    templateDirectory = new File(
                            options.getResourceOutputDirectory(),
                            Constants.TEMPLATE_DIRECTORY);
                }
                Path targetFile = templateDirectory.toPath().resolve(path);
                try {
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(source.toPath(), targetFile);
                } catch (IOException e) {
                    throw new ExecutionFailedException(e);
                }
            }
        }
    }

    private String getJsModuleAnnotationValue(Annotation jsmAnnotation)
            throws ExecutionFailedException {
        try {
            Object value = jsmAnnotation.getClass().getDeclaredMethod("value")
                    .invoke(jsmAnnotation);
            return (String) value;
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new ExecutionFailedException(e);
        }
    }
}
