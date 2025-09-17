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
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.testutil.TestUtils;

import elemental.json.JsonObject;

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
    public void should_createPackageJson() throws IOException {
        TaskGeneratePackageJson task = new TaskGeneratePackageJson(npmFolder,
                generatedFolder, TARGET, Mockito.mock(FeatureFlags.class));
        task.execute();
        Assert.assertTrue(new File(npmFolder, PACKAGE_JSON).exists());
        Assert.assertFalse(new File(generatedFolder, PACKAGE_JSON).exists());
        JsonObject deps = task.getPackageJson().getObject("dependencies");
        Assert.assertFalse(deps.hasKey(NodeUpdater.DEP_NAME_FLOW_DEPS));
        Assert.assertFalse(deps.hasKey(NodeUpdater.DEP_NAME_FLOW_JARS));
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

        TaskCopyFrontendFiles task = new TaskCopyFrontendFiles(
                frontendDepsFolder, jars(jar, dir));

        task.execute();

        List<String> files = TestUtils.listFilesRecursively(frontendDepsFolder);
        Assert.assertEquals(19, files.size());

        // Check some resources
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

        Assert.assertTrue("HTML resource should have been copied from jar file",
                files.contains("ExampleTemplate.html"));

        Assert.assertFalse(
                "Resource from unsupported frontend folder location should not have been copied from jar file",
                files.contains("ignored.js"));

    }

    private static Set<File> jars(File... files) {
        return Stream.of(files).collect(Collectors.toSet());
    }
}
