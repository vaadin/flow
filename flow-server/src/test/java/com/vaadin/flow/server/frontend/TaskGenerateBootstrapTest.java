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
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.FEATURE_FLAGS_FILE_NAME;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.getClassFinder;

public class TaskGenerateBootstrapTest {

    private static final String DEV_TOOLS_IMPORT = "import '"
            + FrontendUtils.JAR_RESOURCES_IMPORT + "vaadin-dev-tools.js';";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FrontendDependenciesScanner frontDeps;
    private File frontendFolder;
    private TaskGenerateBootstrap taskGenerateBootstrap;

    @Before
    public void setUp() throws Exception {
        frontDeps = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, new ClassFinder.DefaultClassFinder(
                        Collections.singleton(this.getClass())), false);

        frontendFolder = temporaryFolder.newFolder(FRONTEND);
        taskGenerateBootstrap = new TaskGenerateBootstrap(frontDeps,
                frontendFolder, true);
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
        taskGenerateBootstrap = new TaskGenerateBootstrap(frontDeps,
                frontendFolder, false);
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
    public void should_importFeatureFlagTS() throws ExecutionFailedException {
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains(
                String.format("import './%s';", FEATURE_FLAGS_FILE_NAME)));
    }

    @Test
    public void should_load_AppTheme()
            throws MalformedURLException, ExecutionFailedException {
        taskGenerateBootstrap = new TaskGenerateBootstrap(getThemedDependency(),
                frontendFolder, true);
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
        return new FrontendDependencies(finder) {

            @Override
            public List<String> getModules() {
                return Collections.emptyList();
            }

            @Override
            public Set<String> getScripts() {
                return Collections.emptySet();
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
