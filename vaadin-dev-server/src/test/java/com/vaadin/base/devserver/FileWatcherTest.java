package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class FileWatcherTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void fileWatcherTriggeredForModification() throws Exception {
        AtomicReference<File> changed = new AtomicReference<>();

        File dir = temporaryFolder.newFolder("watched");
        FileWatcher watcher = new FileWatcher(file -> {
            changed.set(file);
        }, dir);

        watcher.start();

        try {
            File newFile = new File(dir, "newFile.txt");
            newFile.createNewFile();

            Thread.sleep(50); // The watcher is supposed to be triggered
                              // immediately
            Assert.assertEquals(newFile, changed.get());
        } finally {
            watcher.stop();
        }
    }

    @Test
    public void externalDependencyWatcher_setViaParameter_TriggeredForModification()
            throws Exception {
        File projectFolder = temporaryFolder.newFolder("projectFolder");

        String metaInf = "/src/main/resources/META-INF/";
        String rootProjectResourceFrontend = projectFolder.getAbsolutePath()
                + metaInf + "resources/frontend";
        String subProjectLegacyFrontend = projectFolder.getAbsolutePath()
                + "/fakeproject" + metaInf + "frontend";

        new File(rootProjectResourceFrontend).mkdirs();
        new File(subProjectLegacyFrontend).mkdirs();

        File jarFrontendResources = temporaryFolder
                .newFolder("jarFrontendResources");

        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getStringProperty(
                InitParameters.FRONTEND_HOTDEPLOY_DEPENDENCIES, null))
                .thenReturn("./,./fakeproject");
        Mockito.when(config.getProjectFolder()).thenReturn(projectFolder);

        try (MockedStatic<ApplicationConfiguration> appConfig = Mockito
                .mockStatic(ApplicationConfiguration.class)) {
            appConfig.when(() -> ApplicationConfiguration.get(Mockito.any()))
                    .thenReturn(config);
            try (var watcher = new ExternalDependencyWatcher(vaadinContext,
                    jarFrontendResources)) {

                assertFileCountFound(jarFrontendResources, 0);

                createFile(rootProjectResourceFrontend + "/somestyles.css");
                assertFileCountFound(jarFrontendResources, 1);

                createFile(subProjectLegacyFrontend + "/somejs.js");
                assertFileCountFound(jarFrontendResources, 2);

                // Map files as listFiles makes no promises on ordering
                List<String> frontendFiles = Arrays
                        .stream(jarFrontendResources.listFiles())
                        .map(File::getName).toList();
                Assert.assertTrue("No 'somestyles.css' file found",
                        frontendFiles.contains("somestyles.css"));
                Assert.assertTrue("No 'somejs.js' file found",
                        frontendFiles.contains("somejs.js"));
            }
        }
    }

    @Test
    public void externalDependencyWatcher_setAsDefaultForRunnerProjectButNotSubProject_TriggeredForModification()
            throws Exception {
        File projectFolder = temporaryFolder.newFolder("projectFolder");

        String metaInf = "/src/main/resources/META-INF/";
        String rootPorjectResourceFrontend = projectFolder.getAbsolutePath()
                + metaInf + "resources/frontend";
        String subProjectLegacyFrontend = projectFolder.getAbsolutePath()
                + "/fakeproject" + metaInf + "frontend";

        new File(rootPorjectResourceFrontend).mkdirs();
        new File(subProjectLegacyFrontend).mkdirs();

        File jarFrontendResources = temporaryFolder
                .newFolder("jarFrontendResources");

        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getStringProperty(
                InitParameters.FRONTEND_HOTDEPLOY_DEPENDENCIES, null))
                .thenReturn(null);
        Mockito.when(config.getProjectFolder()).thenReturn(projectFolder);

        try (MockedStatic<ApplicationConfiguration> appConfig = Mockito
                .mockStatic(ApplicationConfiguration.class)) {
            appConfig.when(() -> ApplicationConfiguration.get(Mockito.any()))
                    .thenReturn(config);
            try (var watcher = new ExternalDependencyWatcher(vaadinContext,
                    jarFrontendResources)) {

                assertFileCountFound(jarFrontendResources, 0);

                createFile(rootPorjectResourceFrontend + "/somestyles.css");
                assertFileCountFound(jarFrontendResources, 1);

                createFile(subProjectLegacyFrontend + "/somejs.js");
                assertFileCountFound(jarFrontendResources, 1);

                Assert.assertEquals("somestyles.css",
                        jarFrontendResources.listFiles()[0].getName());
            }
        }
    }

    private void assertFileCountFound(File directory, int count) {
        Awaitility.await().untilAsserted(directory::listFiles, files -> {
            Assert.assertEquals(
                    "Wrong amount of copied files found when there should be "
                            + count + ". Current files were: "
                            + Arrays.toString(files),
                    count, files.length);
        });

    }

    private void createFile(String path) throws IOException {
        File newFile = new File(path);
        Files.writeString(newFile.toPath(), "some text");
    }
}
