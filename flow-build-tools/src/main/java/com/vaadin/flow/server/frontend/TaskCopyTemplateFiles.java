/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.DevBundleUtils;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.Template;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Copies template files to the target folder so as to be available for parsing
 * at runtime in production mode.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 9.0
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
        File templateDirectory = getTemplateDirectory(options);

        for (Map.Entry<String, String> template : getTemplateJsModules(
                classFinder).entrySet()) {
            String path = template.getKey();
            Path targetFile = templateDirectory.toPath().resolve(path);
            File source = FrontendUtils.resolveFrontendPath(
                    options.getNpmFolder(), path,
                    options.getFrontendDirectory());
            if (source == null) {
                if (Files.exists(targetFile)) {
                    // The file lives in an npm package and the bundle it was
                    // copied into is being reused, so npm install has not been
                    // run and node_modules does not exist. Bundle validation
                    // requires a reused bundle to contain the template, and
                    // unpacking the bundle has put it in place.
                    continue;
                }
                throw new ExecutionFailedException("Unable to locate file "
                        + path + " used by the template class "
                        + template.getValue()
                        + ". Template sources are looked up in node_modules, "
                        + options.getFrontendDirectory()
                        + " and the jar resources folder.");
            }

            try {
                Files.createDirectories(targetFile.getParent());
                Files.copy(source.toPath(), targetFile,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            }
        }
    }

    /**
     * Gets the folder the template sources are copied into, so that the
     * template parser finds them at runtime.
     *
     * @param options
     *            the task options
     * @return the folder for the template sources
     */
    static File getTemplateDirectory(Options options) {
        if (options.isDevBundleBuild()) {
            return new File(
                    DevBundleUtils.getDevBundleFolder(options.getNpmFolder(),
                            options.getBuildDirectoryName()),
                    Constants.TEMPLATE_DIRECTORY);
        }
        return new File(options.getResourceOutputDirectory(),
                Constants.TEMPLATE_DIRECTORY);
    }

    /**
     * Collects the {@code @JsModule} values of all {@link Template} classes.
     * <p>
     * These are the JavaScript sources the template parser reads at runtime to
     * map the {@code @Id} fields of a template to the elements it declares.
     *
     * @param classFinder
     *            the class finder to scan the template classes with
     * @return the {@code @JsModule} values mapped to the name of a class using
     *         them
     * @throws ExecutionFailedException
     *             if the annotation values cannot be read
     */
    static Map<String, String> getTemplateJsModules(ClassFinder classFinder)
            throws ExecutionFailedException {
        Class<? extends Annotation> jsModuleAnnotationClass;
        try {
            jsModuleAnnotationClass = classFinder
                    .loadClass(JsModule.class.getName());
        } catch (ClassNotFoundException e) {
            throw new ExecutionFailedException(e);
        }

        Map<String, String> jsModules = new LinkedHashMap<>();
        for (Class<?> clazz : classFinder.getSubTypesOf(Template.class)) {
            for (Annotation jsmAnnotation : clazz
                    .getAnnotationsByType(jsModuleAnnotationClass)) {
                jsModules.putIfAbsent(getJsModuleAnnotationValue(jsmAnnotation),
                        clazz.getName());
            }
        }
        return jsModules;
    }

    private static String getJsModuleAnnotationValue(Annotation jsmAnnotation)
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
