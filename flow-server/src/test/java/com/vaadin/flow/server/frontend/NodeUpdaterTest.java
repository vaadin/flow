/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NodeUpdaterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private File npmFolder;

    private ClassFinder finder;

    private URL url;

    @Before
    public void setUp() throws IOException {
        url = new URL("file://bar");
        npmFolder = temporaryFolder.newFolder();
        finder = Mockito.mock(ClassFinder.class);
        nodeUpdater = new NodeUpdater(finder,
                Mockito.mock(FrontendDependencies.class), npmFolder,
                new File("")) {

            @Override
            public void execute() {
            }

        };
    }

    @Test
    public void resolveResource_startsWithAt_returnsPassedArg() {
        Assert.assertEquals("@foo", nodeUpdater.resolveResource("@foo", true));
        Assert.assertEquals("@foo", nodeUpdater.resolveResource("@foo", false));
    }

    @Test
    public void resolveResource_hasObsoleteResourcesFolder() {
        resolveResource_happyPath(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void resolveResource_hasModernResourcesFolder() {
        resolveResource_happyPath(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void resolveResource_doesNotHaveObsoleteResourcesFolder() {
        resolveResource_unhappyPath(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void resolveResource_doesNotHaveModernResourcesFolder() {
        resolveResource_unhappyPath(RESOURCES_FRONTEND_DEFAULT);
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
    public void updateMainDefaultDependencies_polymerVersionIsNull_useDefault() {
        JsonObject object = Json.createObject();
        nodeUpdater.addVaadinDefaultsToJson(object);
        nodeUpdater.updateDefaultDependencies(object);

        String version = getPolymerVersion(object);
        Assert.assertEquals("3.2.0", version);
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

        Assert.assertEquals("3.2.0", getPolymerVersion(object));
        Assert.assertEquals("3.2.0", getPolymerVersion(
                object.getObject(NodeUpdater.VAADIN_DEP_KEY)));
    }

    @Test
    public void updateDefaultDependencies_olderVersionsAreUpdated()
            throws IOException {
        JsonObject packageJson = nodeUpdater.getPackageJson();
        packageJson.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.DEV_DEPENDENCIES, Json.createObject());
        packageJson.getObject(NodeUpdater.DEPENDENCIES)
                .put("@webcomponents/webcomponentsjs", "^2.1.1");
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("webpack",
                "3.3.10");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("^2.2.10",
                packageJson.getObject(NodeUpdater.DEPENDENCIES)
                        .getString("@webcomponents/webcomponentsjs"));
        Assert.assertEquals("5.99.6", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("webpack"));
    }

    @Test // #6907 test when user has set newer versions
    public void updateDefaultDependencies_newerVersionsAreNotChanged()
            throws IOException {
        JsonObject packageJson = nodeUpdater.getPackageJson();
        packageJson.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.DEV_DEPENDENCIES, Json.createObject());
        packageJson.getObject(NodeUpdater.DEPENDENCIES)
                .put("@webcomponents/webcomponentsjs", "2.3.1");
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("webpack",
                "5.100.1");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("2.3.1",
                packageJson.getObject(NodeUpdater.DEPENDENCIES)
                        .getString("@webcomponents/webcomponentsjs"));
        Assert.assertEquals("5.100.1", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("webpack"));
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
    public void testGetPlatformPinnedDependencies_vaadinCoreVersionIsNotPresent_outputIsNull()
            throws IOException {
        Logger logger = Mockito.spy(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMocked = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactoryMocked
                    .when(() -> LoggerFactory.getLogger("dev-updater"))
                    .thenReturn(logger);

            Mockito.when(
                    finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                    .thenReturn(null);
            Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                    .thenReturn(null);

            JsonObject pinnedVersions = nodeUpdater
                    .getPlatformPinnedDependencies();
            Assert.assertNull(pinnedVersions);

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

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toJson(), UTF_8);
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

        FileUtils.write(coreVersionsFile, mockedVaadinCoreJson.toJson(), UTF_8);
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

        FileUtils.write(vaadinVersionsFile, mockedVaadinJson.toJson(), UTF_8);
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

    private void resolveResource_happyPath(String resourceFolder) {
        Mockito.when(finder.getResource(resourceFolder + "/foo"))
                .thenReturn(url);
        Assert.assertEquals(FrontendUtils.FLOW_NPM_PACKAGE_NAME + "foo",
                nodeUpdater.resolveResource("foo", true));
        Assert.assertEquals(FrontendUtils.FLOW_NPM_PACKAGE_NAME + "foo",
                nodeUpdater.resolveResource("foo", false));
    }

    private void resolveResource_unhappyPath(String resourceFolder) {
        Mockito.when(finder.getResource(resourceFolder + "/foo"))
                .thenReturn(null);
        Assert.assertEquals("foo", nodeUpdater.resolveResource("foo", true));
        Assert.assertEquals("foo", nodeUpdater.resolveResource("foo", false));
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
