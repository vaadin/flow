package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FileIOUtils;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.TaskGenerateEndpoint;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;
import com.vaadin.flow.server.frontend.TaskRunNpmInstall;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.utils.LookupImpl;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.Product;

import static com.vaadin.flow.server.frontend.FrontendUtils.FEATURE_FLAGS_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

public class BuildFrontendUtilTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    private File baseDir;

    private PluginAdapterBuild adapter;

    private Lookup lookup;

    private File statsJson;
    private File resourceOutput;

    @Before
    public void setup() throws Exception {
        baseDir = tmpDir.newFolder();

        adapter = Mockito.mock(PluginAdapterBuild.class);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        Mockito.when(adapter.generatedTsFolder())
                .thenReturn(new File(baseDir, "src/main/frontend/generated"));
        Mockito.when(adapter.projectBaseDirectory())
                .thenReturn(tmpDir.getRoot().toPath());
        Mockito.when(adapter.applicationIdentifier()).thenReturn("TEST_APP_ID");
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                getClass().getClassLoader());
        lookup = Mockito.spy(new LookupImpl(classFinder));
        Mockito.when(adapter.createLookup(Mockito.any())).thenReturn(lookup);
        Mockito.doReturn(classFinder).when(lookup).lookup(ClassFinder.class);

        // setup: mock a vite executable
        File viteBin = new File(baseDir, "node_modules/vite/bin");
        Assert.assertTrue(viteBin.mkdirs());
        File viteExecutableMock = new File(viteBin, "vite.js");
        Assert.assertTrue(viteExecutableMock.createNewFile());

        resourceOutput = new File(baseDir, "resOut");
        Mockito.when(adapter.servletResourceOutputDirectory())
                .thenReturn(resourceOutput);
        statsJson = new File(new File(resourceOutput, "config"), "stats.json");
        statsJson.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(statsJson)) {
            IOUtils.write("{\"npmModules\":{}}", out, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void should_notUseHilla_inPrepareFrontend()
            throws ExecutionFailedException, IOException, URISyntaxException {
        setupPluginAdapterDefaults();

        File openApiJsonFile = new File(new File(baseDir, Constants.TARGET),
                "classes/com/vaadin/hilla/openapi.json");
        Mockito.when(adapter.openApiJsonFile()).thenReturn(openApiJsonFile);

        BuildFrontendUtil.prepareFrontend(adapter);

        // no hilla no lookup call
        Mockito.verify(lookup, Mockito.never())
                .lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateOpenAPI.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateEndpoint.class);
        Mockito.verify(adapter, Mockito.never()).openApiJsonFile();
    }

    @Test
    public void should_useHillaEngine_withNodeUpdater()
            throws URISyntaxException, ExecutionFailedException, IOException {
        setupPluginAdapterDefaults();

        MockedConstruction<TaskRunNpmInstall> construction = Mockito
                .mockConstruction(TaskRunNpmInstall.class);

        final EndpointGeneratorTaskFactory endpointGeneratorTaskFactory = Mockito
                .mock(EndpointGeneratorTaskFactory.class);
        Mockito.doReturn(endpointGeneratorTaskFactory).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        final TaskGenerateOpenAPI taskGenerateOpenAPI = Mockito
                .mock(TaskGenerateOpenAPI.class);
        Mockito.doReturn(taskGenerateOpenAPI).when(endpointGeneratorTaskFactory)
                .createTaskGenerateOpenAPI(Mockito.any());

        final TaskGenerateEndpoint taskGenerateEndpoint = Mockito
                .mock(TaskGenerateEndpoint.class);
        Mockito.doReturn(taskGenerateEndpoint)
                .when(endpointGeneratorTaskFactory)
                .createTaskGenerateEndpoint(Mockito.any());

        FrontendDependenciesScanner frontendDependencies = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(frontendDependencies.getPwaConfiguration())
                .thenReturn(new PwaConfiguration());
        try (MockedStatic<FrontendUtils> util = Mockito
                .mockStatic(FrontendUtils.class, Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> FrontendUtils.isHillaUsed(Mockito.any(),
                    Mockito.any())).thenReturn(true);
            BuildFrontendUtil.runNodeUpdater(adapter, frontendDependencies);
        }

        Mockito.verify(lookup).lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateOpenAPI.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateEndpoint.class);

        // Hilla Engine requires npm install, the order of execution is critical
        final TaskRunNpmInstall taskRunNpmInstall = construction.constructed()
                .get(0);
        InOrder inOrder = Mockito.inOrder(taskRunNpmInstall,
                taskGenerateOpenAPI, taskGenerateEndpoint);
        inOrder.verify(taskRunNpmInstall).execute();
        inOrder.verify(taskGenerateOpenAPI).execute();
        inOrder.verify(taskGenerateEndpoint).execute();
    }

    @Test
    public void detectsUsedCommercialComponents() {

        String statsJson = """
                    {
                        "cvdlModules": {
                        "component": {
                            "name": "component",
                            "version":"1.2.3"
                           },
                           "comm-component": {
                            "name":"comm-comp",
                            "version":"4.6.5"
                           },
                           "comm-component2": {
                            "name":"comm-comp2",
                            "version":"4.6.5"
                           }
                        }
                    }
                """;

        final FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("comm-component", "4.6.5");
        packages.put("comm-component2", "4.6.5");
        packages.put("@vaadin/button", "1.2.1");
        Mockito.when(scanner.getPackages()).thenReturn(packages);

        List<String> modules = new ArrayList<>();
        modules.add("comm-component/foo.js");
        Map<ChunkInfo, List<String>> modulesMap = Collections
                .singletonMap(ChunkInfo.GLOBAL, modules);
        Mockito.when(scanner.getModules()).thenReturn(modulesMap);

        List<Product> components = BuildFrontendUtil
                .findCommercialFrontendComponents(scanner, statsJson);
        // Two components are included, only one is used
        Assert.assertEquals(1, components.size());
        Assert.assertEquals("comm-comp", components.get(0).getName());
        Assert.assertEquals("4.6.5", components.get(0).getVersion());
    }

    @Test
    public void propagateBuildInfo_tokenFileNotExisting_createTokenFile()
            throws Exception {
        prepareAndAssertTokenFile();
    }

    @Test
    public void propagateBuildInfo_existingTokenFileWithDifferentContent_overwritesTokenFile()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();
        long lastModified = tokenFile.lastModified();

        Thread.sleep(100);
        Mockito.when(adapter.nodeVersion()).thenReturn("v1.0.0");
        BuildFrontendUtil.propagateBuildInfo(adapter);

        Assert.assertNotEquals("Expected token file to be updated, but was not",
                lastModified, tokenFile.lastModified());
    }

    @Test
    public void propagateBuildInfo_existingTokenFileWithSameContent_doesNotWriteTokenFile()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();
        long lastModified = tokenFile.lastModified();

        Thread.sleep(100);

        BuildFrontendUtil.propagateBuildInfo(adapter);
        Assert.assertEquals(
                "Expected token file not to be updated, but it has been written",
                lastModified, tokenFile.lastModified());
    }

    @Test
    public void prepareFrontend_shouldCleanUnusedGeneratedFiles()
            throws Exception {
        fillAdapter();
        File frontendGeneratedFolder = new File(
                new File(tmpDir.getRoot(), "frontend"), "generated");
        Mockito.when(adapter.generatedTsFolder())
                .thenReturn(frontendGeneratedFolder);

        // First run to create generated files
        BuildFrontendUtil.prepareFrontend(adapter);

        Set<Path> expectedGeneratedFiles = Files
                .walk(frontendGeneratedFolder.toPath())
                .filter(Files::isRegularFile).collect(Collectors.toSet());

        // Adding additional files that should be removed
        Set<Path> additionalFiles = new HashSet<>();
        additionalFiles
                .add(frontendGeneratedFolder.toPath().resolve("test.js"));
        additionalFiles.add(frontendGeneratedFolder.toPath()
                .resolve(Paths.get("sub", "other.js")));
        for (Path additional : additionalFiles) {
            Files.createDirectories(additional.getParent());
            Files.writeString(additional, "");
        }

        // Run again to verify useless files have been removed
        BuildFrontendUtil.prepareFrontend(adapter);
        Set<Path> generatedFiles = Files.walk(frontendGeneratedFolder.toPath())
                .filter(Files::isRegularFile).collect(Collectors.toSet());

        Assert.assertTrue(
                "Files not generated by prepare frontend should not be present",
                generatedFiles.stream().noneMatch(additionalFiles::contains));
        Assert.assertEquals("Expecting same generated files to be present",
                expectedGeneratedFiles, generatedFiles);

    }

    @Test
    public void updateBuildFile_tokenFileNotExisting_doNothing()
            throws Exception {
        fillAdapter();

        BuildFrontendUtil.updateBuildFile(adapter, false);
        File tokenFile = new File(resourceOutput, TOKEN_FILE);
        Assert.assertFalse("Token file should not have been created",
                tokenFile.exists());
    }

    @Test
    public void updateBuildFile_tokenExisting_developmentEntriesRemoved()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();
        JsonNode buildInfoJsonDev = JacksonUtils
                .readTree(Files.readString(tokenFile.toPath()));

        BuildFrontendUtil.updateBuildFile(adapter, false);
        Assert.assertTrue("Token file should still exist", tokenFile.exists());
        JsonNode buildInfoJsonProd = JacksonUtils
                .readTree(Files.readString(tokenFile.toPath()));

        Set<String> removedKeys = JacksonUtils.getKeys(buildInfoJsonDev)
                .stream().filter(key -> !buildInfoJsonProd.has(key))
                .collect(Collectors.toSet());
        Assert.assertFalse(
                "Development entries have not been removed from token file",
                removedKeys.isEmpty());
    }

    @Test
    public void updateBuildFile_tokenExisting_applicationIdentifierAdded()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();

        BuildFrontendUtil.updateBuildFile(adapter, false);
        Assert.assertTrue("Token file should still exist", tokenFile.exists());
        JsonNode buildInfoJsonProd = JacksonUtils
                .readTree(Files.readString(tokenFile.toPath()));
        Assert.assertEquals("Wrong application identifier in token file",
                "TEST_APP_ID",
                buildInfoJsonProd.get(InitParameters.APPLICATION_IDENTIFIER)
                        .textValue());
    }

    @Test
    public void updateBuildFile_tokenExisting_licenseRequiredAndSubscriptionKey_dauFlagAdded()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();
        withMockedLicenseChecker(false, () -> {
            String subscriptionKey = System
                    .getProperty("vaadin.subscriptionKey");
            System.setProperty("vaadin.subscriptionKey", "sub-123");
            try {
                BuildFrontendUtil.updateBuildFile(adapter, true);
            } finally {
                if (subscriptionKey != null) {
                    System.setProperty("vaadin.subscriptionKey",
                            subscriptionKey);
                } else {
                    System.clearProperty("vaadin.subscriptionKey");
                }
            }
            Assert.assertTrue("Token file should still exist",
                    tokenFile.exists());
            JsonNode buildInfoJsonProd = JacksonUtils
                    .readTree(Files.readString(tokenFile.toPath()));
            Assert.assertTrue("DAU flag should be active in token file",
                    buildInfoJsonProd.get(Constants.DAU_TOKEN).booleanValue());
        });
    }

    @Test
    public void updateBuildFile_tokenExisting_licenseNotRequiredAndSubscriptionKey_dauFlagNotAdded()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();

        addPremiumFeatureAndDAUFlagTrue(tokenFile);

        String subscriptionKey = System.getProperty("vaadin.subscriptionKey");
        System.setProperty("vaadin.subscriptionKey", "sub-123");
        try {
            BuildFrontendUtil.updateBuildFile(adapter, false);
        } finally {
            if (subscriptionKey != null) {
                System.setProperty("vaadin.subscriptionKey", subscriptionKey);
            } else {
                System.clearProperty("vaadin.subscriptionKey");
            }
        }
        Assert.assertTrue("Token file should still exist", tokenFile.exists());
        JsonNode buildInfoJsonProd = JacksonUtils
                .readTree(Files.readString(tokenFile.toPath()));
        Assert.assertFalse("DAU flag should not be present in token file",
                buildInfoJsonProd.has(Constants.DAU_TOKEN));
    }

    @Test
    public void updateBuildFile_tokenExisting_licenseRequiredNoSubscriptionKey_dauFlagNotAdded()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();
        withMockedLicenseChecker(false, () -> {
            String subscriptionKey = System
                    .getProperty("vaadin.subscriptionKey");
            System.clearProperty("vaadin.subscriptionKey");
            try {
                BuildFrontendUtil.updateBuildFile(adapter, true);
            } finally {
                if (subscriptionKey != null) {
                    System.setProperty("vaadin.subscriptionKey",
                            subscriptionKey);
                } else {
                    System.clearProperty("vaadin.subscriptionKey");
                }
            }
            Assert.assertTrue("Token file should still exist",
                    tokenFile.exists());
            JsonNode buildInfoJsonProd = JacksonUtils
                    .readTree(Files.readString(tokenFile.toPath()));
            Assert.assertFalse("DAU flag should not be present in token file",
                    buildInfoJsonProd.has(Constants.DAU_TOKEN));
        });
    }

    @Test
    public void updateBuildFile_tokenExisting_licenseRequiredAndIsPremiumLike_premiumFeaturesFlagAdded()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();

        addPremiumFeatureAndDAUFlagTrue(tokenFile);

        ClassLoader classLoader = new URLClassLoader(
                new URL[] { new File(baseDir, "target/test-classes/").toURI()
                        .toURL() },
                BuildFrontendUtilTest.class.getClassLoader());
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                classLoader);
        Mockito.when(adapter.getClassFinder()).thenReturn(classFinder);

        withMockedLicenseChecker(true, () -> {
            BuildFrontendUtil.updateBuildFile(adapter, true);
            Assert.assertTrue("Token file should still exist",
                    tokenFile.exists());
            JsonNode buildInfoJsonProd = JacksonUtils
                    .readTree(Files.readString(tokenFile.toPath()));
            Assert.assertTrue(
                    Constants.PREMIUM_FEATURES
                            + " flag should be active in token file",
                    buildInfoJsonProd.get(Constants.PREMIUM_FEATURES)
                            .booleanValue());
        });
    }

    @Test
    public void updateBuildFile_tokenExisting_licenseRequiredAndIsNotPremiumLike_premiumFeaturesFlagNotAdded()
            throws Exception {
        File tokenFile = prepareAndAssertTokenFile();

        addPremiumFeatureAndDAUFlagTrue(tokenFile);

        withMockedLicenseChecker(false, () -> {
            BuildFrontendUtil.updateBuildFile(adapter, true);
            Assert.assertTrue("Token file should still exist",
                    tokenFile.exists());
            JsonNode buildInfoJsonProd = JacksonUtils
                    .readTree(Files.readString(tokenFile.toPath()));
            Assert.assertFalse(
                    Constants.PREMIUM_FEATURES
                            + " flag should not be active in token file",
                    buildInfoJsonProd.has(Constants.PREMIUM_FEATURES));
        });
    }

    private void withMockedLicenseChecker(boolean isValidLicense,
            ThrowingRunnable test) throws IOException {
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class, Answers.RETURNS_DEEP_STUBS)) {
            licenseChecker
                    .when(() -> LicenseChecker.isValidLicense(Mockito.any(),
                            Mockito.any(), Mockito.any()))
                    .thenReturn(isValidLicense);
            test.run();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws IOException;
    }

    private File prepareAndAssertTokenFile() throws URISyntaxException {
        fillAdapter();

        BuildFrontendUtil.propagateBuildInfo(adapter);

        File tokenFile = new File(resourceOutput, TOKEN_FILE);
        Assert.assertTrue("Token file should have been created",
                tokenFile.exists());
        return tokenFile;
    }

    @Test
    public void runNodeUpdater_generateFeatureFlagsJsFile() throws Exception {
        setupPluginAdapterDefaults();

        File targetDir = baseDir.toPath().resolve(adapter.buildFolder())
                .toFile();
        targetDir.mkdirs();
        targetDir.deleteOnExit();
        File testClassesDir = targetDir.toPath().resolve("test-classes")
                .toFile();
        testClassesDir.mkdirs();
        testClassesDir.deleteOnExit();
        File featureFlagsResourceFile = new File(testClassesDir,
                FeatureFlags.PROPERTIES_FILENAME);
        FileUtils.write(featureFlagsResourceFile,
                "com.vaadin.experimental.exampleFeatureFlag = true\n");
        featureFlagsResourceFile.deleteOnExit();

        ClassLoader classLoader = new URLClassLoader(
                new URL[] { new File(baseDir, "target/test-classes/").toURI()
                        .toURL() },
                BuildFrontendUtilTest.class.getClassLoader());
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                classLoader);
        Mockito.when(adapter.getClassFinder()).thenReturn(classFinder);
        lookup = Mockito.spy(new LookupImpl(classFinder));
        Mockito.when(adapter.createLookup(Mockito.any())).thenReturn(lookup);
        Mockito.doReturn(classFinder).when(lookup).lookup(ClassFinder.class);

        FrontendDependenciesScanner frontendDependencies = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(frontendDependencies.getPwaConfiguration())
                .thenReturn(new PwaConfiguration());
        BuildFrontendUtil.runNodeUpdater(adapter, frontendDependencies);

        File generatedFeatureFlagsFile = new File(adapter.generatedTsFolder(),
                FEATURE_FLAGS_FILE_NAME);
        String featureFlagsJs = Files
                .readString(generatedFeatureFlagsFile.toPath())
                .replace("\r\n", "\n");

        Assert.assertTrue("Example feature should not be set at build time",
                featureFlagsJs.contains(
                        "window.Vaadin.featureFlags.exampleFeatureFlag = false;\n"));
    }

    private void fillAdapter() throws URISyntaxException {
        Mockito.when(adapter.nodeDownloadRoot())
                .thenReturn(URI.create("http://something/node/"));
        Mockito.when(adapter.nodeVersion()).thenReturn("v0.0.0");
        Mockito.when(adapter.frontendDirectory())
                .thenReturn(new File(tmpDir.getRoot(), "frontend"));
        Mockito.when(adapter.javaSourceFolder())
                .thenReturn(new File(tmpDir.getRoot(), "src/main/java"));
        Mockito.when(adapter.javaResourceFolder())
                .thenReturn(new File(tmpDir.getRoot(), "src/main/resources"));
        Mockito.when(adapter.applicationProperties()).thenReturn(new File(
                tmpDir.getRoot(), "src/main/resources/application.properties"));
        Mockito.when(adapter.openApiJsonFile()).thenReturn(new File(
                tmpDir.getRoot(), "target/generated-resources/openapi.json"));
        Mockito.when(adapter.buildFolder()).thenReturn("target");
    }

    private void writePackageJson(File nodeModulesFolder, String name,
            String version, String cvdlName) throws IOException {
        File componentFolder = new File(nodeModulesFolder, name);
        componentFolder.mkdirs();
        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("name", name);
        json.put("version", version);
        if (cvdlName == null) {
            json.put("license", "MIT");
        } else {
            json.put("license", "CVDL");
            json.put("cvdlName", cvdlName);
        }
        FileUtils.write(new File(componentFolder, "package.json"),
                json.toString(), StandardCharsets.UTF_8);

    }

    private void setupPluginAdapterDefaults() throws URISyntaxException {
        Mockito.when(adapter.nodeVersion())
                .thenReturn(FrontendTools.DEFAULT_NODE_VERSION);
        Mockito.when(adapter.nodeDownloadRoot()).thenReturn(
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        Mockito.when(adapter.frontendDirectory()).thenReturn(
                new File(baseDir, FrontendUtils.DEFAULT_FRONTEND_DIR));
        Mockito.when(adapter.buildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        File javaSourceFolder = new File(baseDir, "src/main/java");
        Assert.assertTrue(javaSourceFolder.mkdirs());
        Mockito.when(adapter.javaSourceFolder()).thenReturn(javaSourceFolder);
        File javaResourceFolder = new File(baseDir, "src/main/resources");
        Assert.assertTrue(javaResourceFolder.mkdirs());
        Mockito.when(adapter.javaResourceFolder())
                .thenReturn(javaResourceFolder);
        Mockito.when(adapter.openApiJsonFile())
                .thenReturn(new File(new File(baseDir, Constants.TARGET),
                        "classes/com/vaadin/hilla/openapi.json"));
        Mockito.when(adapter.getClassFinder())
                .thenReturn(new ClassFinder.DefaultClassFinder(
                        this.getClass().getClassLoader()));
        Mockito.when(adapter.runNpmInstall()).thenReturn(true);
    }

    private void addPremiumFeatureAndDAUFlagTrue(File tokenFile)
            throws IOException {
        // simulates true value placed into pre-compiled bundle
        // when bundle is compiled on Vaadin CI server
        String tokenJson = FileUtils.readFileToString(tokenFile,
                StandardCharsets.UTF_8);
        ObjectNode buildInfo = JacksonUtils.readTree(tokenJson);
        buildInfo.put(Constants.PREMIUM_FEATURES, true);
        buildInfo.put(Constants.DAU_TOKEN, true);

        FileIOUtils.writeIfChanged(tokenFile,
                buildInfo.toPrettyString() + "\n");
    }
}
