/*
 * Copyright (C) 2000-2025 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class TaskGeneratePWAIconsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final TestPwaConfiguration pwaConfiguration = new TestPwaConfiguration();
    private TaskGeneratePWAIcons task;
    private Path resourcesDirectory;
    private Path iconsOutDirectory;

    @Before
    public void setUp() throws Exception {
        // creating non-existing folder to make sure the execute() creates
        // the folder if missing
        File projectDirectory = temporaryFolder.newFolder("my-project");
        resourcesDirectory = temporaryFolder
                .newFolder("my-project", "out", "classes").toPath();
        Files.createDirectories(resourcesDirectory);
        Path resourceOutDirectory = projectDirectory.toPath()
                .resolve(Path.of("out", "VAADIN"));
        Path wabappResourceOutDirectory = resourceOutDirectory
                .resolve("wabapp");
        iconsOutDirectory = wabappResourceOutDirectory
                .resolve(Constants.VAADIN_PWA_ICONS);

        FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(scanner.getPwaConfiguration()).then(i -> pwaConfiguration);

        URLClassLoader classFinderClassLoader = new URLClassLoader(
                new URL[] { resourcesDirectory.toUri().toURL() }, null);
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                classFinderClassLoader);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(lookup.lookup(ClassFinder.class)).thenReturn(classFinder);
        Options options = new Options(lookup, projectDirectory,
                resourceOutDirectory.getParent().toFile().getAbsolutePath())
                .withWebpack(wabappResourceOutDirectory.toFile(),
                        resourceOutDirectory.toFile());
        task = new TaskGeneratePWAIcons(options, pwaConfiguration);
    }

    @Test
    public void execute_PWA_disabled_iconsNotGenerated()
            throws ExecutionFailedException {
        pwaConfiguration.enabled = false;
        task.execute();
        Assert.assertFalse("PWA icons should not have been generated",
                Files.exists(iconsOutDirectory));
    }

    @Test
    public void execute_PWA_iconInClassPath_generateIcons()
            throws ExecutionFailedException, IOException {
        createBaseIcon(resourcesDirectory);
        task.execute();
        assertIconsGenerated();
    }

    @Test
    public void execute_PWA_iconInMetaInfResourcesFolder_generateIcons()
            throws ExecutionFailedException, IOException {
        createBaseIcon(
                resourcesDirectory.resolve(Path.of("META-INF", "resources")));
        task.execute();
        assertIconsGenerated();
    }

    @Test
    public void execute_PWA_baseIconNotFound_generateIconsFromDefaultLogo()
            throws ExecutionFailedException, IOException {
        task.execute();
        assertIconsGenerated();
    }

    @Test
    public void execute_PWA_invalidBaseIconNotFound_throws()
            throws IOException {
        createBaseIcon(
                resourcesDirectory.resolve(Path.of("META-INF", "resources")),
                new ByteArrayInputStream("NOT AN IMAGE".getBytes()));
        ExecutionFailedException exception = Assert
                .assertThrows(ExecutionFailedException.class, task::execute);
        Assert.assertTrue(
                exception.getMessage().contains("Cannot load PWA icon"));
        Assert.assertFalse("PWA icons should not have been generated",
                Files.exists(iconsOutDirectory));
    }

    private void createBaseIcon(Path resourcesFolder) throws IOException {
        createBaseIcon(resourcesFolder, getClass()
                .getResourceAsStream("/META-INF/resources/icons/icon.png"));
    }

    private void createBaseIcon(Path resourcesFolder, InputStream data)
            throws IOException {
        Path baseIcon = resourcesFolder.resolve(resourcesFolder)
                .resolve(pwaConfiguration.getIconPath().replace('/',
                        File.separatorChar));
        Files.createDirectories(baseIcon.getParent());
        Files.copy(data, baseIcon);
    }

    private void assertIconsGenerated() throws IOException {
        String iconPath = pwaConfiguration.getIconPath();
        Path generatedIconsPath = iconsOutDirectory
                .resolve(iconPath.replace('/', File.separatorChar)).getParent();
        Assert.assertTrue("PWA icons folder should have been generated",
                Files.exists(generatedIconsPath));
        String iconName = iconPath.substring(iconPath.lastIndexOf("/") + 1,
                iconPath.lastIndexOf("."));
        String iconExt = iconPath.substring(iconPath.lastIndexOf(".") + 1);
        Predicate<String> iconNamePattern = Pattern
                .compile(iconName + "-\\d+x\\d+\\." + iconExt).asPredicate();
        List<String> generatedIcons = Files.list(generatedIconsPath)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        Assert.assertFalse("Expected PWA icons to be generated",
                generatedIcons.isEmpty());
        List<String> invalidIcons = generatedIcons.stream()
                .filter(iconNamePattern.negate()).collect(Collectors.toList());
        Assert.assertTrue("Generated icons have invalid names: " + invalidIcons,
                invalidIcons.isEmpty());

    }

    private static class TestPwaConfiguration extends PwaConfiguration {
        private Boolean enabled;

        public TestPwaConfiguration() {
            super(true, DEFAULT_NAME, "Flow PWA", "", DEFAULT_BACKGROUND_COLOR,
                    DEFAULT_THEME_COLOR, "custom/icons/logo.png", DEFAULT_PATH,
                    DEFAULT_OFFLINE_PATH, DEFAULT_DISPLAY, DEFAULT_START_URL,
                    new String[] {}, false);
        }

        @Override
        public boolean isEnabled() {
            return enabled != null ? enabled : super.isEnabled();
        }

    }
}
