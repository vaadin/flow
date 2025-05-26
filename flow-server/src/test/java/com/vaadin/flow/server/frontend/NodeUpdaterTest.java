/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NodeUpdaterTest {

    private static final String POLYMER_VERSION = "3.5.2";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private File npmFolder;

    private ClassFinder finder;

    private Options options;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        FeatureFlags featureFlags = Mockito.mock(FeatureFlags.class);
        finder = Mockito.spy(new ClassFinder.DefaultClassFinder(
                this.getClass().getClassLoader()));
        options = new MockOptions(finder, npmFolder).withBuildDirectory(TARGET)
                .withFeatureFlags(featureFlags);

        nodeUpdater = new NodeUpdater(Mockito.mock(FrontendDependencies.class),
                options) {

            @Override
            public void execute() {
                // NO-OP
            }

        };
    }

    @Test
    public void getGeneratedModules_should_includeOnlyWebComponents()
            throws IOException {
        File frontend = temporaryFolder.newFolder();
        File generated = new File(frontend, FrontendUtils.GENERATED);
        File flow = new File(generated, "flow");
        File webComponents = new File(flow, "web-components");
        File fileA = new File(webComponents, "a.js");
        File fileB = new File(webComponents, "b.js");
        File fileC = new File(generated, "c.js");
        create(fileA);
        create(fileB);
        create(fileC);

        Set<String> modules = NodeUpdater.getGeneratedModules(frontend);

        Assert.assertEquals(
                Set.of("web-components/a.js", "web-components/b.js"), modules);
    }

    private void create(File file) throws IOException {
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    @Test
    public void getDefaultDependencies_includesAllDependencies() {
        Map<String, String> defaultDeps = nodeUpdater.getDefaultDependencies();
        Set<String> expectedDependencies = new HashSet<>();
        expectedDependencies.add("@polymer/polymer");
        expectedDependencies.add("@vaadin/common-frontend");
        expectedDependencies.add("construct-style-sheets-polyfill");
        expectedDependencies.add("lit");
        expectedDependencies.add("react");
        expectedDependencies.add("react-dom");
        expectedDependencies.add("react-router");

        Set<String> actualDependendencies = defaultDeps.keySet();

        Assert.assertEquals(expectedDependencies, actualDependendencies);
    }

    @Test
    public void getDefaultDevDependencies_includesAllDependencies_whenUsingVite() {
        Map<String, String> defaultDeps = nodeUpdater
                .getDefaultDevDependencies();
        Set<String> expectedDependencies = getCommonDevDeps();

        // Vite
        expectedDependencies.add("vite");
        expectedDependencies.add("@vitejs/plugin-react");
        expectedDependencies.add("rollup-plugin-brotli");
        expectedDependencies.add("@rollup/plugin-replace");
        expectedDependencies.add("@rollup/pluginutils");
        expectedDependencies.add("rollup-plugin-visualizer");
        expectedDependencies.add("vite-plugin-checker");
        expectedDependencies.add("workbox-build");
        expectedDependencies.add("transform-ast");
        expectedDependencies.add("strip-css-comments");
        expectedDependencies.add("@babel/preset-react");
        expectedDependencies.add("@types/react");
        expectedDependencies.add("@types/react-dom");
        expectedDependencies.add("@preact/signals-react-transform");
        expectedDependencies.add("magic-string");

        Set<String> actualDependendencies = defaultDeps.keySet();

        Assert.assertEquals(expectedDependencies, actualDependendencies);
    }

    private Set<String> getCommonDevDeps() {
        Set<String> expectedDependencies = new HashSet<>();
        expectedDependencies.add("typescript");
        expectedDependencies.add("workbox-core");
        expectedDependencies.add("workbox-precaching");
        expectedDependencies.add("glob");
        expectedDependencies.add("async");
        return expectedDependencies;
    }

    @Test
    public void updateMainDefaultDependencies_polymerVersionIsNull_useDefault() {
        ObjectNode object = JacksonUtils.createObjectNode();
        nodeUpdater.addVaadinDefaultsToJson(object);
        nodeUpdater.updateDefaultDependencies(object);

        String version = getPolymerVersion(object);
        Assert.assertEquals(POLYMER_VERSION, version);
    }

    @Test
    public void updateMainDefaultDependencies_polymerVersionIsProvidedByUser_useProvided() {
        ObjectNode object = JacksonUtils.createObjectNode();
        ObjectNode dependencies = JacksonUtils.createObjectNode();
        dependencies.put("@polymer/polymer", "4.0.0");
        object.set(NodeUpdater.DEPENDENCIES, dependencies);
        nodeUpdater.addVaadinDefaultsToJson(object);

        nodeUpdater.updateDefaultDependencies(object);

        String version = getPolymerVersion(object);
        Assert.assertEquals("4.0.0", version);
    }

    @Test
    public void updateMainDefaultDependencies_vaadinIsProvidedByUser_useDefault() {
        ObjectNode object = JacksonUtils.createObjectNode();

        ObjectNode vaadin = JacksonUtils.createObjectNode();
        vaadin.put("disableUsageStatistics", true);
        object.set(NodeUpdater.VAADIN_DEP_KEY, vaadin);

        nodeUpdater.addVaadinDefaultsToJson(object);
        nodeUpdater.updateDefaultDependencies(object);

        Assert.assertEquals(POLYMER_VERSION, getPolymerVersion(object));
        Assert.assertEquals(POLYMER_VERSION,
                getPolymerVersion(object.get(NodeUpdater.VAADIN_DEP_KEY)));
    }

    @Test
    public void updateDefaultDependencies_olderVersionsAreUpdated()
            throws IOException {
        ObjectNode packageJson = nodeUpdater.getPackageJson();
        packageJson.set(NodeUpdater.DEPENDENCIES,
                JacksonUtils.createObjectNode());
        packageJson.set(NodeUpdater.DEV_DEPENDENCIES,
                JacksonUtils.createObjectNode());
        ((ObjectNode) packageJson.get(NodeUpdater.DEV_DEPENDENCIES)).put("glob",
                "7.0.0");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("11.0.3", packageJson
                .get(NodeUpdater.DEV_DEPENDENCIES).get("glob").textValue());
    }

    @Test // #6907 test when user has set newer versions
    public void updateDefaultDependencies_newerVersionsAreNotChanged()
            throws IOException {
        ObjectNode packageJson = nodeUpdater.getPackageJson();
        packageJson.set(NodeUpdater.DEPENDENCIES,
                JacksonUtils.createObjectNode());
        packageJson.set(NodeUpdater.DEV_DEPENDENCIES,
                JacksonUtils.createObjectNode());
        ((ObjectNode) packageJson.get(NodeUpdater.DEV_DEPENDENCIES)).put("vite",
                "78.2.3");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("78.2.3", packageJson
                .get(NodeUpdater.DEV_DEPENDENCIES).get("vite").textValue());
    }

    @Test
    public void shouldUpdateExistingLocalFormPackageToNpmPackage() {
        ObjectNode packageJson = JacksonUtils.createObjectNode();
        ObjectNode dependencies = JacksonUtils.createObjectNode();
        packageJson.set(NodeUpdater.DEPENDENCIES, dependencies);
        ObjectNode vaadinDependencies = JacksonUtils.createObjectNode();
        vaadinDependencies.set(NodeUpdater.DEPENDENCIES,
                JacksonUtils.createObjectNode());
        packageJson.set(NodeUpdater.VAADIN_DEP_KEY, vaadinDependencies);

        String formPackage = "@vaadin/form";
        String legecyVersion = "./target/flow-frontend/form";
        String newVersion = "22.0.0";

        dependencies.put(formPackage, legecyVersion);

        nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES,
                formPackage, newVersion);

        Assert.assertEquals(newVersion, packageJson
                .get(NodeUpdater.DEPENDENCIES).get(formPackage).textValue());
    }

    @Test
    public void shouldSkipUpdatingNonParsableVersions() {
        ObjectNode packageJson = JacksonUtils.createObjectNode();
        ObjectNode dependencies = JacksonUtils.createObjectNode();
        packageJson.set(NodeUpdater.DEPENDENCIES, dependencies);
        ObjectNode vaadinDependencies = JacksonUtils.createObjectNode();
        vaadinDependencies.set(NodeUpdater.DEPENDENCIES,
                JacksonUtils.createObjectNode());
        packageJson.set(NodeUpdater.VAADIN_DEP_KEY, vaadinDependencies);

        String formPackage = "@vaadin/form";
        String existingVersion = "../../../some/local/path";
        String newVersion = "2.0.0";

        dependencies.put(formPackage, existingVersion);

        nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES,
                formPackage, newVersion);

        Assert.assertEquals(existingVersion, packageJson
                .get(NodeUpdater.DEPENDENCIES).get(formPackage).textValue());
    }

    @Test
    public void canUpdateNonParseableVersions() {
        ObjectNode packageJson = JacksonUtils.createObjectNode();
        ObjectNode dependencies = JacksonUtils.createObjectNode();
        packageJson.set(NodeUpdater.DEPENDENCIES, dependencies);
        ObjectNode vaadinDependencies = JacksonUtils.createObjectNode();
        vaadinDependencies.set(NodeUpdater.DEPENDENCIES,
                JacksonUtils.createObjectNode());
        packageJson.set(NodeUpdater.VAADIN_DEP_KEY, vaadinDependencies);

        String pkg = "mypackage";
        String existingVersion = "./some/path";

        dependencies.put(pkg, existingVersion);

        nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES, pkg,
                existingVersion);

        Assert.assertEquals(existingVersion,
                packageJson.get(NodeUpdater.DEPENDENCIES).get(pkg).textValue());

    }

    @Test
    public void getJsonFileContent_incorrectPackageJsonContent_throwsExceptionWithFileName()
            throws IOException {
        File brokenPackageJsonFile = temporaryFolder
                .newFile("broken-package.json");
        FileUtils.writeStringToFile(brokenPackageJsonFile,
                "{ some broken json ", UTF_8);

        RuntimeException exception = Assert.assertThrows(RuntimeException.class,
                () -> NodeUpdater.getJsonFileContent(brokenPackageJsonFile));

        MatcherAssert.assertThat(exception.getMessage(),
                StringContains.containsString("Cannot parse package file "));
        MatcherAssert.assertThat(exception.getMessage(),
                StringContains.containsString("broken-package.json"));
    }

    @Test
    @Ignore("Can be removed if we agree on ignoring potential issues in [23 + webpack] -> [25] upgrades")
    public void removedAllOldAndExistingPlugins() throws IOException {
        File packageJson = new File(npmFolder, "package.json");
        FileWriter packageJsonWriter = new FileWriter(packageJson);
        packageJsonWriter
                .write("""
                        {
                          "devDependencies": {
                            "@vaadin/some-old-plugin": "./target/plugins/some-old-plugin",
                            "@vaadin/application-theme-plugin": "./target/plugins/application-theme-plugin"
                          }
                        }
                        """);
        packageJsonWriter.close();
        ObjectNode actualDevDeps = (ObjectNode) nodeUpdater.getPackageJson()
                .get(NodeUpdater.DEV_DEPENDENCIES);
        Assert.assertFalse(actualDevDeps.has("@vaadin/some-old-plugin"));
        Assert.assertFalse(
                actualDevDeps.has("@vaadin/application-theme-plugin"));
    }

    @Test
    public void generateVersionsJson_noVersions_noDevDeps_versionsGeneratedFromPackageJson()
            throws IOException {
        nodeUpdater.generateVersionsJson(JacksonUtils.createObjectNode());
        Assert.assertEquals("{}", nodeUpdater.versionsJson.toString());
    }

    @Test
    public void generateVersionsJson_versionsGeneratedFromPackageJson_containsBothDepsAndDevDeps()
            throws IOException {

        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        // @formatter:off
        FileUtils.write(packageJson,
            """
                {
                  "vaadin": {
                    "dependencies": {
                      "lit": "2.0.0",
                      "@vaadin/router": "1.7.5",
                      "@polymer/polymer": "3.4.1"
                    },
                    "devDependencies": {
                      "css-loader": "4.2.1",
                      "file-loader": "6.1.0"
                    }
                  },
                  "dependencies": {
                    "lit": "2.0.0",
                    "@vaadin/router": "1.7.5",
                    "@polymer/polymer": "3.4.1"
                  },
                  "devDependencies": {
                    "css-loader": "4.2.1",
                    "file-loader": "6.1.0"
                  }
                }
                """, StandardCharsets.UTF_8);
        // @formatter:on

        nodeUpdater.generateVersionsJson(JacksonUtils.readTree(FileUtils
                .readFileToString(packageJson, StandardCharsets.UTF_8)));
        Assert.assertEquals(
                "{\"lit\":\"2.0.0\",\"@vaadin/router\":\"1.7.5\",\"@polymer/polymer\":\"3.4.1\"}",
                nodeUpdater.versionsJson.toString());
    }

    @Test
    public void testGetPlatformPinnedDependencies_vaadinCoreVersionIsNotPresent_outputIsEmptyJson()
            throws IOException {
        Logger logger = Mockito.spy(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMocked = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactoryMocked
                    .when(() -> LoggerFactory.getLogger(nodeUpdater.getClass()))
                    .thenReturn(logger);

            Mockito.when(
                    finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                    .thenReturn(null);
            Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                    .thenReturn(null);

            ObjectNode pinnedVersions = nodeUpdater
                    .getPlatformPinnedDependencies();
            Assert.assertEquals(0, JacksonUtils.getKeys(pinnedVersions).size());

            Mockito.verify(logger, Mockito.times(1)).info(
                    "Couldn't find {} file to pin dependency versions for core components."
                            + " Transitive dependencies won't be pinned for npm/pnpm/bun.",
                    Constants.VAADIN_CORE_VERSIONS_JSON);
        }
    }

    @Test
    public void testGetPlatformPinnedDependencies_onlyVaadinCoreVersionIsPresent_outputContainsOnlyCoreVersions()
            throws IOException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        ObjectNode mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();
        Assert.assertTrue(mockedVaadinCoreJson.has("core"));
        Assert.assertTrue(mockedVaadinCoreJson.get("core").has("button"));
        Assert.assertFalse(mockedVaadinCoreJson.has("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toString(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);

        ObjectNode pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.has("@vaadin/button"));
        Assert.assertFalse(pinnedVersions.has("@vaadin/grid-pro"));
        Assert.assertFalse(pinnedVersions.has("@vaadin/vaadin-grid-pro"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_reactNotAvailable_noReactComponents()
            throws IOException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        ObjectNode mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();

        ObjectNode reactComponents = JacksonUtils.createObjectNode();
        ObjectNode reactData = JacksonUtils.createObjectNode();
        reactData.put("jsVersion", "24.4.0-alpha13");
        reactData.put("npmName", "@vaadin/react-components");

        reactComponents.set("react-components", reactData);

        mockedVaadinCoreJson.set("react", reactComponents);

        Assert.assertTrue(mockedVaadinCoreJson.has("core"));
        Assert.assertTrue(mockedVaadinCoreJson.get("core").has("button"));
        Assert.assertFalse(mockedVaadinCoreJson.has("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toString(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);

        ObjectNode pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.has("@vaadin/button"));
        Assert.assertFalse(pinnedVersions.has("react-components"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_reactAvailable_containsReactComponents()
            throws IOException, ClassNotFoundException {
        generateTestDataForReactComponents();

        ObjectNode pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.has("@vaadin/button"));
        Assert.assertTrue(pinnedVersions.has("@vaadin/react-components"));
        Assert.assertTrue(pinnedVersions.has("@vaadin/react-components-pro"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_reactAvailable_excludeWebComponents()
            throws IOException, ClassNotFoundException {
        options.withNpmExcludeWebComponents(true);
        generateTestDataForReactComponents();

        ObjectNode pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        // @vaadin/button doesn't have 'mode' set, so it should be included
        Assert.assertTrue(pinnedVersions.has("@vaadin/button"));
        Assert.assertFalse(pinnedVersions.has("@vaadin/react-components"));
        Assert.assertFalse(pinnedVersions.has("@vaadin/react-components-pro"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_reactDisabled_excludeWebComponents()
            throws IOException, ClassNotFoundException {
        options.withReact(false);
        options.withNpmExcludeWebComponents(true);
        generateTestDataForReactComponents();

        ObjectNode pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        // @vaadin/button doesn't have 'mode' set, so it should be included
        Assert.assertTrue(pinnedVersions.has("@vaadin/button"));
        Assert.assertFalse(pinnedVersions.has("@vaadin/react-components"));
        Assert.assertFalse(pinnedVersions.has("@vaadin/react-components-pro"));
    }

    private void generateTestDataForReactComponents()
            throws IOException, ClassNotFoundException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        File vaadinVersionsFile = File.createTempFile("vaadin-versions",
                ".json", temporaryFolder.newFolder());
        ObjectNode mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();

        ObjectNode reactComponents = JacksonUtils.createObjectNode();
        ObjectNode reactData = JacksonUtils.createObjectNode();
        reactData.put("jsVersion", "24.4.0-alpha13");
        reactData.put("npmName", "@vaadin/react-components");
        reactData.put("mode", "react");

        reactComponents.set("react-components", reactData);

        mockedVaadinCoreJson.set("react", reactComponents);

        Assert.assertTrue(mockedVaadinCoreJson.has("core"));
        Assert.assertTrue(mockedVaadinCoreJson.get("core").has("button"));
        Assert.assertFalse(mockedVaadinCoreJson.has("vaadin"));

        ObjectNode mockedVaadinJson = getMockVaadinVersionsJson();

        reactComponents = JacksonUtils.createObjectNode();
        reactData = JacksonUtils.createObjectNode();
        reactData.put("jsVersion", "24.4.0-alpha13");
        reactData.put("npmName", "@vaadin/react-components-pro");
        reactData.put("mode", "react");

        reactComponents.set("react-components-pro", reactData);

        mockedVaadinJson.set("react", reactComponents);

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toString(),
                StandardCharsets.UTF_8);
        FileUtils.write(vaadinVersionsFile, mockedVaadinJson.toString(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(vaadinVersionsFile.toURI().toURL());
        Class clazz = FeatureFlags.class; // actual class doesn't matter
        Mockito.doReturn(clazz).when(finder).loadClass(
                "com.vaadin.flow.component.react.ReactAdapterComponent");
    }

    @Test
    public void testGetPlatformPinnedDependencies_VaadinAndVaadinCoreVersionsArePresent_outputContainsBothCoreAndCommercialVersions()
            throws IOException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        JsonNode mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();
        Assert.assertTrue(mockedVaadinCoreJson.has("core"));
        Assert.assertTrue(mockedVaadinCoreJson.get("core").has("button"));
        Assert.assertFalse(mockedVaadinCoreJson.has("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toString(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());

        File vaadinVersionsFile = File.createTempFile("vaadin-versions",
                ".json", temporaryFolder.newFolder());
        JsonNode mockedVaadinJson = getMockVaadinVersionsJson();
        Assert.assertFalse(mockedVaadinJson.has("core"));
        Assert.assertTrue(mockedVaadinJson.has("vaadin"));
        Assert.assertTrue(mockedVaadinJson.get("vaadin").has("grid-pro"));
        Assert.assertTrue(
                mockedVaadinJson.get("vaadin").has("vaadin-grid-pro"));

        FileUtils.write(vaadinVersionsFile, mockedVaadinJson.toString(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(vaadinVersionsFile.toURI().toURL());

        ObjectNode pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.has("@vaadin/button"));
        Assert.assertTrue(pinnedVersions.has("@vaadin/grid-pro"));
        Assert.assertTrue(pinnedVersions.has("@vaadin/vaadin-grid-pro"));
    }

    @Test
    public void getDefaultDependencies_reactIsUsed_addsHillaReactComponents() {
        boolean reactEnabled = options.isReactEnabled();
        try (MockedStatic<FrontendUtils> mock = Mockito
                .mockStatic(FrontendUtils.class)) {
            mock.when(() -> FrontendUtils.isHillaUsed(Mockito.any(File.class),
                    Mockito.any(ClassFinder.class))).thenReturn(true);
            mock.when(() -> FrontendUtils
                    .isReactRouterRequired(Mockito.any(File.class)))
                    .thenReturn(true);
            options.withReact(true);
            Map<String, String> defaultDeps = nodeUpdater
                    .getDefaultDependencies();
            Assert.assertFalse(
                    "Lit component added unexpectedly for react-router",
                    defaultDeps.containsKey("@vaadin/hilla-lit-form"));
            Assert.assertTrue(
                    "React component should be added when react-router is used",
                    defaultDeps.containsKey("@vaadin/hilla-react-auth"));
            Assert.assertTrue(
                    defaultDeps.containsKey("@vaadin/hilla-react-crud"));
            Assert.assertTrue(
                    defaultDeps.containsKey("@vaadin/hilla-react-form"));

            Map<String, String> defaultDevDeps = nodeUpdater
                    .getDefaultDevDependencies();
            Assert.assertFalse(
                    "Lit dev dependency added unexpectedly for react-router",
                    defaultDevDeps.containsKey("lit-dev-dependency"));
            Assert.assertTrue(
                    "React dev dependency should be added when react-router is used",
                    defaultDevDeps.containsKey("react-dev-dependency"));
        } finally {
            options.withReact(reactEnabled);
        }
    }

    @Test
    public void getDefaultDependencies_vaadinRouterIsUsed_addsHillaLitComponents() {
        boolean reactEnabled = options.isReactEnabled();
        try (MockedStatic<FrontendUtils> mock = Mockito
                .mockStatic(FrontendUtils.class)) {
            mock.when(() -> FrontendUtils.isHillaUsed(Mockito.any(File.class),
                    Mockito.any(ClassFinder.class))).thenReturn(true);
            options.withReact(false);
            Map<String, String> defaultDeps = nodeUpdater
                    .getDefaultDependencies();
            Assert.assertTrue(
                    "Lit component should be when vaadin-router is used",
                    defaultDeps.containsKey("@vaadin/hilla-lit-form"));
            Assert.assertFalse(
                    "React component added unexpectedly for vaadin-router",
                    defaultDeps.containsKey("@vaadin/hilla-react-form"));

            Map<String, String> defaultDevDeps = nodeUpdater
                    .getDefaultDevDependencies();
            Assert.assertFalse(
                    "React dev dependency added unexpectedly for vaadin-router",
                    defaultDevDeps.containsKey("react-dev-dependency"));
            Assert.assertTrue(
                    "Lit dev dependency should be added when vaadin-router is used",
                    defaultDevDeps.containsKey("lit-dev-dependency"));
        } finally {
            options.withReact(reactEnabled);
        }
    }

    @Test
    public void getDefaultDependencies_hillaIsNotUsed_doesntAddHillaComponents() {
        Map<String, String> defaultDeps = nodeUpdater.getDefaultDependencies();
        Assert.assertFalse(
                "Lit component added unexpectedly when Hilla isn't used",
                defaultDeps.containsKey("@vaadin/hilla-lit-form"));
        Assert.assertFalse(
                "React component added unexpectedly when Hilla isn't used",
                defaultDeps.containsKey("@vaadin/hilla-react-auth"));

        Map<String, String> defaultDevDeps = nodeUpdater
                .getDefaultDevDependencies();
        Assert.assertFalse(
                "React dev dependency added unexpectedly when Hilla isn't used",
                defaultDevDeps.containsKey("react-dev-dependency"));
        Assert.assertFalse(
                "Lit dev dependency added unexpectedly when Hilla isn't used",
                defaultDevDeps.containsKey("lit-dev-dependency"));
    }

    @Test
    public void readPackageJson_nonExistingFile_doesNotThrow()
            throws IOException {
        nodeUpdater.readPackageJson("non-existing-folder");
    }

    @Test
    public void readPackageJson_nonExistingFile_jsonContainsDepsAndDevDeps()
            throws IOException {
        JsonNode jsonObject = nodeUpdater
                .readPackageJson("non-existing-folder");
        Assert.assertTrue(jsonObject.has("dependencies"));
        Assert.assertTrue(jsonObject.has("devDependencies"));
    }

    @Test
    public void readDependencies_doesntHaveDependencies_doesNotThrow() {
        nodeUpdater.readDependencies("no-deps", "dependencies");
        nodeUpdater.readDependencies("no-deps", "devDependencies");
    }

    @Test
    public void readPackageJsonIfAvailable_nonExistingFile_noErrorLog() {
        Logger log = Mockito.mock(Logger.class);
        nodeUpdater = new NodeUpdater(Mockito.mock(FrontendDependencies.class),
                options) {

            @Override
            public void execute() {
                // NO-OP
            }

            @Override
            Logger log() {
                return log;
            }
        };
        nodeUpdater.readDependenciesIfAvailable("non-existing-folder",
                "dependencies");
        Mockito.verifyNoInteractions(log);

        nodeUpdater.readDependenciesIfAvailable("non-existing-folder",
                "devDpendencies");
        Mockito.verifyNoInteractions(log);
    }

    private String getPolymerVersion(JsonNode object) {
        JsonNode deps = object.get("dependencies");
        return deps.get("@polymer/polymer").textValue();
    }

    private ObjectNode getMockVaadinCoreVersionsJson() {
        // @formatter:off
        return (ObjectNode) JacksonUtils.readTree(
                """
                {
                    "bundles": {
                        "vaadin": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/bundles"
                        }
                    },
                    "core": {
                        "accordion": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/accordion"
                        },
                        "app-layout": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/app-layout"
                        },
                        "avatar": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/avatar"
                        },
                        "avatar-group": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/avatar-group"
                        },
                        "button": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/button"
                        },
                        "checkbox": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/checkbox"
                        }
                    },
                    "platform": "23.2.0"
                }
                """
        );
        // @formatter:on
    }

    private ObjectNode getMockVaadinVersionsJson() {
        // @formatter:off
        return (ObjectNode) JacksonUtils.readTree(
                """
                {
                    "vaadin": {
                        "board": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/board"
                        },
                        "charts": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/charts"
                        },
                        "grid-pro": {
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/grid-pro"
                        },
                        "vaadin-board": {
                            "component": true,
                            "javaVersion": "23.2.0",
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/vaadin-board",
                            "pro": true
                        },
                        "vaadin-charts": {
                            "component": true,
                            "javaVersion": "23.2.0",
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/vaadin-charts",
                            "pro": true
                        },
                        "vaadin-grid-pro": {
                            "component": true,
                            "javaVersion": "23.2.0",
                            "jsVersion": "23.2.0",
                            "npmName": "@vaadin/vaadin-grid-pro",
                            "pro": true
                        }
                    },
                    "platform": "23.2.0"
                }
                """
        );
        // @formatter:on
    }
}
