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
import java.net.MalformedURLException;
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

import static com.vaadin.flow.server.frontend.FrontendUtils.BOOTSTRAP_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.getClassFinder;

public class TaskGenerateBootstrapTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FrontendDependenciesScanner frontDeps;
    private File frontendFolder;
    private File generatedFolder;
    private TaskGenerateBootstrap taskGenerateBootstrap;

    @Before
    public void setUp() throws Exception {
        frontDeps = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, new ClassFinder.DefaultClassFinder(
                        Collections.singleton(this.getClass())), false);

        frontendFolder = temporaryFolder.newFolder(FRONTEND);
        generatedFolder = temporaryFolder.newFolder(FRONTEND, GENERATED);
        taskGenerateBootstrap = new TaskGenerateBootstrap(frontDeps,
                frontendFolder);
    }

    @Test
    public void should_importTargetIndexTS() throws ExecutionFailedException {
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import '../../target/index';\n"));
    }

    @Test
    public void should_importFrontendIndexTS()
            throws ExecutionFailedException, IOException {
        new File(frontendFolder, INDEX_TS).createNewFile();
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import '../index';\n"));
    }

    @Test
    public void should_load_AppTheme() throws MalformedURLException, ExecutionFailedException {
        taskGenerateBootstrap = new TaskGenerateBootstrap(getThemedDependency(), frontendFolder);
        taskGenerateBootstrap.execute();
        String content = taskGenerateBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import '../../target/index';\n" +
            "\n" +
            "//@ts-ignore\n" +
            "import {applyTheme} from '../../target/flow-frontend/themes/theme-generated.js';\n" +
            "applyTheme(document);\n"));
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
