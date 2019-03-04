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
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal that updates package.json file with @NpmPackage annotations defined in
 * the classpath.
 */
@Mojo(name = "update-npm-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class UpdateNpmDependenciesMojo extends AbstractMojo {

    public static final String PACKAGE_JSON = "package.json";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        URL[] projectClassPathUrls = getProjectClassPathUrls(project);

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

        boolean packageFileOK = true;

        if (!FileUtils.fileExists(PACKAGE_JSON)) {

            packages.add("@webcomponents/webcomponentsjs");

            log.info("Creating package.json...");

            try {
                FileUtils.fileWrite(PACKAGE_JSON, "{}");
            } catch (IOException e) {
                log.error(e);
            }

            if (!FileUtils.fileExists(PACKAGE_JSON)) {
                log.error("Failed to create package.json file.");
                packageFileOK = false;
            }
        }

        if (packageFileOK) {
            savePackageJson(packages);

        } else {
            log.error("Failing to write packages into package.json.");
        }
    }

    private void savePackageJson(Set<String> dependencies) {
        if (dependencies.isEmpty()) {
            return;

        } else {
            this.getLog().info("Waiting for npm to update package.json...");
        }

        List<String> command = new ArrayList<>(5 + dependencies.size());
        command.add("npm");
        command.add("install");
        command.add("--save");
        command.add("--package-lock-only");
        command.add("--no-package-lock");
        command.addAll(dependencies);

        ProcessBuilder builder = new ProcessBuilder(command);

        try {
            logProcessOutput(builder.start());
        } catch (IOException e) {
            this.getLog().error(e);
        }
    }

    private void logProcessOutput(Process process) {
        Log log = this.getLog();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8));) {

            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }

        } catch (IOException e) {
            log.error(e);
        }
    }

    static Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(UpdateNpmDependenciesMojo::getHtmlImportNpmPackage)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    static String getHtmlImportNpmPackage(String htmlImport) {
        String module = htmlImport
                .replaceFirst("^.*bower_components/(vaadin-[^/]*)/.*\\.html$", "@vaadin/$1")
                .replaceFirst("^.*bower_components/((iron|paper)-[^/]*)/.*\\.html$", "@polymer/$1")
                .replaceFirst("^frontend://(.*)$", "./$1")
                .replaceFirst("\\.html$", ".js");
        return Objects.equals(module, htmlImport) ? null : module;
    }

    static URL[] getProjectClassPathUrls(MavenProject project) {
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
