/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Copies template files to the target folder so as to be available for parsing
 * when in production mode. Only required for Vite builds; with Webpack, sources
 * are parsed from stats.json.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskCopyTemplateFiles implements FallibleCommand {

    private final ClassFinder classFinder;
    private final File projectDirectory;
    private final File resourceOutputDirectory;

    TaskCopyTemplateFiles(ClassFinder classFinder, File projectDirectory,
            File resourceOutputDirectory) {
        this.classFinder = classFinder;
        this.projectDirectory = projectDirectory;
        this.resourceOutputDirectory = resourceOutputDirectory;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        Set<Class<?>> classes = new HashSet<>();
        String litTemplateClass = "com.vaadin.flow.component.littemplate.LitTemplate";
        try {
            classes.addAll(classFinder.getSubTypesOf(litTemplateClass));
        } catch (ClassNotFoundException ignore) { // NOSONAR
            log().debug(litTemplateClass
                    + " not on classpath, skipping copying of Lit template");
        }
        String polymerTemplateClass = "com.vaadin.flow.component.polymertemplate.PolymerTemplate";

        try {
            classes.addAll(classFinder.getSubTypesOf(polymerTemplateClass));
        } catch (ClassNotFoundException ignore) { // NOSONAR
            log().debug(polymerTemplateClass
                    + " not on classpath, skipping copying of Polymer templates");
        }
        for (Class<?> clazz : classes) {
            for (JsModule jsmAnnotation : clazz
                    .getAnnotationsByType(JsModule.class)) {
                String path = jsmAnnotation.value();
                File source = FrontendUtils
                        .resolveFrontendPath(projectDirectory, path);
                if (source == null) {
                    throw new ExecutionFailedException(
                            "Unable to locate file " + path);
                }
                File templateDirectory = new File(resourceOutputDirectory,
                        Constants.TEMPLATE_DIRECTORY);
                File target = new File(templateDirectory, path).getParentFile();
                target.mkdirs();
                try {
                    FileUtils.copyFileToDirectory(source, target);
                } catch (IOException e) {
                    throw new ExecutionFailedException(e);
                }
            }
        }
    }

    Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
