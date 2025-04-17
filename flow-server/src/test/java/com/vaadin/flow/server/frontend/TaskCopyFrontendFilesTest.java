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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;

public class TaskCopyFrontendFilesTest extends NodeUpdateTestUtil {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;
    private File generatedFolder;
    private File frontendDepsFolder;

    @Before
    public void setup() throws IOException {
        // creating non-existing folder to make sure the execute() creates
        // the folder if missing
        npmFolder = new File(temporaryFolder.newFolder(), "child/");
        generatedFolder = new File(npmFolder, "target/frontend");
        frontendDepsFolder = new File(npmFolder, "target/frontend-deps");
    }

    @Test
    public void should_collectJsAndCssFilesFromJars_obsoleteResourceFolder()
            throws IOException {
        should_collectJsAndCssFilesFromJars("jar-with-frontend-resources.jar",
                "dir-with-frontend-resources/");
    }

    @Test
    public void should_collectJsAndCssFilesFromJars_modernResourceFolder()
            throws IOException {
        should_collectJsAndCssFilesFromJars("jar-with-modern-frontend.jar",
                "dir-with-modern-frontend");
    }

    @Test
    public void should_collectJsAndCssFilesFromJars_removeExtraFiles()
            throws IOException {
        File dummy = new File(frontendDepsFolder, "dummy.ts");
        frontendDepsFolder.mkdirs();
        dummy.createNewFile();
        should_collectJsAndCssFilesFromJars("jar-with-modern-frontend.jar",
                "dir-with-modern-frontend");
    }

    @Test
    public void should_createPackageJson() throws IOException {
        Options options = new MockOptions(getClassFinder(), npmFolder)
                .withBuildDirectory(TARGET).withBundleBuild(true);

        TaskGeneratePackageJson task = new TaskGeneratePackageJson(options);
        task.execute();
        Assert.assertTrue(new File(npmFolder, PACKAGE_JSON).exists());
        Assert.assertFalse(new File(generatedFolder, PACKAGE_JSON).exists());
        JsonNode deps = task.getPackageJson().get("dependencies");
        Assert.assertFalse(deps.has(NodeUpdater.DEP_NAME_FLOW_DEPS));
        Assert.assertFalse(deps.has(NodeUpdater.DEP_NAME_FLOW_JARS));
    }

    private void should_collectJsAndCssFilesFromJars(String jarFile,
            String fsDir) throws IOException {

        // contains:
        // - ExampleConnector.js
        // - ExampleConnector.js.map
        // - inline.css
        // - inline.css.map
        // - example.ts
        // - example.ts.map
        File jar = TestUtils.getTestJar(jarFile);
        // Contains:
        // - resourceInFolder.js
        // - resourceInFolder.js.map
        File dir = TestUtils.getTestFolder(fsDir);

        Options options = new MockOptions(null);
        options.withJarFrontendResourcesFolder(frontendDepsFolder)
                .copyResources(jars(jar, dir));
        TaskCopyFrontendFiles task = new TaskCopyFrontendFiles(options);
        GeneratedFilesSupport generatedFileSupport = new GeneratedFilesSupport();
        task.setGeneratedFileSupport(generatedFileSupport);

        task.execute();

        List<String> files = TestUtils.listFilesRecursively(frontendDepsFolder);
        Assert.assertEquals(12, files.size());

        Assert.assertTrue("TS resource should have been copied from jar file",
                files.contains("example.ts"));

        Assert.assertTrue(
                "TS resource source map should have been copied from jar file",
                files.contains("example.ts.map"));

        Assert.assertTrue("JS resource should have been copied from jar file",
                files.contains("ExampleConnector.js"));

        Assert.assertTrue(
                "JS resource source map should have been copied from jar file",
                files.contains("ExampleConnector.js.map"));

        Assert.assertTrue("CSS resource should have been copied from jar file",
                files.contains("inline.css"));

        Assert.assertTrue(
                "CSS resource source map should have been copied from jar file",
                files.contains("inline.css.map"));

        Assert.assertTrue(
                "JS resource should have been copied from resource folder",
                files.contains("resourceInFolder.js"));

        Assert.assertTrue(
                "JS resource source map should have been copied from resource folder",
                files.contains("resourceInFolder.js.map"));

        Assert.assertTrue("TSX resource should have been copied from jar file",
                files.contains("react.tsx"));

        Assert.assertTrue(
                "TSX resource source map should have been copied from jar file",
                files.contains("react.tsx.map"));

        Assert.assertTrue("JSX resource should have been copied from jar file",
                files.contains("test.jsx"));

        Assert.assertTrue(
                "JSX resource source map should have been copied from jar file",
                files.contains("test.jsx.map"));

        Assert.assertEquals("Generated files should have been tracked",
                files.stream()
                        .map(path -> frontendDepsFolder.toPath().resolve(path))
                        .collect(Collectors.toSet()),
                generatedFileSupport.getFiles());
    }

    private static Set<File> jars(File... files) {
        return Stream.of(files).collect(Collectors.toSet());
    }
}
