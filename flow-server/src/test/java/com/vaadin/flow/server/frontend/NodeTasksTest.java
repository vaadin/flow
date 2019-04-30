/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.router.Route;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;

public class NodeTasksTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File projectRoot;
    private File importsFile;
    private File nodeModulesPath;
    private File frontendDirectory;
    private Path webpackFile;
    private final Map<Class<? extends Annotation>, Integer> annotationScanCount = new HashMap<>();

    @Before
    public void setUp() {
        projectRoot = temporaryFolder.getRoot();
        importsFile = new File(projectRoot, "flow-imports.js");
        nodeModulesPath = new File(projectRoot, "node_modules");
        frontendDirectory = new File(projectRoot, "frontend");
        webpackFile = projectRoot.toPath().resolve(WEBPACK_CONFIG);
    }

    @Test
    public void should_ScanAnnotations_Once() throws Exception {
        createNodeTasksBuilder().build().execute();

        Assert.assertEquals("Route scanned more than once", 1,
                annotationScanCount.get(Route.class).intValue());
    }

    @Test
    public void should_updateWebpack_when_updatesEnabled() throws Exception {
        String webpackContents = "test_webpack_contents";
        Files.write(webpackFile, Collections.singletonList(webpackContents));

        createNodeTasksBuilder().enablePackagesUpdate(true).build().execute();

        Assert.assertFalse("Webpack config contents should be updated",
                Files.lines(webpackFile).anyMatch(webpackContents::equals));
    }

    @Test
    public void should_not_updateWebpack_when_updatesDisabled()
            throws Exception {
        String webpackContents = "test_webpack_contents";
        Files.write(webpackFile, Collections.singletonList(webpackContents));
        createNodeTasksBuilder().enablePackagesUpdate(false).build().execute();

        List<String> webpackConfigContents = Files.lines(webpackFile)
                .collect(Collectors.toList());
        Assert.assertEquals("Webpack config contents should not be updated",
                webpackConfigContents.size(), 1);
        Assert.assertTrue("Webpack config contents should not be updated",
                webpackConfigContents.contains(webpackContents));
    }

    @SuppressWarnings("unchecked")
    private NodeTasks.Builder createNodeTasksBuilder() throws Exception {
        ClassFinder classFinder = getClassFinder();
        ClassFinder classFinderSpy = Mockito.spy(getClassFinder());

        annotationScanCount.clear();
        Mockito.doAnswer(invocation -> {
            Class<? extends Annotation> clazz = (Class<? extends Annotation>) invocation
                    .getArguments()[0];
            annotationScanCount.compute(clazz, (k, v) -> v == null ? 1 : v + 1);
            return classFinder.getAnnotatedClasses(clazz);
        }).when(classFinderSpy).getAnnotatedClasses(
                (Class<? extends Annotation>) Mockito.any());

        createExpectedImports(frontendDirectory, nodeModulesPath);

        return new NodeTasks.Builder(classFinderSpy, frontendDirectory,
                importsFile, projectRoot, nodeModulesPath, true);
    }

}
