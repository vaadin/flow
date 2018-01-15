package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.plugin.common.FrontendToolsManager;

/**
 * Transpiles artifacts in the specified directory.
 * <p>
 * Note: this class is intended to be independent from Maven dependencies so that it can be reused in Gradle plugin in future.
 *
 * @author Vaadin Ltd.
 */
public class TranspilationStep {
    private final FrontendToolsManager frontendToolsManager;

    /**
     * Prepares the step.
     *
     * @param frontendToolsManager the manager to be used to transpile files, not {@code null}
     */
    public TranspilationStep(FrontendToolsManager frontendToolsManager) {
        this.frontendToolsManager = Objects.requireNonNull(frontendToolsManager);
    }

    /**
     * Transpiles the files from source directory into the output directory.
     *
     * @param es6SourceDirectory   the directory with original ES6 files
     * @param outputDirectory      the directory that will have processed files in
     * @param skipEs5              whether to skip the transpilation step or not
     * @param bundle               whether to bundle resulting files or not
     * @param userDefinedFragments the user defined fragments that are defined manually by the users
     * @throws IllegalStateException if no transpilation results found
     * @throws UncheckedIOException  if {@link IOException} occurs during file operations
     */
    public void transpileFiles(File es6SourceDirectory, File outputDirectory, boolean skipEs5, boolean bundle, Map<String, Set<String>> userDefinedFragments) {
        Map<String, File> transpilationResult = frontendToolsManager.transpileFiles(es6SourceDirectory, outputDirectory, skipEs5, bundle, userDefinedFragments);
        if (transpilationResult.isEmpty()) {
            throw new IllegalStateException("Received no transpilation results from frontend tools");
        }
        transpilationResult.values().stream()
                .filter(configurationOutput -> !configurationOutput.isDirectory())
                .findAny()
                .ifPresent(nonExistingDirectory -> {
                    throw new IllegalStateException(String.format("Transpilation output at '%s' is not a directory or does not exist", nonExistingDirectory));
                });
    }
}
