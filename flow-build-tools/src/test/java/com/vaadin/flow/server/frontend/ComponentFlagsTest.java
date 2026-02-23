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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.frontend.NodeTestComponents.FlagView;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.internal.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.internal.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.Constants.TARGET;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class ComponentFlagsTest extends NodeUpdateTestUtil {

    @TempDir
    File temporaryFolder;

    private FeatureFlags featureFlags;
    private File propertiesDir;

    private File importsFile;
    private File frontendDirectory;
    private File nodeModulesPath;
    private File tmpRoot;
    private Class<?>[] testClasses = { FlagView.class,
            EagerFlagRouteAppConf.class };

    @BeforeEach
    void before() throws IOException {

        tmpRoot = temporaryFolder;
        propertiesDir = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();

        featureFlags = new FeatureFlags(Mockito.mock(Lookup.class));
        featureFlags.setPropertiesLocation(propertiesDir);

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        importsFile = FrontendUtils.getFlowGeneratedImports(frontendDirectory);

        assertTrue(nodeModulesPath.mkdirs());
        createExpectedImports(frontendDirectory, nodeModulesPath);
    }

    protected FrontendDependenciesScanner getScanner(ClassFinder finder,
            FeatureFlags featureFlags) {
        return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, finder, true, featureFlags, true);
    }

    @Test
    void should_ExcludeExperimentalComponent_WhenFlagDisabled()
            throws IOException {
        createUpdater().execute();

        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        assertFalse(content
                .contains("@vaadin/example-flag/experimental-module-1.js"));
        assertFalse(content
                .contains("@vaadin/example-flag/experimental-module-2.js"));
        assertFalse(content.contains("experimental-Connector.js"));
    }

    @Test
    void should_ExcludeExperimentalComponent_WhenFlagFoo() throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=FOO\n");
        featureFlags.loadProperties();

        createUpdater().execute();

        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        assertFalse(content
                .contains("@vaadin/example-flag/experimental-module-1.js"));
        assertFalse(content
                .contains("@vaadin/example-flag/experimental-module-2.js"));
        assertFalse(content.contains("experimental-Connector.js"));
    }

    @Test
    void should_IncludeExperimentalComponent_WhenFlagEnabled()
            throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        featureFlags.loadProperties();

        createUpdater().execute();

        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        assertTrue(content
                .contains("@vaadin/example-flag/experimental-module-1.js"));
        assertTrue(content
                .contains("@vaadin/example-flag/experimental-module-2.js"));
        assertTrue(content.contains("experimental-Connector.js"));
    }

    private void createFeatureFlagsFile(String data) throws IOException {
        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME), data,
                StandardCharsets.UTF_8);
    }

    @LoadDependenciesOnStartup()
    public static class EagerFlagRouteAppConf implements AppShellConfigurator {

    }

    private TaskUpdateImports createUpdater() throws IOException {
        ClassFinder classFinder = getClassFinder(testClasses);

        Options options = new MockOptions(classFinder, tmpRoot)
                .withFrontendDirectory(frontendDirectory)
                .withBuildDirectory(TARGET).withProductionMode(true);
        return new TaskUpdateImports(getScanner(classFinder, featureFlags),
                options);
    }
}
