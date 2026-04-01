/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCopyFrontendFilesTest extends NodeUpdateTestUtil {
    @TempDir
    File temporaryFolder;

    private File npmFolder;
    private File generatedFolder;
    private File frontendDepsFolder;

    @BeforeEach
    void setup() throws IOException {
        // creating non-existing folder to make sure the execute() creates
        // the folder if missing
        npmFolder = new File(Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile(),
                "child/");
        generatedFolder = new File(npmFolder, "target/frontend");
        frontendDepsFolder = new File(npmFolder, "target/frontend-deps");
    }

    @Test
    void should_collectJsAndCssFilesFromJars_obsoleteResourceFolder()
            throws IOException {
        should_collectJsAndCssFilesFromJars("jar-with-frontend-resources.jar",
                "dir-with-frontend-resources/");
    }

    @Test
    void should_collectJsAndCssFilesFromJars_modernResourceFolder()
            throws IOException {
        should_collectJsAndCssFilesFromJars("jar-with-modern-frontend.jar",
                "dir-with-modern-frontend");
    }

    @Test
    void should_collectJsAndCssFilesFromJars_removeExtraFiles()
            throws IOException {
        File dummy = new File(frontendDepsFolder, "dummy.ts");
        frontendDepsFolder.mkdirs();
        dummy.createNewFile();
        should_collectJsAndCssFilesFromJars("jar-with-modern-frontend.jar",
                "dir-with-modern-frontend");
    }

    @Test
    void should_createPackageJson() throws IOException {
        Options options = new MockOptions(getClassFinder(), npmFolder)
                .withBuildDirectory(TARGET).withBundleBuild(true);

        TaskGeneratePackageJson task = new TaskGeneratePackageJson(options);
        task.execute();
        assertTrue(new File(npmFolder, PACKAGE_JSON).exists());
        assertFalse(new File(generatedFolder, PACKAGE_JSON).exists());
        JsonNode deps = task.getPackageJson().get("dependencies");
        assertFalse(deps.has(NodeUpdater.DEP_NAME_FLOW_DEPS));
        assertFalse(deps.has(NodeUpdater.DEP_NAME_FLOW_JARS));
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
        assertEquals(19, files.size());

        // Check some resources
        assertTrue(files.contains("example.ts"),
                "TS resource should have been copied from jar file");

        assertTrue(files.contains("example.ts.map"),
                "TS resource source map should have been copied from jar file");

        assertTrue(files.contains("ExampleConnector.js"),
                "JS resource should have been copied from jar file");

        assertTrue(files.contains("ExampleConnector.js.map"),
                "JS resource source map should have been copied from jar file");

        assertTrue(files.contains("inline.css"),
                "CSS resource should have been copied from jar file");

        assertTrue(files.contains("inline.css.map"),
                "CSS resource source map should have been copied from jar file");

        assertTrue(files.contains("resourceInFolder.js"),
                "JS resource should have been copied from resource folder");

        assertTrue(files.contains("resourceInFolder.js.map"),
                "JS resource source map should have been copied from resource folder");

        assertTrue(files.contains("react.tsx"),
                "TSX resource should have been copied from jar file");

        assertTrue(files.contains("react.tsx.map"),
                "TSX resource source map should have been copied from jar file");

        assertTrue(files.contains("test.jsx"),
                "JSX resource should have been copied from jar file");

        assertTrue(files.contains("test.jsx.map"),
                "JSX resource source map should have been copied from jar file");

        assertTrue(files.contains("ExampleTemplate.html"),
                "HTML resource should have been copied from jar file");

        assertFalse(files.contains("ignored.js"),
                "Resource from unsupported frontend folder location should not have been copied from jar file");

        assertEquals(
                files.stream()
                        .map(path -> frontendDepsFolder.toPath().resolve(path))
                        .collect(Collectors.toSet()),
                generatedFileSupport.getFiles(),
                "Generated files should have been tracked");
    }

    private static Set<File> jars(File... files) {
        return Stream.of(files).collect(Collectors.toSet());
    }
}
