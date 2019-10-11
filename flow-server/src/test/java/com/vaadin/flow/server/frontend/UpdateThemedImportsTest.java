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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

public class UpdateThemedImportsTest extends NodeUpdateTestUtil {

    public static class MyTheme implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/myTheme/";
        }

        @Override
        public List<String> getHeaderInlineContents() {
            return Collections.emptyList();
        }
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File importsFile;
    private File generatedPath;
    private File frontendDirectory;
    private File nodeModulesPath;
    private TaskUpdateImports updater;

    @Before
    public void setup() throws Exception {
        File tmpRoot = temporaryFolder.getRoot();

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        generatedPath = new File(tmpRoot, DEFAULT_GENERATED_DIR);
        importsFile = new File(generatedPath, IMPORTS_NAME);

        Assert.assertTrue(nodeModulesPath.mkdirs());
        createImport("./src/subfolder/sub-template.js", "");
        createImport("./src/client-side-template.js",
                "import 'xx' from './subfolder/sub-template.js';"
                        + "import '@vaadin/vaadin-button/src/vaadin-button.js'");
        createImport("./src/client-side-no-themed-template.js", "");
        createImport("./src/main-template.js",
                "import 'xx' from './client-side-template.js';"
                        + "import \"./client-side-no-themed-template.js\"';"
                        + "import './src/wrong-themed-template.js';"
                        + "import '@vaadin/vaadin-button/src/vaadin-button.js'");

        // create themed modules
        createImport("./theme/myTheme/subfolder/sub-template.js", "");
        createImport("./theme/myTheme/client-side-template.js", "");
        createImport("./theme/myTheme/main-template.js", "");

        // wrong-themed-template.js should not be resolved inside node_modules.
        // It should be searched only inside frontend directory
        createImport("theme/myTheme/wrong-themed-template.js", "");
        // create css files to avoid exception when files not found during the
        // test
        createImport("./foo.css", "");
        createImport("@vaadin/vaadin-mixed-component/bar.css", "");

        // make external component's module and its themed version
        createImport("@vaadin/vaadin-button/src/vaadin-button.js", "");
        createImport("@vaadin/vaadin-button/theme/myTheme/vaadin-button.js",
                "");

        ClassFinder finder = getClassFinder();
        FrontendDependencies deps = new FrontendDependencies(finder) {

            @Override
            public List<String> getModules() {
                return Stream.of("./src/main-template.js")
                        .collect(Collectors.toList());
            }

            @Override
            public Set<String> getScripts() {
                return Collections.emptySet();
            }

            @Override
            public AbstractTheme getTheme() {
                return new MyTheme();
            }

            @Override
            public ThemeDefinition getThemeDefinition() {
                return new ThemeDefinition(MyTheme.class, "");
            }
        };
        updater = new TaskUpdateImports(finder, deps, cf -> null, tmpRoot,
                generatedPath, frontendDirectory, null, null);
    }

    @Test
    public void themedClientSideModulesAreWrittenIntoImportsFile()
            throws Exception {
        updater.execute();

        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        Assert.assertThat(content, CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "import 'Frontend/theme/myTheme/main-template.js';"),
                CoreMatchers.containsString(
                        "import 'Frontend/theme/myTheme/client-side-template.js';"),
                CoreMatchers.containsString(
                        "import 'Frontend/theme/myTheme/subfolder/sub-template.js';"),
                CoreMatchers.containsString(
                        "import '@vaadin/vaadin-button/theme/myTheme/vaadin-button.js';"),
                CoreMatchers.not(CoreMatchers.containsString(
                        "import 'theme/myTheme/wrong-themed-template.js';"))));
    }

    @Test
    public void noDuplicateImportEntryIsWrittenIntoImportsFile()
            throws Exception {
        updater.execute();

        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        int count = StringUtils.countMatches(content,
                "import '@vaadin/vaadin-button/theme/myTheme/vaadin-button.js';");
        Assert.assertEquals(
                "Import entries in the imports file should be unique.", 1,
                count);
    }

    private void createImport(String path, String content) throws IOException {
        File newFile = resolveImportFile(frontendDirectory, nodeModulesPath,
                path);
        newFile.getParentFile().mkdirs();
        newFile.delete();
        Assert.assertTrue(newFile.createNewFile());
        if (content != null) {
            Files.write(newFile.toPath(), Collections.singletonList(content));
        }
    }

}
