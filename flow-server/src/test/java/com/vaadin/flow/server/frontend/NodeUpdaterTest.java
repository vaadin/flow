/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.tests.util.MockOptions;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NodeUpdaterTest {

    private static final String POLYMER_VERSION = "3.5.1";

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
        // expectedDependencies.add("@vaadin/router");
        expectedDependencies.add("construct-style-sheets-polyfill");
        expectedDependencies.add("lit");
        expectedDependencies.add("react");
        expectedDependencies.add("react-dom");
        expectedDependencies.add("react-router-dom");

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
        expectedDependencies.add("@vaadin/hilla-file-router");

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
        JsonObject object = Json.createObject();
        nodeUpdater.addVaadinDefaultsToJson(object);
        nodeUpdater.updateDefaultDependencies(object);

        String version = getPolymerVersion(object);
        Assert.assertEquals(POLYMER_VERSION, version);
    }

    @Test
    public void updateMainDefaultDependencies_polymerVersionIsProvidedByUser_useProvided() {
        JsonObject object = Json.createObject();
        JsonObject dependencies = Json.createObject();
        dependencies.put("@polymer/polymer", "4.0.0");
        object.put(NodeUpdater.DEPENDENCIES, dependencies);
        nodeUpdater.addVaadinDefaultsToJson(object);

        nodeUpdater.updateDefaultDependencies(object);

        String version = getPolymerVersion(object);
        Assert.assertEquals("4.0.0", version);
    }

    @Test
    public void updateMainDefaultDependencies_vaadinIsProvidedByUser_useDefault() {
        JsonObject object = Json.createObject();

        JsonObject vaadin = Json.createObject();
        vaadin.put("disableUsageStatistics", true);
        object.put(NodeUpdater.VAADIN_DEP_KEY, vaadin);

        nodeUpdater.addVaadinDefaultsToJson(object);
        nodeUpdater.updateDefaultDependencies(object);

        Assert.assertEquals(POLYMER_VERSION, getPolymerVersion(object));
        Assert.assertEquals(POLYMER_VERSION, getPolymerVersion(
                object.getObject(NodeUpdater.VAADIN_DEP_KEY)));
    }

    @Test
    public void updateDefaultDependencies_olderVersionsAreUpdated()
            throws IOException {
        JsonObject packageJson = nodeUpdater.getPackageJson();
        packageJson.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.DEV_DEPENDENCIES, Json.createObject());
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("glob",
                "7.0.0");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("10.3.10", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("glob"));
    }

    @Test // #6907 test when user has set newer versions
    public void updateDefaultDependencies_newerVersionsAreNotChanged()
            throws IOException {
        JsonObject packageJson = nodeUpdater.getPackageJson();
        packageJson.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.DEV_DEPENDENCIES, Json.createObject());
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("vite",
                "78.2.3");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("78.2.3", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("vite"));
    }

    @Test
    public void shouldUpdateExistingLocalFormPackageToNpmPackage()
            throws IOException {
        JsonObject packageJson = Json.createObject();
        JsonObject dependencies = Json.createObject();
        packageJson.put(NodeUpdater.DEPENDENCIES, dependencies);
        JsonObject vaadinDependencies = Json.createObject();
        vaadinDependencies.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.VAADIN_DEP_KEY, vaadinDependencies);

        String formPackage = "@vaadin/form";
        String legecyVersion = "./target/flow-frontend/form";
        String newVersion = "22.0.0";

        dependencies.put(formPackage, legecyVersion);

        nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES,
                formPackage, newVersion);

        Assert.assertEquals(newVersion, packageJson
                .getObject(NodeUpdater.DEPENDENCIES).getString(formPackage));
    }

    @Test
    public void shouldSkipUpdatingNonParsableVersions() throws IOException {
        JsonObject packageJson = Json.createObject();
        JsonObject dependencies = Json.createObject();
        packageJson.put(NodeUpdater.DEPENDENCIES, dependencies);
        JsonObject vaadinDependencies = Json.createObject();
        vaadinDependencies.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.VAADIN_DEP_KEY, vaadinDependencies);

        String formPackage = "@vaadin/form";
        String existingVersion = "../../../some/local/path";
        String newVersion = "2.0.0";

        dependencies.put(formPackage, existingVersion);

        nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES,
                formPackage, newVersion);

        Assert.assertEquals(existingVersion, packageJson
                .getObject(NodeUpdater.DEPENDENCIES).getString(formPackage));
    }

    @Test
    public void canUpdateNonParseableVersions() throws IOException {
        JsonObject packageJson = Json.createObject();
        JsonObject dependencies = Json.createObject();
        packageJson.put(NodeUpdater.DEPENDENCIES, dependencies);
        JsonObject vaadinDependencies = Json.createObject();
        vaadinDependencies.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.VAADIN_DEP_KEY, vaadinDependencies);

        String pkg = "mypackage";
        String existingVersion = "./some/path";

        dependencies.put(pkg, existingVersion);

        nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES, pkg,
                existingVersion);

        Assert.assertEquals(existingVersion,
                packageJson.getObject(NodeUpdater.DEPENDENCIES).getString(pkg));

    }

    @Test
    public void getJsonFileContent_incorrectPackageJsonContent_throwsExceptionWithFileName()
            throws IOException {
        File brokenPackageJsonFile = temporaryFolder
                .newFile("broken-package.json");
        FileUtils.writeStringToFile(brokenPackageJsonFile,
                "{ some broken json ", UTF_8);

        JsonException exception = Assert.assertThrows(JsonException.class,
                () -> NodeUpdater.getJsonFileContent(brokenPackageJsonFile));

        MatcherAssert.assertThat(exception.getMessage(),
                StringContains.containsString("Cannot parse package file "));
        MatcherAssert.assertThat(exception.getMessage(),
                StringContains.containsString("broken-package.json"));
    }

    @Test
    public void removedAllOldAndExistingPlugins() throws IOException {
        File packageJson = new File(npmFolder, "package.json");
        FileWriter packageJsonWriter = new FileWriter(packageJson);
        packageJsonWriter.write("{\"devDependencies\": {"
                + "\"@vaadin/some-old-plugin\": \"./target/plugins/some-old-plugin\","
                + "\"@vaadin/application-theme-plugin\": \"./target/plugins/application-theme-plugin\"}"
                + "}");
        packageJsonWriter.close();
        JsonObject actualDevDeps = nodeUpdater.getPackageJson()
                .getObject(NodeUpdater.DEV_DEPENDENCIES);
        Assert.assertFalse(actualDevDeps.hasKey("some-old-plugin"));
        Assert.assertFalse(
                actualDevDeps.hasKey("@vaadin/application-theme-plugin"));
    }

    @Test
    public void generateVersionsJson_noVersions_noDevDeps_versionsGeneratedFromPackageJson()
            throws IOException {
        nodeUpdater.generateVersionsJson(Json.createObject());
        Assert.assertEquals("{}", nodeUpdater.versionsJson.toJson());
    }

    @Test
    public void generateVersionsJson_versionsGeneratedFromPackageJson_containsBothDepsAndDevDeps()
            throws IOException {

        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        // @formatter:off
        FileUtils.write(packageJson,
            "{"
                + "\"vaadin\": {"
                  + "\"dependencies\": {"
                    + "\"lit\": \"2.0.0\","
                    + "\"@vaadin/router\": \"1.7.5\","
                    + "\"@polymer/polymer\": \"3.4.1\","
                  + "},"
                  + "\"devDependencies\": {"
                    + "\"css-loader\": \"4.2.1\","
                    + "\"file-loader\": \"6.1.0\""
                  + "}"
                + "},"
                + "\"dependencies\": {"
                  + "\"lit\": \"2.0.0\","
                  + "\"@vaadin/router\": \"1.7.5\","
                  + "\"@polymer/polymer\": \"3.4.1\","
                + "},"
                + "\"devDependencies\": {"
                  + "\"css-loader\": \"4.2.1\","
                  + "\"file-loader\": \"6.1.0\""
                + "}"
            + "}", StandardCharsets.UTF_8);
        // @formatter:on

        nodeUpdater.generateVersionsJson(Json.parse(FileUtils
                .readFileToString(packageJson, StandardCharsets.UTF_8)));
        Assert.assertEquals(
                "{" + "\"lit\":\"2.0.0\"," + "\"@vaadin/router\":\"1.7.5\","
                        + "\"@polymer/polymer\":\"3.4.1\"" + "}",
                nodeUpdater.versionsJson.toJson());
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

            JsonObject pinnedVersions = nodeUpdater
                    .getPlatformPinnedDependencies();
            Assert.assertEquals(0, pinnedVersions.keys().length);

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
        JsonObject mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();
        Assert.assertTrue(mockedVaadinCoreJson.hasKey("core"));
        Assert.assertTrue(
                mockedVaadinCoreJson.getObject("core").hasKey("button"));
        Assert.assertFalse(mockedVaadinCoreJson.hasKey("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toJson(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);

        JsonObject pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/button"));
        Assert.assertFalse(pinnedVersions.hasKey("@vaadin/grid-pro"));
        Assert.assertFalse(pinnedVersions.hasKey("@vaadin/vaadin-grid-pro"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_reactNotAvailable_noReactComponents()
            throws IOException, ClassNotFoundException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        JsonObject mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();

        JsonObject reactComponents = Json.createObject();
        JsonObject reactData = Json.createObject();
        reactData.put("jsVersion", "24.4.0-alpha13");
        reactData.put("npmName", "@vaadin/react-components");

        reactComponents.put("react-components", reactData);

        mockedVaadinCoreJson.put("react", reactComponents);

        Assert.assertTrue(mockedVaadinCoreJson.hasKey("core"));
        Assert.assertTrue(
                mockedVaadinCoreJson.getObject("core").hasKey("button"));
        Assert.assertFalse(mockedVaadinCoreJson.hasKey("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toJson(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);

        JsonObject pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/button"));
        Assert.assertFalse(pinnedVersions.hasKey("react-components"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_reactAvailable_containsReactComponents()
            throws IOException, ClassNotFoundException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        JsonObject mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();

        JsonObject reactComponents = Json.createObject();
        JsonObject reactData = Json.createObject();
        reactData.put("jsVersion", "24.4.0-alpha13");
        reactData.put("npmName", "@vaadin/react-components");

        reactComponents.put("react-components", reactData);

        mockedVaadinCoreJson.put("react", reactComponents);

        Assert.assertTrue(mockedVaadinCoreJson.hasKey("core"));
        Assert.assertTrue(
                mockedVaadinCoreJson.getObject("core").hasKey("button"));
        Assert.assertFalse(mockedVaadinCoreJson.hasKey("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toJson(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);
        Class clazz = FeatureFlags.class; // actual class doesn't matter
        Mockito.doReturn(clazz).when(finder).loadClass(
                "com.vaadin.flow.component.react.ReactAdapterComponent");

        JsonObject pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/button"));
        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/react-components"));
    }

    @Test
    public void testGetPlatformPinnedDependencies_VaadinAndVaadinCoreVersionsArePresent_outputContainsBothCoreAndCommercialVersions()
            throws IOException {
        File coreVersionsFile = File.createTempFile("vaadin-core-versions",
                ".json", temporaryFolder.newFolder());
        JsonObject mockedVaadinCoreJson = getMockVaadinCoreVersionsJson();
        Assert.assertTrue(mockedVaadinCoreJson.hasKey("core"));
        Assert.assertTrue(
                mockedVaadinCoreJson.getObject("core").hasKey("button"));
        Assert.assertFalse(mockedVaadinCoreJson.hasKey("vaadin"));

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toJson(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(coreVersionsFile.toURI().toURL());

        File vaadinVersionsFile = File.createTempFile("vaadin-versions",
                ".json", temporaryFolder.newFolder());
        JsonObject mockedVaadinJson = getMockVaadinVersionsJson();
        Assert.assertFalse(mockedVaadinJson.hasKey("core"));
        Assert.assertTrue(mockedVaadinJson.hasKey("vaadin"));
        Assert.assertTrue(
                mockedVaadinJson.getObject("vaadin").hasKey("grid-pro"));
        Assert.assertTrue(
                mockedVaadinJson.getObject("vaadin").hasKey("vaadin-grid-pro"));

        FileUtils.write(vaadinVersionsFile, mockedVaadinJson.toJson(),
                StandardCharsets.UTF_8);
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(vaadinVersionsFile.toURI().toURL());

        JsonObject pinnedVersions = nodeUpdater.getPlatformPinnedDependencies();

        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/button"));
        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/grid-pro"));
        Assert.assertTrue(pinnedVersions.hasKey("@vaadin/vaadin-grid-pro"));
    }

    @Test
    public void getDefaultDependencies_reactIsUsed_addsHillaReactComponents() {
        boolean reactEnabled = options.isReactEnabled();
        try (MockedStatic<FrontendUtils> mock = Mockito
                .mockStatic(FrontendUtils.class)) {
            mock.when(() -> FrontendUtils.isHillaUsed(Mockito.any(File.class),
                    Mockito.any(ClassFinder.class))).thenReturn(true);
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
        JsonObject jsonObject = nodeUpdater
                .readPackageJson("non-existing-folder");
        Assert.assertTrue(jsonObject.hasKey("dependencies"));
        Assert.assertTrue(jsonObject.hasKey("devDependencies"));
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

    private String getPolymerVersion(JsonObject object) {
        JsonObject deps = object.get("dependencies");
        String version = deps.getString("@polymer/polymer");
        return version;
    }

    private JsonObject getMockVaadinCoreVersionsJson() {
        // @formatter:off
        return Json.parse(
                "{\n" +
                        "    \"bundles\": {\n" +
                        "        \"vaadin\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/bundles\"\n" +
                        "        }\n" +
                        "    },\n" +
                        "    \"core\": {\n" +
                        "        \"accordion\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/accordion\"\n" +
                        "        },\n" +
                        "        \"app-layout\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/app-layout\"\n" +
                        "        },\n" +
                        "        \"avatar\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/avatar\"\n" +
                        "        },\n" +
                        "        \"avatar-group\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/avatar-group\"\n" +
                        "        },\n" +
                        "        \"button\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/button\"\n" +
                        "        },\n" +
                        "        \"checkbox\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/checkbox\"\n" +
                        "        }" +
                        "    },\n" +
                        "    \"platform\": \"23.2.0\"\n" +
                        "}"
        );
        // @formatter:on
    }

    private JsonObject getMockVaadinVersionsJson() {
        // @formatter:off
        return Json.parse(
                "{\n" +
                        "    \"vaadin\": {\n" +
                        "        \"board\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/board\"\n" +
                        "        },\n" +
                        "        \"charts\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/charts\"\n" +
                        "        },\n" +
                        "        \"grid-pro\": {\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/grid-pro\"\n" +
                        "        },\n" +
                        "        \"vaadin-board\": {\n" +
                        "            \"component\": true,\n" +
                        "            \"javaVersion\": \"23.2.0\",\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/vaadin-board\",\n" +
                        "            \"pro\": true\n" +
                        "        },\n" +
                        "        \"vaadin-charts\": {\n" +
                        "            \"component\": true,\n" +
                        "            \"javaVersion\": \"23.2.0\",\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/vaadin-charts\",\n" +
                        "            \"pro\": true\n" +
                        "        },\n" +
                        "        \"vaadin-grid-pro\": {\n" +
                        "            \"component\": true,\n" +
                        "            \"javaVersion\": \"23.2.0\",\n" +
                        "            \"jsVersion\": \"23.2.0\",\n" +
                        "            \"npmName\": \"@vaadin/vaadin-grid-pro\",\n" +
                        "            \"pro\": true\n" +
                        "        },\n" +
                        "    },\n" +
                        "    \"platform\": \"23.2.0\"\n" +
                        "}"
        );
        // @formatter:on
    }
}
