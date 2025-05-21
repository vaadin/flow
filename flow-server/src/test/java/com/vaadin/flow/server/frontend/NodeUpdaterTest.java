/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NodeUpdaterTest {

    private static final String POLYMER_VERSION = "3.5.2";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private File npmFolder;

    private File generatedPath;

    private ClassFinder finder;

    private URL url;

    private boolean useWebpack = false;

    @Before
    public void setUp() throws IOException {
        url = new URL("file://bar");
        npmFolder = temporaryFolder.newFolder();
        generatedPath = temporaryFolder.newFolder();
        FeatureFlags featureFlags = Mockito.mock(FeatureFlags.class);
        Mockito.when(featureFlags.isEnabled(Mockito.any(Feature.class)))
                .thenAnswer((query) -> {
                    if (query.getArgument(0).equals(FeatureFlags.WEBPACK)) {
                        return useWebpack;
                    }
                    return false;
                });
        finder = Mockito.mock(ClassFinder.class);
        nodeUpdater = new NodeUpdater(finder,
                Mockito.mock(FrontendDependencies.class), npmFolder,
                generatedPath, TARGET, featureFlags) {

            @Override
            public void execute() {
            }

        };
    }

    @Test
    public void getGeneratedModules_should_excludeByFileName()
            throws IOException {
        File generated = temporaryFolder.newFolder();
        File fileA = new File(generated, "a.js");
        File fileB = new File(generated, "b.js");
        File fileC = new File(generated, "c.js");
        fileA.createNewFile();
        fileB.createNewFile();
        fileC.createNewFile();

        Set<String> modules = NodeUpdater.getGeneratedModules(generated,
                Stream.of("a.js", "/b.js").collect(Collectors.toSet()));

        Assert.assertEquals(1, modules.size());
        // GENERATED/ is an added prefix for files from this method
        Assert.assertTrue(modules.contains("GENERATED/c.js"));
    }

    @Test
    public void getDefaultDependencies_includesAllDependencies() {
        Map<String, String> defaultDeps = nodeUpdater.getDefaultDependencies();
        Set<String> expectedDependencies = new HashSet<>();
        expectedDependencies.add("@polymer/polymer");
        expectedDependencies.add("@vaadin/common-frontend");
        expectedDependencies.add("@vaadin/router");
        expectedDependencies.add("construct-style-sheets-polyfill");
        expectedDependencies.add("lit");

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
        expectedDependencies.add("rollup-plugin-brotli");
        expectedDependencies.add("@rollup/plugin-replace");
        expectedDependencies.add("@rollup/pluginutils");
        expectedDependencies.add("vite-plugin-checker");
        expectedDependencies.add("mkdirp");
        expectedDependencies.add("workbox-build");
        expectedDependencies.add("transform-ast");
        expectedDependencies.add("strip-css-comments");

        Set<String> actualDependendencies = defaultDeps.keySet();

        Assert.assertEquals(expectedDependencies, actualDependendencies);
    }

    @Test
    public void getDefaultDevDependencies_includesAllDependencies_whenUsingWebpack() {
        useWebpack = true;
        Map<String, String> defaultDeps = nodeUpdater
                .getDefaultDevDependencies();
        Set<String> expectedDependencies = getCommonDevDeps();

        // Webpack
        // Webpack plugins and helpers
        expectedDependencies.add("esbuild-loader");
        expectedDependencies.add("html-webpack-plugin");
        expectedDependencies.add("fork-ts-checker-webpack-plugin");
        expectedDependencies.add("webpack");
        expectedDependencies.add("webpack-cli");
        expectedDependencies.add("webpack-dev-server");
        expectedDependencies.add("compression-webpack-plugin");
        expectedDependencies.add("extra-watch-webpack-plugin");
        expectedDependencies.add("webpack-merge");
        expectedDependencies.add("css-loader");
        expectedDependencies.add("extract-loader-5");
        expectedDependencies.add("lit-css-loader");
        expectedDependencies.add("loader-utils");
        expectedDependencies.add("workbox-webpack-plugin");
        expectedDependencies.add("mkdirp");
        expectedDependencies.add("chokidar");
        expectedDependencies.add("strip-css-comments");

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

        Assert.assertEquals("7.2.3", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("glob"));
    }

    @Test // #6907 test when user has set newer versions
    public void updateDefaultDependencies_newerVersionsAreNotChanged()
            throws IOException {
        JsonObject packageJson = nodeUpdater.getPackageJson();
        packageJson.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.DEV_DEPENDENCIES, Json.createObject());
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("vite",
                "v4.0.0");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("v4.0.0", packageJson
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
        final String versions = nodeUpdater
                .generateVersionsJson(Json.createObject());
        Assert.assertNotNull(versions);

        File generatedVersionsFile = new File(npmFolder, versions);
        final JsonObject versionsJson = Json.parse(FileUtils.readFileToString(
                generatedVersionsFile, StandardCharsets.UTF_8));
        Assert.assertEquals("{}", versionsJson.toJson());
    }

    @Test
    public void generateVersionsJson_versionsGeneratedFromPackageJson_containsBothDepsAndDevDeps()
            throws IOException {

        File packageJson = new File(nodeUpdater.npmFolder, PACKAGE_JSON);
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

        final String versions = nodeUpdater.generateVersionsJson(
                Json.parse(FileUtils.readFileToString(packageJson,
                        StandardCharsets.UTF_8)));
        Assert.assertNotNull(versions);

        File generatedVersionsFile = new File(npmFolder, versions);
        final JsonObject versionsJson = Json.parse(FileUtils.readFileToString(
                generatedVersionsFile, StandardCharsets.UTF_8));
        Assert.assertEquals(
                "{" + "\"lit\":\"2.0.0\"," + "\"@vaadin/router\":\"1.7.5\","
                        + "\"@polymer/polymer\":\"3.4.1\","
                        + "\"css-loader\":\"4.2.1\","
                        + "\"file-loader\":\"6.1.0\"" + "}",
                versionsJson.toJson());
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
                            + " Transitive dependencies won't be pinned for npm/pnpm.",
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
