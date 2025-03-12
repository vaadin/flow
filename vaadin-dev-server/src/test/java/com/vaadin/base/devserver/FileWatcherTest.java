package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class FileWatcherTest {

    @Test
    public void fileWatcherTriggeredForModification() throws Exception {
        AtomicReference<File> changed = new AtomicReference<>();

        File dir = Files.createTempDirectory("watched").toFile();
        FileWatcher watcher = new FileWatcher(file -> {
            changed.set(file);
        }, dir);

        watcher.start();

        File newFile = new File(dir, "newFile.txt");
        newFile.createNewFile();

        Thread.sleep(50); // The watcher is supposed to be triggered immediately
        Assert.assertEquals(newFile, changed.get());
    }

    @Test
    public void externalDependencyWatcher_setViaParameter_TriggeredForModification()
            throws Exception {
        File projectFolder = Files.createTempDirectory("projectFolder")
                .toFile();
        projectFolder.deleteOnExit();

        String metaInf = "/src/main/resources/META-INF/";
        String rootPorjectResourceFrontend = projectFolder.getAbsolutePath()
                + metaInf + "resources/frontend";
        String subProjectLegacyFrontend = projectFolder.getAbsolutePath()
                + "/fakeProject" + metaInf + "frontend";

        new File(rootPorjectResourceFrontend).mkdirs();
        new File(subProjectLegacyFrontend).mkdirs();

        File jarFrontendResources = Files
                .createTempDirectory("jarFrontendResources").toFile();
        jarFrontendResources.deleteOnExit();

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
            new ExternalDependencyWatcher(vaadinContext, jarFrontendResources);

            assertFileCountFound(jarFrontendResources, 0);

            createFile(rootPorjectResourceFrontend + "/somestyles.css");
            assertFileCountFound(jarFrontendResources, 1);

            createFile(subProjectLegacyFrontend + "/somejs.js");
            assertFileCountFound(jarFrontendResources, 2);

            Assert.assertEquals("somestyles.css",
                    jarFrontendResources.listFiles()[0].getName());
            Assert.assertEquals("somejs.js",
                    jarFrontendResources.listFiles()[1].getName());
        }
    }

    @Test
    public void externalDependencyWatcher_setAsDefaultForRunnerProjectButNotSubProject_TriggeredForModification()
            throws Exception {
        File projectFolder = Files.createTempDirectory("projectFolder")
                .toFile();
        projectFolder.deleteOnExit();

        String metaInf = "/src/main/resources/META-INF/";
        String rootPorjectResourceFrontend = projectFolder.getAbsolutePath()
                + metaInf + "resources/frontend";
        String subProjectLegacyFrontend = projectFolder.getAbsolutePath()
                + "/fakeProject" + metaInf + "frontend";

        new File(rootPorjectResourceFrontend).mkdirs();
        new File(subProjectLegacyFrontend).mkdirs();

        File jarFrontendResources = Files
                .createTempDirectory("jarFrontendResources").toFile();
        jarFrontendResources.deleteOnExit();

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
            new ExternalDependencyWatcher(vaadinContext, jarFrontendResources);

            assertFileCountFound(jarFrontendResources, 0);

            createFile(rootPorjectResourceFrontend + "/somestyles.css");
            assertFileCountFound(jarFrontendResources, 1);

            createFile(subProjectLegacyFrontend + "/somejs.js");
            assertFileCountFound(jarFrontendResources, 1);

            Assert.assertEquals("somestyles.css",
                    jarFrontendResources.listFiles()[0].getName());
        }
    }

    private void assertFileCountFound(File directory, int count)
            throws InterruptedException {
        Thread.sleep(50);
        Assert.assertEquals(
                "Wroug amount of copied files found when there should be "
                        + count + ".",
                count, directory.listFiles().length);

    }

    private void createFile(String path) throws IOException {
        File newFile = new File(path);
        newFile.createNewFile();
    }
}
