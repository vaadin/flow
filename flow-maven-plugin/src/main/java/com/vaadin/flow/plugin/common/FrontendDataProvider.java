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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.migration.ClassPathIntrospector;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Provides necessary data for {@link FrontendToolsManager} to process project
 * frontend files.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class FrontendDataProvider {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FrontendDataProvider.class);

    private final boolean shouldBundle;
    private final boolean shouldMinify;
    private final boolean shouldHash;

    private final Map<String, Set<File>> fragments;
    private final Set<File> shellFileImports;

    /**
     * Creates the data provider.
     *
     * @param shouldBundle
     *            whether bundling data should be prepared
     * @param shouldMinify
     *            whether the output files should be minified
     * @param shouldHash
     *            whether the output file names should containt a fingerprint
     * @param es6SourceDirectory
     *            the directory with original ES6 files, not {@code null}
     * @param annotationValuesExtractor
     *            extractor for getting all required values from project to
     *            prepare its resources properly, not {@code null}
     * @param fragmentConfigurationFile
     *            path to external configuration file with fragments, may be
     *            {@code null}
     * @param webComponentOutputDirectoryName
     *            folder name inside {@code es6SourceDirectory} where web
     *            component module files will be generated, not {@code null}
     * @param userDefinedFragments
     *            another list of fragments, if user preferred to specify them
     *            without external configuration file, not {@code null}
     */
    public FrontendDataProvider(boolean shouldBundle, boolean shouldMinify,
            boolean shouldHash, File es6SourceDirectory,
            AnnotationValuesExtractor annotationValuesExtractor,
            File fragmentConfigurationFile,
            String webComponentOutputDirectoryName,
            Map<String, Set<String>> userDefinedFragments) {
        this.shouldBundle = shouldBundle;
        this.shouldMinify = shouldMinify;
        this.shouldHash = shouldHash;
        fragments = shouldBundle
                ? resolveFragmentFiles(es6SourceDirectory,
                        fragmentConfigurationFile, userDefinedFragments)
                : Collections.emptyMap();
        shellFileImports = resolveShellFileImports(es6SourceDirectory,
                annotationValuesExtractor, fragments.values().stream()
                        .flatMap(Set::stream).collect(Collectors.toSet()));
        shellFileImports.addAll(generateWebComponentModules(
                new File(es6SourceDirectory, webComponentOutputDirectoryName),
                annotationValuesExtractor));
    }

    /**
     * Gets the information whether should the plugin bundle the frontend files
     * or not.
     *
     * @return {@code true} if bundling should be performed, {@code false}
     *         otherwise
     */
    public boolean shouldBundle() {
        return shouldBundle;
    }

    /**
     * Gets the information whether should the plugin minify the output files or
     * not.
     *
     * @return {@code true} if minification should be performed, {@code false}
     *         otherwise
     */
    public boolean shouldMinify() {
        return shouldMinify;
    }

    /**
     * Gets the information whether should the plugin rename the output files by
     * adding a hash fragment.
     *
     * @return {@code true} if renaming of fragments to include a hash part
     *         should be performed
     */
    public boolean shouldHash() {
        return shouldHash;
    }

    /**
     * If bundling is enabled, creates fragment files required for the bundling,
     * if any were configured.
     *
     * @param targetDirectory
     *            the directory to create the files into
     * @return absolute paths of the files created
     */
    public Set<String> createFragmentFiles(File targetDirectory) {
        return fragments.entrySet().stream()
                .map(entry -> createFragmentFile(targetDirectory,
                        entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    /**
     * Creates a shell file that contains all application imports excluding the
     * ones included into fragments (if any). Used by the
     * {@link FrontendToolsManager} to process the application files
     *
     * @param targetDirectory
     *            the directory to create the file into
     * @return an absolute path to the file created
     */
    public String createShellFile(File targetDirectory) {
        Path shellFile = targetDirectory.toPath()
                .resolve("vaadin-flow-bundle.html");
        try {
            Files.write(shellFile, getShellFileImports(targetDirectory),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String
                    .format("Failed to create shell file '%s'", shellFile), e);
        }
        return shellFile.toAbsolutePath().toString();
    }

    /**
     * Gets the URL translator to rewrite URL using the theme in declared in the
     * application.
     *
     * @param es6SourceDirectory
     *            the directory with original ES6 files, not {@code null}
     * @param introspector
     *            the introspector whose classpath will be used for returned
     *            translator
     * @return the translator which rewrites URLs using the application theme
     */
    protected ThemedURLTranslator getTranslator(File es6SourceDirectory,
            ClassPathIntrospector introspector) {
        return new ThemedURLTranslator(
                url -> new File(es6SourceDirectory, removeFrontendPrefix(url)),
                introspector);
    }

    /**
     * Gets web component module content generator.
     *
     * @param introspector
     *            the introspector whose classpath will be used for returned
     *            generator
     * @return the web component module content generator
     */
    protected WebComponentModulesGenerator getWebComponentGenerator(
            ClassPathIntrospector introspector) {
        return new WebComponentModulesGenerator(introspector);
    }

    private Map<String, Set<File>> resolveFragmentFiles(File es6SourceDirectory,
            File fragmentConfigurationFile,
            Map<String, Set<String>> userFragments) {
        Map<String, Set<File>> result = new HashMap<>();
        if (fragmentConfigurationFile != null
                && fragmentConfigurationFile.isFile()) {
            new BundleConfigurationReader(fragmentConfigurationFile)
                    .getFragments()
                    .forEach((fragmentName, fragmentPaths) -> result.merge(
                            fragmentName,
                            findInSourceDirectory(es6SourceDirectory,
                                    fragmentPaths),
                            this::mergeSets));
        }
        userFragments.forEach((fragmentName, fragmentPaths) -> result.merge(
                fragmentName,
                findInSourceDirectory(es6SourceDirectory, fragmentPaths),
                this::mergeSets));
        return Collections.unmodifiableMap(result);
    }

    private Set<File> findInSourceDirectory(File es6SourceDirectory,
            Set<String> fragmentPaths) {
        return fragmentPaths.stream()
                .map(fragmentPath -> getFileFromSourceDirectory(
                        es6SourceDirectory, fragmentPath))
                .collect(Collectors.toSet());
    }

    private File getFileFromSourceDirectory(File es6SourceDirectory,
            String fragmentImportPath) {
        File fragmentFile = new File(es6SourceDirectory, fragmentImportPath);
        if (!fragmentFile.isFile()) {
            throw new IllegalArgumentException(String.format(
                    "An import that ends with '%s' cannot be resolved: the corresponding file '%s' was not found.%n"
                            + "Double check the corresponding import and verify the following:%n"
                            + "* the import string is correct%n"
                            + "* the file imported is either present in '%s' directory of the project or in one of the project WebJar dependencies or in one of the regular jar dependencies%n"
                            + "* if the file is present in one of the regular jar dependencies, it should be located in `%s` directory in the jar",
                    fragmentImportPath, fragmentFile,
                    ApplicationConstants.FRONTEND_PROTOCOL_PREFIX,
                    Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT));
        }
        return fragmentFile;
    }

    private <T> Set<T> mergeSets(Set<T> one, Set<T> two) {
        Set<T> result = new HashSet<>(one);
        result.addAll(two);
        return result;
    }

    private Set<File> resolveShellFileImports(File es6SourceDirectory,
            AnnotationValuesExtractor annotationValuesExtractor,
            Set<File> fragmentFiles) {
        Map<Class<? extends Annotation>, Set<String>> annotationValues = annotationValuesExtractor
                .extractAnnotationValues(ImmutableMap.of(StyleSheet.class,
                        ThemedURLTranslator.VALUE, JavaScript.class,
                        ThemedURLTranslator.VALUE));

        Collection<Set<String>> htmlImports = annotationValuesExtractor
                .extractAnnotationValues(Collections.singletonMap(
                        HtmlImport.class, ThemedURLTranslator.VALUE))
                .values();
        Set<String> htmlImportUrls = htmlImports.isEmpty()
                ? Collections.emptySet()
                : htmlImports.iterator().next();

        annotationValues.put(HtmlImport.class,
                getTranslator(es6SourceDirectory, annotationValuesExtractor)
                        .applyTheme(htmlImportUrls));

        return annotationValues.values().stream().flatMap(Collection::stream)
                .filter(this::canBeResolvedInFrontendDirectory)
                .map(this::removeFrontendPrefix)
                .map(annotationImport -> getFileFromSourceDirectory(
                        es6SourceDirectory, annotationImport))
                .filter(fileInSourceDirectory -> !fragmentFiles
                        .contains(fileInSourceDirectory))
                .collect(Collectors.toSet());
    }

    private boolean canBeResolvedInFrontendDirectory(String url) {
        boolean canBeResolved = url
                .startsWith(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX)
                || !(url.startsWith("/") || url.contains("://"));
        if (!canBeResolved) {
            LOGGER.debug(
                    "Import '{}' will not be processed by the plugin: only imports with '{}' protocol or relative urls with no protocol are eligible for processing",
                    url, ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        }
        return canBeResolved;
    }

    private String removeFrontendPrefix(String url) {
        return url.replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, "");
    }

    private String createFragmentFile(File targetDirectory, String fragmentName,
            Set<File> filesFromFragment) {
        List<String> fragmentImports = filesFromFragment.stream()
                .map(fileFromFragment -> relativeToTargetDirectory(
                        targetDirectory, fileFromFragment))
                .map(this::toFrontendImport).collect(Collectors.toList());

        String fragmentFileName = fragmentName.endsWith(".html") ? fragmentName
                : fragmentName + ".html";
        Path fragmentFile = targetDirectory.toPath().resolve(fragmentFileName);
        try {
            Files.write(fragmentFile, fragmentImports, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create fragment file '%s'", fragmentFile), e);
        }
        return fragmentFile.toString();
    }

    private List<String> getShellFileImports(File targetDirectory) {
        return shellFileImports.stream()
                .map(fileNotInFragments -> relativeToTargetDirectory(
                        targetDirectory, fileNotInFragments))
                .map(this::toFrontendImport).collect(Collectors.toList());
    }

    private String relativeToTargetDirectory(File targetDirectory,
            File fragmentFilePath) {
        return targetDirectory.toPath().relativize(fragmentFilePath.toPath())
                .toString();
    }

    private String toFrontendImport(String importToFormat) {
        String importWithReplacedBackslashes = importToFormat.replace("\\",
                "/");
        if (importToFormat.endsWith(".js")) {
            return String.format(
                    "<script type='text/javascript' src='%s'></script>",
                    importWithReplacedBackslashes);
        }
        if (importToFormat.endsWith(".css")) {
            return String.format("<link rel='stylesheet' href='%s'>",
                    importWithReplacedBackslashes);
        }
        return String.format("<link rel='import' href='%s'>",
                importWithReplacedBackslashes);
    }

    private Collection<File> generateWebComponentModules(File outputDir,
            AnnotationValuesExtractor annotationValuesExtractor) {
        try {
            FileUtils.forceMkdir(outputDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create output " +
                    "directory for generated web components!", e);
        }

        WebComponentModulesGenerator generator = getWebComponentGenerator(
                annotationValuesExtractor);
        return generator.generateWebComponentModules(outputDir);
    }
}
