/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathList;
import io.quarkus.paths.PathTree;
import io.quarkus.runtime.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.Platform;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuarkusPluginAdapterTest {

    // A real filesystem root, so the fixtures below are absolute on Windows
    // too, where a path like /project has no drive letter and is therefore
    // relative. The directories need not exist, buildFolder() never touches
    // the filesystem.
    private static final Path ROOT = Paths.get("").toAbsolutePath().getRoot();

    private VaadinBuildTimeConfig config;
    private ApplicationModel model;
    private WorkspaceModule appModule;
    private final List<ResolvedDependency> runtimeDependencies = new ArrayList<>();

    @BeforeEach
    void setUp() {
        config = mock(VaadinBuildTimeConfig.class);
        appModule = mock(WorkspaceModule.class);
        when(appModule.getModuleDir())
                .thenReturn(ROOT.resolve("project").toFile());
        when(appModule.getBuildDir()).thenReturn(
                ROOT.resolve(Paths.get("project", "target")).toFile());
        when(appModule.hasMainSources()).thenReturn(false);

        model = mock(ApplicationModel.class);
        when(model.getRuntimeDependencies()).thenReturn(runtimeDependencies);
    }

    private QuarkusPluginAdapter createAdapter() {
        return new QuarkusPluginAdapter(config, model, appModule);
    }

    private void addRuntimeDependency(String groupId, String artifactId) {
        ResolvedDependency dependency = mock(ResolvedDependency.class);
        when(dependency.getGroupId()).thenReturn(groupId);
        when(dependency.getArtifactId()).thenReturn(artifactId);
        runtimeDependencies.add(dependency);
    }

    @Test
    void checkRuntimeDependency_dependencyPresent_noMessage() {
        addRuntimeDependency("com.vaadin", "license-checker");
        List<String> messages = new ArrayList<>();

        assertTrue(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", messages::add));
        assertTrue(messages.isEmpty(),
                "No message expected for a present dependency");
    }

    @Test
    void checkRuntimeDependency_onlyGroupIdMatches_reportsMissing() {
        // com.vaadin is always a runtime dependency of a Vaadin application,
        // so matching on the group id alone would never report the artifact as
        // missing.
        addRuntimeDependency("com.vaadin", "vaadin-quarkus");
        List<String> messages = new ArrayList<>();

        assertFalse(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", messages::add));
        assertEquals(1, messages.size(),
                "Missing dependency message expected: " + messages);
        assertTrue(
                messages.get(0).contains("com.vaadin:license-checker")
                        && messages.get(0).contains("<scope>runtime</scope>"),
                "Unexpected message: " + messages.get(0));
    }

    @Test
    void checkRuntimeDependency_noDependencies_reportsMissing() {
        List<String> messages = new ArrayList<>();

        assertFalse(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", messages::add));
        assertEquals(1, messages.size());
    }

    @Test
    void checkRuntimeDependency_nullMessageConsumer_doesNotFail() {
        assertFalse(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", null));
    }

    @Test
    void checkRuntimeDependency_nullGroupIdOrArtifactId_throws() {
        QuarkusPluginAdapter adapter = createAdapter();
        assertThrows(NullPointerException.class, () -> adapter
                .checkRuntimeDependency(null, "license-checker", null));
        assertThrows(NullPointerException.class,
                () -> adapter.checkRuntimeDependency("com.vaadin", null, null));
    }

    @Test
    void applicationIdentifier_notConfigured_hashesGroupIdColonArtifactId() {
        when(config.applicationIdentifier()).thenReturn(Optional.empty());
        ResolvedDependency appArtifact = mock(ResolvedDependency.class);
        when(appArtifact.getGroupId()).thenReturn("com.example");
        when(appArtifact.getArtifactId()).thenReturn("my-app");
        when(model.getAppArtifact()).thenReturn(appArtifact);

        assertEquals(
                "app-" + StringUtil.getHash("com.example:my-app",
                        StandardCharsets.UTF_8),
                createAdapter().applicationIdentifier());
    }

    @Test
    void applicationIdentifier_configured_returnsConfiguredValue() {
        when(config.applicationIdentifier())
                .thenReturn(Optional.of("my-identifier"));

        assertEquals("my-identifier", createAdapter().applicationIdentifier());
    }

    @Test
    void buildFolder_insideProjectDirectory_relativeToProject() {
        when(appModule.getBuildDir()).thenReturn(
                ROOT.resolve(Paths.get("project", "target")).toFile());

        assertEquals("target", createAdapter().buildFolder());
    }

    @Test
    void buildFolder_outsideProjectDirectory_parentRelativePath() {
        when(appModule.getBuildDir()).thenReturn(
                ROOT.resolve(Paths.get("builds", "target")).toFile());

        // Must be relative, since consumers resolve it against the project
        // folder with new File(npmFolder(), buildFolder())
        assertEquals(
                ".." + File.separator + "builds" + File.separator + "target",
                createAdapter().buildFolder());
    }

    @Test
    void buildFolder_relativeBuildDirectory_returnedUnchanged() {
        when(appModule.getBuildDir()).thenReturn(new File("build/classes"));

        assertEquals("build" + File.separator + "classes",
                createAdapter().buildFolder());
    }

    @Test
    void minimumFrontendPackageAgeDays_configured_returnsValue() {
        when(config.minimumFrontendPackageAgeDays()).thenReturn(Optional.of(7));

        assertEquals(7, createAdapter().minimumFrontendPackageAgeDays());
    }

    @Test
    void minimumFrontendPackageAgeDays_notConfigured_returnsNull() {
        when(config.minimumFrontendPackageAgeDays())
                .thenReturn(Optional.empty());

        // null lets the frontend tools keep their own minimum release age
        assertNull(createAdapter().minimumFrontendPackageAgeDays());
    }

    /**
     * Stubs every directory setting with a relative default, so a test only has
     * to say what it is about. Without this the mock answers null and the
     * resolution below dereferences it.
     */
    private void configureDirectoryDefaults() {
        when(config.frontendResourcesDirectory()).thenReturn(
                new File("src/main/resources/META-INF/resources/frontend"));
        when(config.frontendDirectory())
                .thenReturn(new File("src/main/frontend"));
        when(config.resourcesOutputDirectory())
                .thenReturn(new File("resources"));
        when(config.generatedResourceOutputDirectory())
                .thenReturn(new File("generated-resources"));
        when(config.frontendOutputDirectory()).thenReturn(new File("frontend"));
        when(config.webpackOutputDirectory()).thenReturn(Optional.empty());
        when(config.generatedTsFolder()).thenReturn(Optional.empty());
        when(config.nodeFolder()).thenReturn(Optional.empty());
        when(config.npmFolder()).thenReturn(Optional.empty());
    }

    @Test
    void booleanSettings_areTakenFromTheConfiguration() {
        when(config.generateBundle()).thenReturn(true);
        when(config.generateEmbeddableWebComponents()).thenReturn(true);
        when(config.optimizeBundle()).thenReturn(true);
        when(config.runNpmInstall()).thenReturn(true);
        when(config.ciBuild()).thenReturn(true);
        when(config.forceProductionBuild()).thenReturn(true);
        when(config.eagerServerLoad()).thenReturn(true);
        when(config.pnpmEnable()).thenReturn(true);
        when(config.bunEnable()).thenReturn(true);
        when(config.useGlobalPnpm()).thenReturn(true);
        when(config.requireHomeNodeExec()).thenReturn(true);
        when(config.skipDevBundleBuild()).thenReturn(true);
        when(config.npmExcludeWebComponents()).thenReturn(true);
        when(config.frontendIgnoreVersionChecks()).thenReturn(true);
        when(config.commercialWithBanner()).thenReturn(true);
        when(config.cleanFrontendFiles()).thenReturn(true);

        QuarkusPluginAdapter adapter = createAdapter();
        assertTrue(adapter.generateBundle());
        assertTrue(adapter.generateEmbeddableWebComponents());
        assertTrue(adapter.optimizeBundle());
        assertTrue(adapter.runNpmInstall());
        assertTrue(adapter.ciBuild());
        assertTrue(adapter.forceProductionBuild());
        assertTrue(adapter.eagerServerLoad());
        assertTrue(adapter.pnpmEnable());
        assertTrue(adapter.bunEnable());
        assertTrue(adapter.useGlobalPnpm());
        assertTrue(adapter.requireHomeNodeExec());
        assertTrue(adapter.skipDevBundleBuild());
        assertTrue(adapter.isNpmExcludeWebComponents());
        assertTrue(adapter.isFrontendIgnoreVersionChecks());
        assertTrue(adapter.isCommercialBannerEnabled());
        assertTrue(adapter.cleanFrontendFiles());
    }

    @Test
    void settingsTheAdapterDecides_doNotDependOnTheConfiguration() {
        QuarkusPluginAdapter adapter = createAdapter();

        // A Quarkus application is always packaged as a jar, always serves a
        // compressed bundle, and hot-deploys the frontend; the prepare
        // frontend cache is never disabled.
        assertTrue(adapter.isJarProject());
        assertTrue(adapter.compressBundle());
        assertTrue(adapter.isFrontendHotdeploy());
        assertFalse(adapter.isPrepareFrontendCacheDisabled());
    }

    @Test
    void reactEnabled_configured_isReportedAsConfigured() {
        when(config.reactEnabled()).thenReturn(Optional.of(false));

        assertFalse(createAdapter().isReactEnabled());
    }

    @Test
    void passThroughFiles_areNotResolvedAgainstAnything() {
        File applicationProperties = ROOT
                .resolve("elsewhere/application.properties").toFile();
        File openApi = ROOT.resolve("elsewhere/openapi.json").toFile();
        when(config.applicationProperties()).thenReturn(applicationProperties);
        when(config.openApiJsonFile()).thenReturn(openApi);

        QuarkusPluginAdapter adapter = createAdapter();
        assertEquals(applicationProperties, adapter.applicationProperties());
        assertEquals(openApi, adapter.openApiJsonFile());
    }

    @Test
    void relativeDirectories_areResolvedAgainstProjectOrBuildDirectory() {
        configureDirectoryDefaults();
        QuarkusPluginAdapter adapter = createAdapter();

        Path project = ROOT.resolve("project");
        Path classes = project.resolve(Paths.get("target", "classes"));

        // frontend sources are relative to the project
        assertEquals(
                project.resolve(Paths.get("src", "main", "frontend")).toFile(),
                adapter.frontendDirectory());
        assertEquals(
                project.resolve(Paths.get("src", "main", "resources",
                        "META-INF", "resources", "frontend")).toFile(),
                adapter.frontendResourcesDirectory());

        // build output is relative to the classes directory
        assertEquals(classes.resolve("resources").toFile(),
                adapter.resourcesOutputDirectory());
        assertEquals(classes.resolve("generated-resources").toFile(),
                adapter.servletResourceOutputDirectory());
        assertEquals(classes.resolve("frontend").toFile(),
                adapter.frontendOutputDirectory());
        // webpackOutputDirectory() is the deprecated spelling of the same
        assertEquals(adapter.frontendOutputDirectory(),
                adapter.webpackOutputDirectory());
    }

    @Test
    void absoluteExistingDirectory_isUsedUnchanged(@TempDir Path existing) {
        configureDirectoryDefaults();
        when(config.frontendDirectory()).thenReturn(existing.toFile());

        assertEquals(existing.toFile(), createAdapter().frontendDirectory());
    }

    @Test
    void directorySettingPointingAtAFile_reportsTheOffendingKey(
            @TempDir Path base) throws Exception {
        Path notADirectory = base.resolve("frontend");
        Files.writeString(notADirectory, "");
        when(appModule.getModuleDir()).thenReturn(base.toFile());
        configureDirectoryDefaults();
        when(config.frontendDirectory()).thenReturn(new File("frontend"));

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> createAdapter().frontendDirectory());
        assertTrue(
                exception.getMessage()
                        .contains("vaadin.build.frontendDirectory"),
                "the message should name the setting that is wrong: "
                        + exception.getMessage());
    }

    @Test
    void generatedTsFolder_notConfigured_sitsUnderTheFrontendDirectory() {
        configureDirectoryDefaults();

        QuarkusPluginAdapter adapter = createAdapter();
        assertEquals(
                adapter.frontendDirectory().toPath()
                        .resolve(FrontendUtils.GENERATED).toFile(),
                adapter.generatedTsFolder());
    }

    @Test
    void generatedTsFolder_configured_isUsedUnchanged() {
        configureDirectoryDefaults();
        File configured = ROOT.resolve("elsewhere/generated").toFile();
        when(config.generatedTsFolder()).thenReturn(Optional.of(configured));

        assertEquals(configured, createAdapter().generatedTsFolder());
    }

    @Test
    void frontendOutputDirectory_bothSpellingsConfigured_prefersTheCurrentOne() {
        configureDirectoryDefaults();
        when(config.webpackOutputDirectory())
                .thenReturn(Optional.of(new File("webpack")));

        Path classes = ROOT.resolve("project")
                .resolve(Paths.get("target", "classes"));
        assertEquals(classes.resolve("frontend").toFile(),
                createAdapter().frontendOutputDirectory());
    }

    @Test
    void npmFolder_notConfigured_isTheProjectDirectory() {
        configureDirectoryDefaults();

        QuarkusPluginAdapter adapter = createAdapter();
        assertEquals(adapter.projectBaseDirectory().toFile(),
                adapter.npmFolder());
    }

    @Test
    void npmFolder_configured_isResolvedAgainstTheProject() {
        configureDirectoryDefaults();
        when(config.npmFolder()).thenReturn(Optional.of(new File("ui")));

        assertEquals(ROOT.resolve(Paths.get("project", "ui")).toFile(),
                createAdapter().npmFolder());
    }

    @Test
    void nodeSettings_areTakenFromTheConfiguration() throws Exception {
        configureDirectoryDefaults();
        File nodeFolder = ROOT.resolve("nodes").toFile();
        when(config.nodeVersion()).thenReturn("v20.11.0");
        when(config.nodeFolder()).thenReturn(Optional.of(nodeFolder));
        when(config.nodeDownloadRoot())
                .thenReturn(Optional.of("https://example.test/node/"));

        QuarkusPluginAdapter adapter = createAdapter();
        assertEquals("v20.11.0", adapter.nodeVersion());
        assertEquals(nodeFolder.getAbsolutePath(), adapter.nodeFolder());
        assertEquals(new URI("https://example.test/node/"),
                adapter.nodeDownloadRoot());
    }

    @Test
    void nodeFolder_notConfigured_isNull() {
        configureDirectoryDefaults();

        // null leaves the frontend tools to pick their own location
        assertNull(createAdapter().nodeFolder());
    }

    @Test
    void nodeDownloadRoot_notAUri_reportsTheOffendingValue() {
        when(config.nodeDownloadRoot()).thenReturn(Optional.of("not a uri"));

        URISyntaxException exception = assertThrows(URISyntaxException.class,
                () -> createAdapter().nodeDownloadRoot());
        assertEquals("not a uri", exception.getInput());
    }

    @Test
    void listSettings_notConfigured_areEmptyRatherThanNull() {
        when(config.postinstallPackages()).thenReturn(Optional.empty());
        when(config.excludePostinstallPackages()).thenReturn(Optional.empty());
        when(config.frontendExtraFileExtensions()).thenReturn(Optional.empty());

        QuarkusPluginAdapter adapter = createAdapter();
        assertTrue(adapter.postinstallPackages().isEmpty());
        assertTrue(adapter.excludePostinstallPackages().isEmpty());
        assertTrue(adapter.frontendExtraFileExtensions().isEmpty());
    }

    @Test
    void listSettings_configured_areTakenFromTheConfiguration() {
        when(config.postinstallPackages())
                .thenReturn(Optional.of(List.of("some-package")));
        when(config.excludePostinstallPackages())
                .thenReturn(Optional.of(List.of("other-package")));
        when(config.frontendExtraFileExtensions())
                .thenReturn(Optional.of(List.of(".svg")));

        QuarkusPluginAdapter adapter = createAdapter();
        assertEquals(List.of("some-package"), adapter.postinstallPackages());
        assertEquals(List.of("other-package"),
                adapter.excludePostinstallPackages());
        assertEquals(List.of(".svg"), adapter.frontendExtraFileExtensions());
    }

    @Test
    void getJarFiles_reportsTheResolvedJarsAndSkipsDirectories(
            @TempDir Path repository) throws Exception {
        Path jar = repository.resolve("library.jar");
        Files.writeString(jar, "");
        Path classesDirectory = repository.resolve("classes");
        Files.createDirectory(classesDirectory);

        ResolvedDependency dependency = mock(ResolvedDependency.class);
        when(dependency.getResolvedPaths())
                .thenReturn(PathList.of(jar, classesDirectory));
        runtimeDependencies.add(dependency);

        assertEquals(Set.of(jar.toFile()), createAdapter().getJarFiles(),
                "a dependency resolved to a directory is not a jar");
    }

    @Test
    void sourceFolders_moduleWithoutMainSources_areTheStandardLayout() {
        QuarkusPluginAdapter adapter = createAdapter();

        Path project = ROOT.resolve("project");
        assertEquals(project.resolve(Paths.get("src", "main", "java")).toFile(),
                adapter.javaSourceFolder());
        assertEquals(
                project.resolve(Paths.get("src", "main", "resources")).toFile(),
                adapter.javaResourceFolder());
    }

    @Test
    void sourceFolders_moduleWithMainSources_areTheDeclaredOnes(
            @TempDir Path module) {
        Path sources = module.resolve("sources");
        Path resources = module.resolve("resources");
        Path classes = module.resolve("classes");
        ArtifactSources mainSources = mock(ArtifactSources.class);
        when(mainSources.getSourceDirs())
                .thenReturn(List.of(SourceDir.of(sources, classes)));
        when(mainSources.getResourceDirs())
                .thenReturn(List.of(SourceDir.of(resources, classes)));
        when(appModule.hasMainSources()).thenReturn(true);
        when(appModule.getMainSources()).thenReturn(mainSources);

        QuarkusPluginAdapter adapter = createAdapter();
        assertEquals(sources.toFile(), adapter.javaSourceFolder());
        assertEquals(resources.toFile(), adapter.javaResourceFolder());
        assertEquals(classes, adapter.buildDir());
    }

    @Test
    void logging_goesThroughWithoutFailing() {
        QuarkusPluginAdapter adapter = createAdapter();
        Throwable cause = new IllegalStateException("boom");

        // The adapter is the plugin's only way to report progress, so every
        // level has to be callable, with and without a cause.
        adapter.logDebug("debug");
        adapter.logDebug("debug", cause);
        adapter.logInfo("info");
        adapter.logWarn("warn");
        adapter.logWarn("warn", cause);
        adapter.logError("error");
        adapter.logError("error", cause);

        assertEquals(LoggerFactory.getLogger(QuarkusPluginAdapter.class)
                .isDebugEnabled(), adapter.isDebugEnabled());
    }

    @Test
    void constructedFromTheModelAlone_usesTheApplicationModule() {
        when(model.getApplicationModule()).thenReturn(appModule);
        configureDirectoryDefaults();

        QuarkusPluginAdapter adapter = new QuarkusPluginAdapter(config, model);

        assertEquals(ROOT.resolve("project"), adapter.projectBaseDirectory());
    }

    @Test
    void getClassFinder_moduleWithMainSources_seesTheOutputTreeAndTheJars(
            @TempDir Path module) throws Exception {
        Path classes = module.resolve("classes");
        Files.createDirectory(classes);
        Files.writeString(classes.resolve("from-output-tree.txt"), "");
        Path jar = module.resolve("library.jar");
        writeJarContaining(jar, "from-dependency.txt");

        ArtifactSources mainSources = mock(ArtifactSources.class);
        when(mainSources.getOutputTree())
                .thenReturn(PathTree.ofDirectoryOrArchive(classes));
        when(mainSources.getSourceDirs()).thenReturn(List.of());
        when(mainSources.getResourceDirs()).thenReturn(List.of());
        when(appModule.hasMainSources()).thenReturn(true);
        when(appModule.getMainSources()).thenReturn(mainSources);
        addResolvedDependency(jar);

        QuarkusPluginAdapter adapter = createAdapter();
        ClassFinder finder = adapter.getClassFinder();

        assertNotNull(finder.getResource("from-output-tree.txt"),
                "the module's own classes have to be scannable");
        assertNotNull(finder.getResource("from-dependency.txt"),
                "and so do the runtime dependencies");
        // Built once: the plugin asks for it repeatedly and scanning the
        // classpath again each time would be wasted work.
        assertSame(finder, adapter.getClassFinder());
    }

    @Test
    void getClassFinder_moduleWithoutMainSources_seesTheBuildDirectory(
            @TempDir Path module) throws Exception {
        // A module that declares no main sources has no output tree to read;
        // the constructor assumes the standard layout for it, and the class
        // finder has to assume the same rather than fail.
        when(appModule.getModuleDir()).thenReturn(module.toFile());
        when(appModule.getBuildDir())
                .thenReturn(module.resolve("target").toFile());
        when(appModule.hasMainSources()).thenReturn(false);
        Path classes = module.resolve(Paths.get("target", "classes"));
        Files.createDirectories(classes);
        Files.writeString(classes.resolve("from-build-directory.txt"), "");

        assertNotNull(
                createAdapter().getClassFinder()
                        .getResource("from-build-directory.txt"),
                "the compiled classes have to be scannable for a module "
                        + "without declared sources too");
    }

    private void addResolvedDependency(Path... paths) {
        ResolvedDependency dependency = mock(ResolvedDependency.class);
        when(dependency.getResolvedPaths()).thenReturn(PathList.of(paths));
        runtimeDependencies.add(dependency);
    }

    private void writeJarContaining(Path jar, String entry) throws Exception {
        try (JarOutputStream out = new JarOutputStream(
                Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry(entry));
            out.closeEntry();
        }
    }

    @Test
    void nodeDownloadRoot_notConfigured_isTheDefaultForThisPlatform()
            throws Exception {
        when(config.nodeDownloadRoot()).thenReturn(Optional.empty());

        assertEquals(new URI(NodeInstaller.getDownloadRoot(Platform.guess())),
                createAdapter().nodeDownloadRoot());
    }

    @Test
    void applicationIdentifier_blank_isTreatedAsNotConfigured() {
        when(config.applicationIdentifier()).thenReturn(Optional.of("   "));
        ResolvedDependency artifact = mock(ResolvedDependency.class);
        when(artifact.getGroupId()).thenReturn("com.example");
        when(artifact.getArtifactId()).thenReturn("demo");
        when(model.getAppArtifact()).thenReturn(artifact);

        assertEquals(
                "app-" + StringUtil.getHash("com.example:demo",
                        StandardCharsets.UTF_8),
                createAdapter().applicationIdentifier());
    }
}
