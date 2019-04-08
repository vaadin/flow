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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Common stuff for node update mojos.
 */
public abstract class NodeUpdateAbstractMojo extends AbstractMojo {

    /**
     * A class finder using org.reflections.
     */
    public static class ReflectionsClassFinder implements ClassFinder {
        private final transient ClassLoader classLoader;

        private final transient Reflections reflections;

        /**
         * Constructor.
         *
         * @param urls
         *            the list of urls for finding classes.
         */
        public ReflectionsClassFinder(URL... urls) {
            classLoader = new URLClassLoader(urls, null); //NOSONAR
            reflections = new Reflections(
                    new ConfigurationBuilder().addClassLoader(classLoader).setExpandSuperTypes(false)
                            .addUrls(urls));
        }

        @Override
        public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> clazz) {
            Set<Class<?>> classes = new HashSet<>();
            classes.addAll(reflections.getTypesAnnotatedWith(clazz, true));
            classes.addAll(getAnnotatedByRepeatedAnnotation(clazz));
            return classes;

        }

        private Set<Class<?>> getAnnotatedByRepeatedAnnotation(AnnotatedElement annotationClass) {
            Repeatable repeatableAnnotation = annotationClass.getAnnotation(Repeatable.class);
            if (repeatableAnnotation != null) {
                return reflections.getTypesAnnotatedWith(repeatableAnnotation.value(), true);
            }
            return Collections.emptySet();
        }

        @Override
        public URL getResource(String name) {
            return classLoader.getResource(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Class<T> loadClass(String name) throws ClassNotFoundException {
            return (Class<T>)classLoader.loadClass(name);
        }

        @Override
        public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
            return reflections.getSubTypesOf(type);
        }
    }

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    protected boolean convertHtml;

    /**
     * The folder where `package.json` file is located. Default is current dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File npmFolder;

    /**
     * The path to the {@literal node_modules} directory of the project.
     */
    @Parameter(defaultValue = "${project.basedir}/node_modules/")
    protected File nodeModulesPath;

    protected NodeUpdater updater;

    @Override
    public void execute() {
        // Do nothing when bower mode
        if (Boolean.getBoolean("vaadin." + Constants.SERVLET_PARAMETER_BOWER_MODE)) {
            String goal = this.getClass().equals(NodeUpdateImportsMojo.class) ? "update-imports"  : "update-npm-dependencies";
            getLog().info("Skipped '" + goal + "' goal because `vaadin.bowerMode` is set.");
            return;
        }
        getUpdater().execute();
    }

    protected abstract NodeUpdater getUpdater();

    static ClassFinder getClassFinder(MavenProject project) {
        final List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
        URL[] urls = runtimeClasspathElements.stream().map(File::new)
                .map(FlowPluginFileUtils::convertToUrl).toArray(URL[]::new);

        return new ReflectionsClassFinder(urls);
    }
}
