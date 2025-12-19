package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

public class BundleBuildUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void packageLockExists_nothingIsCopied() throws IOException {
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Mockito.anyString())).thenReturn(null);
        Options options = new Options(Mockito.mock(Lookup.class), finder,
                temporaryFolder.getRoot()).withBuildDirectory("target");

        File packageLockFile = temporaryFolder
                .newFile(Constants.PACKAGE_LOCK_JSON);
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);

        final String existingLockFile = "{ \"existing\" }";
        FileUtils.write(packageLockFile, existingLockFile);

        FileUtils.write(devPackageLockJson, "{ \"bundleFile\"}");

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        Assert.assertEquals("Existing file should not be overwritten",
                existingLockFile, packageLockContents);
    }

    @Test
    public void noPackageLockExists_devBundleLockIsCopied_notJarLock()
            throws IOException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);

        final String packageLockContent = "{ \"bundleFile\"}";
        FileUtils.write(devPackageLockJson, packageLockContent);

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("dev-bundle file should be used",
                packageLockContent, packageLockContents);
    }

    @Test
    public void noPackageLockExists_jarDevBundleLockIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        File jarHybridPackageLock = new File(options.getNpmFolder(),
                "hybrid-temp.json");
        final String jarHybridPackageLockContent = "{ \"hybridJarData\"}";
        FileUtils.write(jarHybridPackageLock, jarHybridPackageLockContent);

        Mockito.doThrow(new ClassNotFoundException("No Hilla"))
                .when(options.getClassFinder())
                .loadClass("com.vaadin.hilla.EndpointController");
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarHybridPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("File should be gotten from jar on classpath",
                jarPackageLockContent, packageLockContents);
    }

    @Test
    public void noPackageLockExists_hillaUsed_jarHybridDevBundleLockIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        Path dummyView = options.getFrontendDirectory().toPath()
                .resolve(Path.of("views", "dummy.tsx"));
        Files.createDirectories(dummyView.getParent());
        Files.writeString(dummyView, "const x = 1;");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        File jarHybridPackageLock = new File(options.getNpmFolder(),
                "hybrid-temp.json");
        final String jarHybridPackageLockContent = "{ \"hybridJarData\"}";
        FileUtils.write(jarHybridPackageLock, jarHybridPackageLockContent);

        Mockito.when(options.getClassFinder()
                .loadClass("com.vaadin.hilla.EndpointController"))
                .thenReturn(Object.class);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarHybridPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("File should be gotten from jar on classpath",
                jarHybridPackageLockContent, packageLockContents);
    }

    @Test
    public void noPackageLockExists_hillaUsed_hybridPackageLockNotPresentInJar_jarDevBundleIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        Path dummyView = options.getFrontendDirectory().toPath()
                .resolve(Path.of("views", "dummy.tsx"));
        Files.createDirectories(dummyView.getParent());
        Files.writeString(dummyView, "const x = 1;");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .loadClass("com.vaadin.hilla.EndpointController"))
                .thenReturn(Object.class);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(null);

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("File should be gotten from jar on classpath",
                jarPackageLockContent, packageLockContents);
    }

    @Test
    public void pnpm_noPackageLockExists_devBundleLockYamlIsCopied_notJarLockOrJson()
            throws IOException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target").withEnablePnpm(true);

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_YAML))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);
        File devPackageLock = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_YAML);

        final String packageLockContent = "{ \"bundleFile\"}";
        FileUtils.write(devPackageLock, packageLockContent);
        FileUtils.write(devPackageLockJson, "{ \"json\"}");

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_YAML),
                StandardCharsets.UTF_8);

        Assert.assertEquals("dev-bundle file should be used",
                packageLockContent, packageLockContents);
    }

    @Test
    public void pnpm_packageLockExists_nothingIsCopied() throws IOException {
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Mockito.anyString())).thenReturn(null);
        Options options = new Options(Mockito.mock(Lookup.class), finder,
                temporaryFolder.getRoot()).withBuildDirectory("target")
                .withEnablePnpm(true);

        File packageLockFile = temporaryFolder
                .newFile(Constants.PACKAGE_LOCK_YAML);
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_YAML);

        final String existingLockFile = "{ \"existing\" }";
        FileUtils.write(packageLockFile, existingLockFile);

        FileUtils.write(devPackageLockJson, "{ \"bundleFile\"}");

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        Assert.assertEquals("Existing file should not be overwritten",
                existingLockFile, packageLockContents);
    }

}
