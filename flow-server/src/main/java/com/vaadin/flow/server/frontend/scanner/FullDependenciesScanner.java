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
package com.vaadin.flow.server.frontend.scanner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Full classpath scanner.
 *
 * @author Vaadin Ltd
 *
 */
class FullDependenciesScanner implements FrontendDependenciesScanner {

    private final ClassFinder finder;
    private final boolean generateEmbeddableWebComponents;

    /**
     * Creates a new scanner instance which discovers all dependencies in the
     * classpath.
     *
     * @param finder
     *            a class finder
     * @param generateEmbeddableWebComponents
     *            checks {@code WebComponentExporter} classes for dependencies
     *            if {@code true}, doesn't check otherwise
     */
    FullDependenciesScanner(ClassFinder finder,
            boolean generateEmbeddableWebComponents) {
        this.finder = finder;
        this.generateEmbeddableWebComponents = generateEmbeddableWebComponents;
    }

    @Override
    public Map<String, String> getPackages() {
        try {
            Set<Class<?>> classes = finder
                    .getAnnotatedClasses(NpmPackage.class.getName());
            Map<String, String> result = new HashMap<>();
            for (Class<?> clazz : classes) {
                List<NpmPackage> packages = AnnotationReader
                        .getAnnotationsFor(clazz, NpmPackage.class);
                packages.forEach(
                        pckg -> result.put(pckg.value(), pckg.version()));
            }
            return result;
        } catch (ClassNotFoundException exception) {
            return Collections.emptyMap();
        }
    }

    @Override
    public List<String> getModules() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getScripts() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<CssData> getCss() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ThemeDefinition getThemeDefinition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AbstractTheme getTheme() {
        // TODO Auto-generated method stub
        return null;
    }

}
