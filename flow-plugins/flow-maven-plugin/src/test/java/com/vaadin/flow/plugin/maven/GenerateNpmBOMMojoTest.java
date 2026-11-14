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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class GenerateNpmBOMMojoTest {

    private String bomFilename;

    private File resourceOutputDirectory;

    @TempDir
    Path tempDir;

    private File jarResourcesSource;
    private File nodeModulesDir;

    private GenerateNpmBOMMojo mojo;

    private Lookup lookup;

    @BeforeEach
    void setUp() throws Exception {
        this.mojo = spy(new GenerateNpmBOMMojo());

        File projectBase = tempDir.toFile();
        File frontendDirectory = new File(projectBase,
                FrontendUtils.DEFAULT_FRONTEND_DIR);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        jarResourcesSource = new File(projectBase,
                "jar-resources-source/META-INF/frontend");
        jarResourcesSource.mkdirs();
        bomFilename = new File(resourceOutputDirectory, "bom-npm.json")
                .getAbsolutePath();

        nodeModulesDir = new File(tempDir.toFile(), "node_modules");
        boolean nodeModulesDirCreated = nodeModulesDir.mkdir();
        assertTrue(nodeModulesDirCreated);

        String manifestFilePath = new File(projectBase, PACKAGE_JSON)
                .getAbsolutePath();
        // set Mojo properties
        ReflectionUtils.setVariableValueInObject(mojo, "ignoreNpmErrors",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "packageLockOnly",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "omit", "dev");
        ReflectionUtils.setVariableValueInObject(mojo, "flattenComponents",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "shortPURLs", false);
        ReflectionUtils.setVariableValueInObject(mojo, "outputReproducible",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "validate", true);
        ReflectionUtils.setVariableValueInObject(mojo, "mcType", "application");
        ReflectionUtils.setVariableValueInObject(mojo, "outputFormat", "json");
        ReflectionUtils.setVariableValueInObject(mojo, "outputFilePath",
                bomFilename);
        ReflectionUtils.setVariableValueInObject(mojo, "packageManifest",
                manifestFilePath);
        ReflectionUtils.setVariableValueInObject(mojo, "specVersion", "1.4");
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                frontendDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBuildDir",
                Paths.get(projectBase.toString(), "target").toString());
        ReflectionUtils.setVariableValueInObject(mojo, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "productionMode", false);
        doReturn(Set.of(jarResourcesSource.getParentFile().getParentFile()))
                .when(mojo).getJarFiles();

        FileUtils.fileWrite(manifestFilePath, "UTF-8",
                TestUtils.getInitialPackageJson().toString());
        lookup = mock(Lookup.class);
        doReturn(new TestEndpointGeneratorTaskFactory()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);
        doAnswer(invocation -> {
            doReturn(invocation.getArguments()[0]).when(lookup)
                    .lookup(ClassFinder.class);
            return lookup;
        }).when(mojo).createLookup(any(ClassFinder.class));

        setProject(mojo, projectBase);
        // Prevent unwanted resources to be present on classpath
        mojo.project.setArtifacts(Set.of());
    }

    @Test
    void shouldGenerateSBOM() throws Exception {
        Path pnpmLock = Files.createFile(tempDir.resolve("pnpm-lock.yaml"));
        assertFalse(Files.exists(Paths.get(bomFilename)));
        mojo.execute();
        assertTrue(Files.exists(Paths.get(bomFilename)));
        assertTrue(Files.exists(pnpmLock),
                "pnpm-lock.yaml should not be removed");
    }

    @Test
    void getProperties_runsToolWithGivenNpx() {
        String node = new File("node").getAbsolutePath();
        String npxCli = new File("dir with space", "npx-cli.js")
                .getAbsolutePath();

        Properties properties = mojo.getProperties(List.of(node, npxCli));

        assertEquals(node, properties.getProperty("exec.executable"));
        assertTrue(
                properties.getProperty("exec.args")
                        .startsWith("\"" + npxCli + "\" --yes "
                                + GenerateNpmBOMMojo.CYCLONEDX_NPM + " "),
                properties.getProperty("exec.args"));
    }

    @Test
    void getProperties_runsSingleExecutableNpx() {
        String npx = new File("dir with space", "npx").getAbsolutePath();

        Properties properties = mojo.getProperties(List.of(npx));

        assertEquals(npx, properties.getProperty("exec.executable"));
        assertTrue(
                properties.getProperty("exec.args").startsWith(
                        "--yes " + GenerateNpmBOMMojo.CYCLONEDX_NPM + " "),
                properties.getProperty("exec.args"));
    }

    @Test
    void createInvocationRequest_inheritsSessionConfiguration() {
        mojo.session = InvocationRequestBuilderTest.session(
                new DefaultMavenExecutionRequest().setOffline(true),
                new Properties());

        String npx = new File("npx").getAbsolutePath();

        InvocationRequest request = mojo.createInvocationRequest(List.of(npx));

        assertTrue(request.isOffline());
        assertEquals(npx,
                request.getProperties().getProperty("exec.executable"));
    }

    @Test
    void cycloneDxNpm_isPinnedToExactVersion() {
        assertTrue(
                GenerateNpmBOMMojo.CYCLONEDX_NPM
                        .matches("@cyclonedx/cyclonedx-npm@\\d+\\.\\d+\\.\\d+"),
                GenerateNpmBOMMojo.CYCLONEDX_NPM);
    }

    @Test
    void addNodeToPath_putsNodeDirectoryFirst() {
        File nodeDir = new File("node-dir").getAbsoluteFile();
        InvocationRequest request = new DefaultInvocationRequest();

        GenerateNpmBOMMojo.addNodeToPath(request,
                new File(nodeDir, "node").getPath());

        String path = request.getShellEnvironments().entrySet().stream()
                .filter(entry -> "PATH".equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElseThrow();
        assertEquals(nodeDir.getPath(),
                path.split(Pattern.quote(File.pathSeparator))[0]);
    }

    @Test
    void shouldFailWhenNoPackageJsonIsPresent() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "packageManifest", "");
        assertThrows(MojoFailureException.class, () -> {
            mojo.execute();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void shouldInstallWithProjectPackageManagerIfNodeModulesIsNotPresent(
            boolean pnpm) throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "pnpmEnable", pnpm);
        assertTrue(nodeModulesDir.delete());
        assertFalse(nodeModulesDir.exists());
        mojo.execute();
        assertTrue(nodeModulesDir.exists());
        assertEquals(pnpm, Files.exists(tempDir.resolve("pnpm-lock.yaml")));
        assertEquals(!pnpm, Files.exists(tempDir.resolve("package-lock.json")));
    }

    @Test
    void shouldSkipNpmInstallIfNodeModulesIsPresent() throws Exception {
        List<String> originalContent = TestUtils
                .listFilesRecursively(nodeModulesDir);

        mojo.execute();

        List<String> newContent = TestUtils
                .listFilesRecursively(nodeModulesDir);
        assertArrayEquals(originalContent.toArray(), newContent.toArray());
    }

}
