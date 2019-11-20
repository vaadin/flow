/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;

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
        nodeUpdater.updateMainDefaultDependencies(object, null);

        String version = getPolymerVersion(object);
        Assert.assertEquals("3.2.0", version);
    }

    @Test
    public void updateMainDefaultDependencies_polymerVersionIsProvided_useProvided() {
        JsonObject object = Json.createObject();
        nodeUpdater.updateMainDefaultDependencies(object, "foo");

        String version = getPolymerVersion(object);
        Assert.assertEquals("foo", version);
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
}
