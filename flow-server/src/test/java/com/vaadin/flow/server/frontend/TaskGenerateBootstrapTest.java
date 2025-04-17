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
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.frontend.FrontendUtils.FEATURE_FLAGS_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TSX;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.getClassFinder;

public class TaskGenerateBootstrapTest {

    private static final String DEV_TOOLS_IMPORT = "import '"
            + FrontendUtils.JAR_RESOURCES_IMPORT
            + "vaadin-dev-tools/vaadin-dev-tools.js';";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FrontendDependenciesScanner frontDeps;
    private File frontendFolder;
    private TaskGenerateBootstrap taskGenerateBootstrap;

    private Options options;

    @Before
    public void setUp() throws Exception {
        ClassFinder.DefaultClassFinder finder = new ClassFinder.DefaultClassFinder(
                Collections.singleton(this.getClass()));
        frontDeps = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, finder, false, null, true);

        frontendFolder = temporaryFolder.newFolder(FRONTEND);
        options = new MockOptions(finder, null)
                .withFrontendDirectory(frontendFolder).withProductionMode(true);

        taskGenerateBootstrap = new TaskGenerateBootstrap(frontDeps, options);
    }

    @Test
    public void should_importGeneratedIndexTS()
            throws ExecutionFailedException {
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import './index';"));
    }

    @Test
    public void shouldNot_importDevTools_inProduction()
            throws ExecutionFailedException {
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertFalse(content.contains(DEV_TOOLS_IMPORT));
    }

    @Test
    public void should_importDevTools_inDevMode()
            throws ExecutionFailedException {
        options.withProductionMode(false);
        taskGenerateBootstrap = new TaskGenerateBootstrap(frontDeps, options);
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains(DEV_TOOLS_IMPORT));
    }

    @Test
    public void should_importFrontendIndexTS()
            throws ExecutionFailedException, IOException {
        new File(frontendFolder, INDEX_TS).createNewFile();
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import '../index';"));
    }

    @Test
    public void should_importFrontendIndexTSX()
            throws ExecutionFailedException, IOException {
        new File(frontendFolder, INDEX_TSX).createNewFile();
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import '../index';"));
    }

    @Test
    public void should_importFeatureFlagTS() throws ExecutionFailedException {
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains(
                String.format("import './%s';", FEATURE_FLAGS_FILE_NAME)));
    }

    @Test
    public void should_load_AppTheme()
            throws MalformedURLException, ExecutionFailedException {
        options.withFrontendDirectory(frontendFolder).withProductionMode(true);

        taskGenerateBootstrap = new TaskGenerateBootstrap(getThemedDependency(),
                options);
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();

        final List<String> expectedContent = Arrays.asList("import './index';",
                "import { applyTheme } from './theme.js';",
                "applyTheme(document);");

        expectedContent.forEach(expectedLine -> Assert.assertTrue(
                String.format(
                        "Bootstrap 'vaadin.ts' file is supposed to contain "
                                + "the line: [%s],\nbut actually contains the "
                                + "following: [%s]",
                        expectedLine, content),
                content.contains(expectedLine)));
    }

    private FrontendDependencies getThemedDependency()
            throws MalformedURLException {
        ClassFinder finder = getClassFinder();
        return new FrontendDependencies(finder, true, null, true) {

            @Override
            public Map<ChunkInfo, List<String>> getModules() {
                return Collections.emptyMap();
            }

            @Override
            public Map<ChunkInfo, List<String>> getScripts() {
                return Collections.emptyMap();
            }

            @Override
            public AbstractTheme getTheme() {
                return new UpdateThemedImportsTest.MyTheme();
            }

            @Override
            public ThemeDefinition getThemeDefinition() {
                return new ThemeDefinition(
                        UpdateThemedImportsTest.MyTheme.class, "", "my-theme");
            }
        };
    }
}
