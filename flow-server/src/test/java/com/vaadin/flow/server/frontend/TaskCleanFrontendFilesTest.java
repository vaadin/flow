package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;

import static com.vaadin.flow.server.Constants.TARGET;

public class TaskCleanFrontendFilesTest {
    @Rule
    public final TemporaryFolder rootFolder = new TemporaryFolder();

    private File projectRoot;

    @Before
    public void init() {
        projectRoot = rootFolder.getRoot();
    }

    @Test
    public void createdFileAreRemoved()
            throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(projectRoot);

        final Set<String> generatedFiles = Stream
                .of(FrontendUtils.VITE_CONFIG,
                        FrontendUtils.VITE_GENERATED_CONFIG, "node_modules",
                        Constants.PACKAGE_JSON, Constants.PACKAGE_LOCK_JSON,
                        TaskGenerateTsConfig.TSCONFIG_JSON,
                        TaskGenerateTsDefinitions.TS_DEFINITIONS, ".npmrc")
                .collect(Collectors.toSet());
        createFiles(generatedFiles);

        clean.execute();

        assertFilesNotExist(generatedFiles);
    }

    @Test
    public void existingFrontendFiles_onlyCreatedFileAreRemoved()
            throws IOException, ExecutionFailedException {
        final Set<String> existingfiles = Stream
                .of(FrontendUtils.VITE_CONFIG, Constants.PACKAGE_JSON,
                        "node_modules", Constants.PACKAGE_LOCK_JSON)
                .collect(Collectors.toSet());
        createFiles(existingfiles);

        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(projectRoot);

        final Set<String> generatedFiles = Stream
                .of(FrontendUtils.VITE_GENERATED_CONFIG,
                        TaskGenerateTsConfig.TSCONFIG_JSON,
                        TaskGenerateTsDefinitions.TS_DEFINITIONS, ".npmrc")
                .collect(Collectors.toSet());
        createFiles(generatedFiles);

        clean.execute();

        assertFilesNotExist(generatedFiles);
        assertFilesExist(existingfiles);
    }

    @Test
    public void nodeModulesFolderIsCleared()
            throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(projectRoot);

        final File nodeModules = rootFolder.newFolder("node_modules");
        new File(nodeModules, "file").createNewFile();
        final File directory = new File(nodeModules, "directory");
        directory.mkdir();
        new File(directory, "file.fi").createNewFile();

        clean.execute();

        assertFilesNotExist(Collections.singleton("node_modules"));
    }

    @Test
    public void packageJsonExists_nodeModulesFolderIsKept()
            throws IOException, ExecutionFailedException {
        createFiles(Collections.singleton(Constants.PACKAGE_JSON));
        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(projectRoot);

        final File nodeModules = rootFolder.newFolder("node_modules");
        new File(nodeModules, "file").createNewFile();
        final File directory = new File(nodeModules, "directory");
        directory.mkdir();
        new File(directory, "file.fi").createNewFile();

        clean.execute();

        assertFilesExist(Collections.singleton("node_modules"));
    }

    @Test
    public void hillaIsUsed_nodeModulesFolderIsKept()
            throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean;
        try (MockedStatic<EndpointRequestUtil> util = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            util.when(() -> EndpointRequestUtil.isHillaAvailable())
                    .thenReturn(true);
            clean = new TaskCleanFrontendFiles(projectRoot);
        }

        final File nodeModules = rootFolder.newFolder("node_modules");
        new File(nodeModules, "file").createNewFile();
        final File directory = new File(nodeModules, "directory");
        directory.mkdir();
        new File(directory, "file.fi").createNewFile();

        clean.execute();

        assertFilesExist(Collections.singleton("node_modules"));
    }

    private void createFiles(Set<String> filesToCreate) throws IOException {
        for (String file : filesToCreate) {
            rootFolder.newFile(file);
        }
    }

    private void assertFilesNotExist(Set<String> files) {
        Set<String> existingFiles = new HashSet<>();
        for (String file : files) {
            if (new File(projectRoot, file).exists()) {
                existingFiles.add(file);
            }
        }

        if (!existingFiles.isEmpty()) {
            StringBuilder fileList = new StringBuilder();
            existingFiles.forEach(file -> fileList.append(file).append("\n"));
            Assert.fail(String.format(
                    "Found files that should have been removed: %s\n",
                    fileList));
        }
    }

    private void assertFilesExist(Set<String> files) {
        Set<String> existingFiles = new HashSet<>(files);
        for (String file : files) {
            if (new File(projectRoot, file).exists()) {
                existingFiles.remove(file);
            }
        }

        if (!existingFiles.isEmpty()) {
            StringBuilder fileList = new StringBuilder();
            existingFiles.forEach(file -> fileList.append(file).append("\n"));
            Assert.fail(String.format("Missing files that should exist: %s\n",
                    fileList));
        }
    }
}