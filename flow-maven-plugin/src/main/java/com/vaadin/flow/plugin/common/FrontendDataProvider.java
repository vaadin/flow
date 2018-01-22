/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.google.common.collect.ImmutableMap;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Provides necessary data for {@link FrontendToolsManager} to process project frontend files.
 *
 * @author Vaadin Ltd.
 */
public class FrontendDataProvider {
    private static final String VALUE_GETTER_METHOD_NAME = "value";

    private final boolean shouldBundle;
    private final Map<String, Set<File>> fragments;
    private final Set<File> shellFileImports;

    /**
     * Creates the data provider.
     *
     * @param shouldBundle whether bundling data should be prepared
     * @param es6SourceDirectory   the directory with original ES6 files, not {@code null}
     * @param annotationValuesExtractor extractor for getting all required values from project to prepare its resources properly, not {@code null}
     * @param fragmentConfigurationFile path to external configuration file with fragments, may be {@code null}
     * @param userDefinedFragments another list of fragments, if user preferred to specify them without external configuration file, not {@code null}
     */
    public FrontendDataProvider(boolean shouldBundle, File es6SourceDirectory, AnnotationValuesExtractor annotationValuesExtractor, File fragmentConfigurationFile, Map<String, Set<String>> userDefinedFragments) {
        this.shouldBundle = shouldBundle;
        fragments = shouldBundle
                ? resolveFragmentFiles(es6SourceDirectory, fragmentConfigurationFile, userDefinedFragments)
                : Collections.emptyMap();
        shellFileImports = resolveShellFileImports(es6SourceDirectory, annotationValuesExtractor, fragments.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
    }

    /**
     * Gets the information whether should the plugin bundle the frontend files or not.
     *
     * @return {@code true} if bundling should be performed, {@code false} otherwise
     */
    public boolean shouldBundle() {
        return shouldBundle;
    }

    /**
     * If bundling is enabled, creates fragment files required for the bundling, if any were configured
     *
     * @param targetDirectory the directory to create the files into
     * @return absolute paths of the files created
     */
    public Set<String> createFragmentFiles(File targetDirectory) {
        return fragments.entrySet().stream()
                .map(entry -> createFragmentFile(targetDirectory, entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    /**
     * Creates a shell file that contains all application imports excluding the ones included into fragments (if any).
     * Used by the {@link FrontendToolsManager} to process the application files
     *
     * @param targetDirectory the directory to create the file into
     * @return an absolute path to the file created
     */
    public String createShellFile(File targetDirectory) {
        Path shellFile = targetDirectory.toPath().resolve("vaadin-flow-bundle.html");
        try {
            Files.write(shellFile, getShellFileImports(targetDirectory), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to create shell file '%s'", shellFile), e);
        }
        return shellFile.toAbsolutePath().toString();
    }


    private Map<String, Set<File>> resolveFragmentFiles(File es6SourceDirectory, File fragmentConfigurationFile, Map<String, Set<String>> userFragments) {
        Map<String, Set<File>> result = new HashMap<>();
        if (fragmentConfigurationFile != null && fragmentConfigurationFile.isFile()) {
            new BundleConfigurationReader(fragmentConfigurationFile).getFragments()
                    .forEach((fragmentName, fragmentPaths) -> result.merge(fragmentName, findInSourceDirectory(es6SourceDirectory, fragmentPaths), this::mergeSets));
        }
        userFragments.forEach((fragmentName, fragmentPaths) -> result.merge(fragmentName, findInSourceDirectory(es6SourceDirectory, fragmentPaths), this::mergeSets));
        return result;
    }

    private Set<File> findInSourceDirectory(File es6SourceDirectory, Set<String> fragmentPaths) {
        return fragmentPaths.stream()
                .map(fragmentPath -> getFileFromSourceDirectory(es6SourceDirectory, fragmentPath))
                .collect(Collectors.toSet());
    }

    private File getFileFromSourceDirectory(File es6SourceDirectory, String fragmentImportPath) {
        File fragmentFile = new File(es6SourceDirectory, fragmentImportPath);
        if (!fragmentFile.isFile()) {
            throw new IllegalArgumentException(String.format(
                    "The fragment file path '%s' was resolved to '%s', which either does not exist or not a file.",
                    fragmentImportPath, fragmentFile));
        }
        return fragmentFile;
    }

    private <T> Set<T> mergeSets(Set<T> one, Set<T> two) {
        Set<T> result = new HashSet<>(one);
        result.addAll(two);
        return result;
    }

    private Set<File> resolveShellFileImports(File es6SourceDirectory, AnnotationValuesExtractor annotationValuesExtractor, Set<File> fragmentFiles) {
        return annotationValuesExtractor.extractAnnotationValues(ImmutableMap.of(
                HtmlImport.class, VALUE_GETTER_METHOD_NAME,
                StyleSheet.class, VALUE_GETTER_METHOD_NAME,
                JavaScript.class, VALUE_GETTER_METHOD_NAME
        )).values().stream()
                .flatMap(Collection::stream)
                .map(this::removeFlowPrefixes)
                .map(annotationImport -> getFileFromSourceDirectory(es6SourceDirectory, annotationImport))
                .filter(fileInSourceDirectory -> !fragmentFiles.contains(fileInSourceDirectory))
                .collect(Collectors.toSet());
    }

    private String removeFlowPrefixes(String url) {
        return url
                .replace(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.BASE_PROTOCOL_PREFIX, "");
    }

    private String createFragmentFile(File targetDirectory, String fragmentName, Set<File> filesFromFragment) {
        List<String> fragmentImports = filesFromFragment.stream()
                .map(fileFromFragment -> relativeToTargetDirectory(targetDirectory, fileFromFragment))
                .map(this::toFrontendImport)
                .collect(Collectors.toList());

        String fragmentFileName = fragmentName.endsWith(".html") ? fragmentName : fragmentName + ".html";
        Path fragmentFile = targetDirectory.toPath().resolve(fragmentFileName);
        try {
            Files.write(fragmentFile, fragmentImports, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to create fragment file '%s'", fragmentFile), e);
        }
        return fragmentFile.toString();
    }

    private List<String> getShellFileImports(File targetDirectory) {
        return shellFileImports.stream()
                .map(fileNotInFragments -> relativeToTargetDirectory(targetDirectory, fileNotInFragments))
                .map(this::toFrontendImport)
                .collect(Collectors.toList());
    }

    private String relativeToTargetDirectory(File targetDirectory, File fragmentFilePath) {
        return targetDirectory.toPath().relativize(fragmentFilePath.toPath()).toString();
    }

    private String toFrontendImport(String importToFormat) {
        String importWithReplacedBackslashes = importToFormat.replace("\\", "/");
        if (importToFormat.endsWith(".js")) {
            return String.format("<script type='text/javascript' src='%s'></script>", importWithReplacedBackslashes);
        }
        if (importToFormat.endsWith(".css")) {
            return String.format("<link rel='stylesheet' href='%s'>", importWithReplacedBackslashes);
        }
        return String.format("<link rel='import' href='%s'>", importWithReplacedBackslashes);
    }
}
