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


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Goal that updates package.json file with @NpmPackage annotations defined in the classpath.
 */
@Mojo(name = "package-json", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PackageJsonMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        URL[] projectClassPathUrls = getProjectClassPathUrls();

        Log log = this.getLog();

        log.info("Looking for npm packages...");

        AnnotationValuesExtractor annotationValuesExtractor = new AnnotationValuesExtractor(
                projectClassPathUrls);

        Map<Class<? extends Annotation>, Set<String>> npmPackagesMap = annotationValuesExtractor
                .extractAnnotationValues(
                        Collections.singletonMap(NpmPackage.class, "value"));
        Map<Class<? extends Annotation>, Set<String>> htmlImportsMap = annotationValuesExtractor
                .extractAnnotationValues(
                        Collections.singletonMap(HtmlImport.class, "value"));

        Set<String> packages = npmPackagesMap.get(NpmPackage.class);
        Set<String> htmlImports = getHtmlImportNpmPackages(
                htmlImportsMap.get(HtmlImport.class));

        packages.addAll(htmlImports);

        log.info("Found " + packages.size() + " npm packages.");
        log.info("Waiting for npm to update package.json...");

        savePackageJson(packages);
    }

    private void savePackageJson(Set<String> dependencies) {

        List<String> command = new ArrayList<>(5 + dependencies.size());
        command.add("npm");
        command.add("install");
        command.add("--save");
        command.add("--package-lock-only");
        command.add("--no-package-lock");
        command.addAll(dependencies);

        ProcessBuilder builder = new ProcessBuilder(command);

        Log log = this.getLog();
        try {

            final Process process = builder.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        } catch (Exception e) {
            log.error(e);
        }

    }

    private Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(this::getHtmlImportNpmPackage)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private String getHtmlImportNpmPackage(String htmlImport) {

        String module = htmlImport
                .replaceFirst("^.*bower_components/(vaadin-[^/]*)/.*\\.html$",
                        "@vaadin/$1")
                .replaceFirst(
                        "^.*bower_components/((iron|paper)-[^/]*)/.*\\.html$",
                        "@polymer/$1");

        return Objects.equals(module, htmlImport) ? null : module;
    }

    private URL[] getProjectClassPathUrls() {
        final List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
        return runtimeClasspathElements.stream().map(File::new)
                .map(FlowPluginFileUtils::convertToUrl).toArray(URL[]::new);
    }

}
