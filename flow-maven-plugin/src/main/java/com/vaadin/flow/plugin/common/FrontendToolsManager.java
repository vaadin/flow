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
import java.io.InputStream;
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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.github.eirslett.maven.plugins.frontend.lib.YarnInstaller;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Entity to operate frontend tools to transpile files.
 * <p>
 * Transpilation is based on <a href="https://github.com/gulpjs/gulp">gulp</a>
 * and <a href="https://github.com/Polymer/polymer-build">polymer-build</a>.
 * <p>
 * Polymer tools are used since they are the only ones capable of bundling html files along with css and js ones and providing helper method for processing web components.
 * <p>
 * Apart from gulp and polymer-build, <a href="https://github.com/yarnpkg/yarn">yarn</a> and <a href="https://github.com/nodejs/node">node</a>
 * are used to get and launch the tools required.
 *
 * @author Vaadin Ltd.
 */
public class FrontendToolsManager {
    private static final String VALUE_GETTER_METHOD_NAME = "value";

    private final AnnotationValuesExtractor annotationValuesExtractor;
    private final FrontendPluginFactory factory;
    private final File workingDirectory;
    private final String es5OutputDirectoryName;
    private final String es6OutputDirectoryName;
    private final File bundleConfigurationFile;

    /**
     * Prepares the manager.
     *
     * @param annotationValuesExtractor extractor for getting all required values from project to prepare its resources properly, not {@code null}
     * @param workingDirectory          the directory to install and process files in, not {@code null}
     * @param es5OutputDirectoryName    the name for the directory to put transpiled ES5 files into, not {@code null}
     * @param es6OutputDirectoryName    the name for the directory to put minified ES6 files into, not {@code null}
     * @param bundleConfigurationFile   the path to the file used to configure bundling
     */
    public FrontendToolsManager(AnnotationValuesExtractor annotationValuesExtractor, File workingDirectory,
                                String es5OutputDirectoryName, String es6OutputDirectoryName, File bundleConfigurationFile) {
        this.annotationValuesExtractor = Objects.requireNonNull(annotationValuesExtractor);
        FlowPluginFileUtils.forceMkdir(Objects.requireNonNull(workingDirectory));
        this.factory = new FrontendPluginFactory(workingDirectory, workingDirectory);
        this.workingDirectory = workingDirectory;
        this.es5OutputDirectoryName = Objects.requireNonNull(es5OutputDirectoryName);
        this.es6OutputDirectoryName = Objects.requireNonNull(es6OutputDirectoryName);
        this.bundleConfigurationFile = bundleConfigurationFile;
    }

    /**
     * Installs tools required for transpilation.
     * Tools installed are:
     * <ul>
     * <li><a href="https://github.com/nodejs/node">node</a></li>
     * <li><a href="https://github.com/yarnpkg/yarn">yarn</a></li>
     * <li><a href="https://github.com/gulpjs/gulp">gulp</a></li>
     * </ul>
     * Additionally, {@literal gulpfile.js} file that is used for installation and transpilation via {@literal gulp}.
     *
     * @param proxyConfig proxy config to use when downloading frontend tools, not {@code null}
     * @param nodeVersion node version, not {@code null}
     * @param yarnVersion yarn version, not {@code null}
     * @throws IllegalStateException if dependency installation fails
     * @throws UncheckedIOException  if supplementary file creation fails
     */
    public void installFrontendTools(ProxyConfig proxyConfig, String nodeVersion, String yarnVersion) {
        Objects.requireNonNull(proxyConfig);
        Objects.requireNonNull(nodeVersion);
        Objects.requireNonNull(yarnVersion);

        createFileFromTemplateResource("package.json", Collections.emptyMap());
        createFileFromTemplateResource("yarn.lock", Collections.emptyMap());
        try {
            factory.getNodeInstaller(proxyConfig)
                    .setNodeVersion(nodeVersion)
                    .setNodeDownloadRoot(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
                    .install();
            factory.getYarnInstaller(proxyConfig)
                    .setYarnVersion(yarnVersion)
                    .setYarnDownloadRoot(YarnInstaller.DEFAULT_YARN_DOWNLOAD_ROOT)
                    .install();
            factory.getYarnRunner(proxyConfig, null)
                    .execute("install", Collections.emptyMap());
        } catch (InstallationException | TaskRunnerException e) {
            throw new IllegalStateException("Failed to install required frontend dependencies", e);
        }
    }

    private void createFileFromTemplateResource(String templateResourceName, Map<String, String> replacements) {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(templateResourceName)) {
            if (resource == null) {
                throw new IllegalStateException(String.format("File '%s' is not found in application resources", templateResourceName));
            }

            String resourceContents = IOUtils.toString(resource, StandardCharsets.UTF_8);
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                resourceContents = resourceContents.replaceFirst(Pattern.quote(replacement.getKey()), replacement.getValue().replace("\\", "/"));
            }

            FileUtils.write(new File(workingDirectory, templateResourceName), resourceContents, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to create a file in '%s'", workingDirectory), e);
        }
    }

    /**
     * Transpiles files in the directory provided, copying them to the working directory.
     * Creates a new output directory, in working directory, with two separate directories for ES5 (transpiled) and ES6 (optimized) files.
     * Additionally, creates supplementary files, used by transpilation process.
     *
     * @param es6SourceDirectory   the directory to get ES6 files from, not {@code null}
     * @param outputDirectory      the directory to put transpiled files into, created if absent
     * @param skipEs5              whether to skip the transpilation step or not
     * @param bundle               whether to bundle resulting files or not
     * @param userDefinedFragments the user defined fragments that are defined manually by the users
     * @return generated output directory path
     * @throws IllegalStateException    if transpilation fails
     * @throws IllegalArgumentException if es6SourceDirectory is not a directory or does not exist
     * @throws UncheckedIOException     if output directory creation fails or other {@link IOException} occurs
     */
    public Map<String, File> transpileFiles(File es6SourceDirectory, File outputDirectory, boolean skipEs5, boolean bundle, Map<String, Set<String>> userDefinedFragments) {
        try {
            FileUtils.forceMkdir(Objects.requireNonNull(outputDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to create output directory '%s'", outputDirectory), e);
        }

        if (!Objects.requireNonNull(es6SourceDirectory).isDirectory()) {
            throw new IllegalArgumentException(String.format("es6SourceDirectory '%s' is not a directory or does not exist", es6SourceDirectory));
        }

        Map<String, Set<String>> fragments = userDefinedFragments.isEmpty() ? getFragments() : userDefinedFragments;
        Set<String> fragmentFiles = createFragmentFiles(es6SourceDirectory, fragments);
        String shellFile = createShellFile(es6SourceDirectory, fragments.values());

        ImmutableMap.Builder<String, String> gulpFileParameters = new ImmutableMap.Builder<String, String>()
                .put("{skip_es5}", Boolean.toString(skipEs5))
                .put("{bundle}", Boolean.toString(bundle))
                .put("{es6_source_directory}", es6SourceDirectory.getAbsolutePath())
                .put("{target_directory}", outputDirectory.getAbsolutePath())
                .put("{es5_configuration_name}", es5OutputDirectoryName)
                .put("{es6_configuration_name}", es6OutputDirectoryName)
                .put("{shell_file}", shellFile)
                .put("{fragment_files}", fragmentFiles.stream().map(fileName -> "'" + fileName + "'").collect(Collectors.joining(", ")));
        createFileFromTemplateResource("gulpfile.js", gulpFileParameters.build());

        try {
            factory.getGulpRunner().execute("build", Collections.emptyMap());
        } catch (TaskRunnerException e) {
            throw new IllegalStateException("Transpilation with gulp has failed", e);
        }

        Map<String, File> transpilationResults = new HashMap<>();
        addTranspilationResult(transpilationResults, outputDirectory, es6OutputDirectoryName);
        if (!skipEs5) {
            addTranspilationResult(transpilationResults, outputDirectory, es5OutputDirectoryName);
        }
        return transpilationResults;
    }

    private Map<String, Set<String>> getFragments() {
        if (bundleConfigurationFile != null && bundleConfigurationFile.isFile()) {
            return new BundleConfigurationReader(bundleConfigurationFile).getFragments();
        }
        return Collections.emptyMap();
    }

    private void addTranspilationResult(Map<String, File> transpilationResults, File outputDirectory, String configurationName) {
        File configurationOutput = FileUtils.getFile(outputDirectory, configurationName);
        if (!configurationOutput.isDirectory()) {
            throw new IllegalStateException(String.format("Unable to find transpilation result directory at '%s'", configurationOutput));
        }
        transpilationResults.put(configurationName, configurationOutput);
    }

    private List<String> getShellFileImports(File es6SourceDirectory, Set<String> fragmentImports) {
        return annotationValuesExtractor.extractAnnotationValues(ImmutableMap.of(
                HtmlImport.class, VALUE_GETTER_METHOD_NAME,
                StyleSheet.class, VALUE_GETTER_METHOD_NAME,
                JavaScript.class, VALUE_GETTER_METHOD_NAME
        )).values().stream()
            .flatMap(Collection::stream)
            .map(this::removeFlowPrefixes)
            .filter(annotationImport -> !fragmentImports.contains(annotationImport))
            .map(relativePath -> new File(es6SourceDirectory, relativePath))
            .filter(File::exists)
            .map(this::relativeToWorkingDirectory)
            .map(this::formatImport)
            .collect(Collectors.toList());
    }

    private String relativeToWorkingDirectory(File file) {
        return workingDirectory.toPath().relativize(file.toPath()).toString();
    }

    private String removeFlowPrefixes(String url) {
        return url
                .replace(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.BASE_PROTOCOL_PREFIX, "");
    }

    private String createShellFile(File es6SourceDirectory, Collection<Set<String>> fragments) {
        Set<String> fragmentImports = fragments.stream().flatMap(Set::stream).collect(Collectors.toSet());
        Path shellFile = workingDirectory.toPath().resolve("vaadin-flow-bundle.html");
        try {
            Files.write(shellFile, getShellFileImports(es6SourceDirectory, fragmentImports), StandardCharsets.UTF_8);
            return shellFile.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to create file '%s'", shellFile), e);
        }
    }

    private Set<String> createFragmentFiles(File es6SourceDirectory, Map<String, Set<String>> fragments) {
        Set<String> createdFragmentFiles = new HashSet<>();

        for (Map.Entry<String, Set<String>> fragmentData : fragments.entrySet()) {
            String fragmentName = fragmentData.getKey();
            Set<String> fragmentPath = fragmentData.getValue();
            String fragmentFileName = fragmentName + ".html";
            List<String> fragmentImports = fragmentPath.stream()
                    .map(fragmentImportPath -> getFragmentFile(es6SourceDirectory, fragmentImportPath))
                    .map(this::relativeToWorkingDirectory)
                    .map(this::formatImport)
                    .collect(Collectors.toList());
            try {
                Files.write(workingDirectory.toPath().resolve(fragmentFileName), fragmentImports, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create fragment file.", e);
            }
            createdFragmentFiles.add(fragmentFileName);
        }
        return createdFragmentFiles;
    }

    private String formatImport(String importToFormat) {
        String importWithReplacedBackslashes = importToFormat.replace("\\", "/");
        if (importToFormat.endsWith(".js")) {
            return String.format("<script type='text/javascript' src='%s'></script>", importWithReplacedBackslashes);
        }
        if (importToFormat.endsWith(".css")) {
            return String.format("<link rel='stylesheet' href='%s'>", importWithReplacedBackslashes);
        }
        return String.format("<link rel='import' href='%s'>", importWithReplacedBackslashes);
    }

    private File getFragmentFile(File es6SourceDirectory, String fragmentFilePath) {
        File fragmentFile = new File(es6SourceDirectory, fragmentFilePath);
        if (!fragmentFile.isFile()) {
            throw new IllegalArgumentException(String.format(
                    "The fragment file path '%s' was resolved to '%s', which either does not exist or not a file.",
                    fragmentFilePath, fragmentFile));
        }
        return fragmentFile;
    }
}
