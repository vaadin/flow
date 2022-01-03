/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NodeUpdaterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private File npmFolder;

    private File resourceFolder;

    private ClassFinder finder;

    private URL url;

    @Before
    public void setUp() throws IOException {
        url = new URL("file://bar");
        npmFolder = temporaryFolder.newFolder();
        resourceFolder = temporaryFolder.newFolder();
        finder = Mockito.mock(ClassFinder.class);
        nodeUpdater = new NodeUpdater(finder,
                Mockito.mock(FrontendDependencies.class), npmFolder,
                new File(""), resourceFolder, TARGET,
                Mockito.mock(FeatureFlags.class)) {

            @Override
            public void execute() {
            }

        };
    }

    @Test
    public void resolveResource_startsWithAt_returnsPassedArg() {
        Assert.assertEquals("@foo", nodeUpdater.resolveResource("@foo"));
        Assert.assertEquals("@foo", nodeUpdater.resolveResource("@foo"));
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
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("webpack",
                "3.3.10");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("4.46.0", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("webpack"));
    }

    @Test // #6907 test when user has set newer versions
    public void updateDefaultDependencies_newerVersionsAreNotChanged()
            throws IOException {
        JsonObject packageJson = nodeUpdater.getPackageJson();
        packageJson.put(NodeUpdater.DEPENDENCIES, Json.createObject());
        packageJson.put(NodeUpdater.DEV_DEPENDENCIES, Json.createObject());
        packageJson.getObject(NodeUpdater.DEV_DEPENDENCIES).put("webpack",
                "5.0.1");
        nodeUpdater.updateDefaultDependencies(packageJson);

        Assert.assertEquals("5.0.1", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("webpack"));
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
    public void removedDisusedPlugins() throws IOException {
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
        Assert.assertTrue(
                actualDevDeps.hasKey("@vaadin/application-theme-plugin"));
    }

    @Test
    public void generateVersionsJson_noVersions_noDevDeps_versionsGeneratedFromPackageJson()
            throws IOException {
        final String versions = nodeUpdater.generateVersionsJson();
        Assert.assertNotNull(versions);

        File generatedVersionsFile = new File(npmFolder,
                versions);
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
                    + "\"@vaadin/router\": \"1.7.4\","
                    + "\"@polymer/polymer\": \"3.2.0\","
                  + "},"
                  + "\"devDependencies\": {"
                    + "\"css-loader\": \"4.2.1\","
                    + "\"file-loader\": \"6.1.0\""
                  + "}"
                + "},"
                + "\"dependencies\": {"
                  + "\"lit\": \"2.0.0\","
                  + "\"@vaadin/router\": \"1.7.4\","
                  + "\"@polymer/polymer\": \"3.2.0\","
                + "},"
                + "\"devDependencies\": {"
                  + "\"css-loader\": \"4.2.1\","
                  + "\"file-loader\": \"6.1.0\""
                + "}"
            + "}", StandardCharsets.UTF_8);
        // @formatter:on

        final String versions = nodeUpdater.generateVersionsJson();
        Assert.assertNotNull(versions);

        File generatedVersionsFile = new File(npmFolder,
                versions);
        final JsonObject versionsJson = Json.parse(FileUtils.readFileToString(
                generatedVersionsFile, StandardCharsets.UTF_8));
        Assert.assertEquals(
                "{" + "\"lit\":\"2.0.0\"," + "\"@vaadin/router\":\"1.7.4\","
                        + "\"@polymer/polymer\":\"3.2.0\","
                        + "\"css-loader\":\"4.2.1\","
                        + "\"file-loader\":\"6.1.0\"" + "}",
                versionsJson.toJson());
    }

    private String getPolymerVersion(JsonObject object) {
        JsonObject deps = object.get("dependencies");
        String version = deps.getString("@polymer/polymer");
        return version;
    }

    private void resolveResource_happyPath(String resourceFolder) {
        Mockito.when(finder.getResource(resourceFolder + "/foo"))
                .thenReturn(url);
        Assert.assertEquals(FLOW_NPM_PACKAGE_NAME + "foo",
                nodeUpdater.resolveResource("foo"));
        Assert.assertEquals(FLOW_NPM_PACKAGE_NAME + "foo",
                nodeUpdater.resolveResource("foo"));
    }

    private void resolveResource_unhappyPath(String resourceFolder) {
        Mockito.when(finder.getResource(resourceFolder + "/foo"))
                .thenReturn(null);
        Assert.assertEquals("foo", nodeUpdater.resolveResource("foo"));
        Assert.assertEquals("foo", nodeUpdater.resolveResource("foo"));
    }
}
