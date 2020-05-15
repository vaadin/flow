/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;

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
                new File(""), resourceFolder) {

            @Override
            public void execute() {
            }

        };
    }

    @After
    public void tearDown() throws IOException {
        npmFolder.delete();
        resourceFolder.delete();
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

        Assert.assertEquals("4.42.0", packageJson
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
    public void assertTerserVersion() throws IOException {
        final JsonObject packageJson = nodeUpdater.getPackageJson();
        nodeUpdater.updateDefaultDependencies(packageJson);
        Assert.assertEquals("4.6.7", packageJson
                .getObject(NodeUpdater.DEV_DEPENDENCIES).getString("terser"));
    }

    @Test
    public void assertFormResourcesPackageJson() throws IOException {
        JsonObject formPackageJson = nodeUpdater.getFormResourcesPackageJson();
        Assert.assertEquals("@vaadin/form", formPackageJson
                .getString("name"));
        Assert.assertEquals("UNLICENSED", formPackageJson
                .getString("license"));
        Assert.assertEquals("Form", formPackageJson
                .getString("main"));
        Assert.assertEquals("1.0.0", formPackageJson
                .getString("version"));
    }

    @Test
    public void assertWriteFormResourcesPackageFile() throws IOException {
        File formPackageJsonFile = new File(nodeUpdater.formResourcesFolder, Constants.PACKAGE_JSON);
        Assert.assertFalse(formPackageJsonFile.exists());

        JsonObject formPackageJson = Json.createObject();
        formPackageJson.put("foo", "bar");

        nodeUpdater.writeFormResourcesPackageFile(formPackageJson);

        Assert.assertTrue(formPackageJsonFile.exists());
        JsonObject packageJson = NodeUpdater.getJsonFileContent(
                formPackageJsonFile);
        Assert.assertEquals("bar", packageJson.getString("foo"));
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
