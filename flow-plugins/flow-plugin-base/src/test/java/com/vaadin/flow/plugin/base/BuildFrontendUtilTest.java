/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.TaskGenerateHilla;
import com.vaadin.flow.server.frontend.TaskRunNpmInstall;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.utils.LookupImpl;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class BuildFrontendUtilTest {

    private File baseDir;

    private PluginAdapterBuild adapter;

    private FrontendTools tools;

    private Lookup lookup;

    private MockedStatic<LicenseChecker> licenseChecker;

    @Before
    public void setup() throws IOException {
        licenseChecker = Mockito.mockStatic(LicenseChecker.class);
        TemporaryFolder tmpDir = new TemporaryFolder();
        tmpDir.create();
        baseDir = tmpDir.newFolder();

        adapter = Mockito.mock(PluginAdapterBuild.class);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        Mockito.when(adapter.projectBaseDirectory())
                .thenReturn(tmpDir.getRoot().toPath());
        ClassFinder classFinder = Mockito.mock(ClassFinder.class);
        lookup = Mockito.spy(new LookupImpl(classFinder));
        Mockito.when(adapter.createLookup(Mockito.any())).thenReturn(lookup);
        Mockito.doReturn(classFinder).when(lookup).lookup(ClassFinder.class);

        tools = Mockito.mock(FrontendTools.class);

        // setup: mock a webpack executable
        File webpackBin = new File(baseDir, "node_modules/webpack/bin");
        Assert.assertTrue(webpackBin.mkdirs());
        File webPackExecutableMock = new File(webpackBin, "webpack.js");
        Assert.assertTrue(webPackExecutableMock.createNewFile());

        // setup: mock a vite executable
        File viteBin = new File(baseDir, "node_modules/vite/bin");
        Assert.assertTrue(viteBin.mkdirs());
        File viteExecutableMock = new File(viteBin, "vite.js");
        Assert.assertTrue(viteExecutableMock.createNewFile());

        File resourceOutput = new File(baseDir, "resOut");
        Mockito.when(adapter.servletResourceOutputDirectory())
                .thenReturn(resourceOutput);
        File statsJson = new File(new File(resourceOutput, "config"),
                "stats.json");
        statsJson.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(statsJson)) {
            IOUtils.write("{\"npmModules\":[]}", out, StandardCharsets.UTF_8);
        }
    }

    @After
    public void cleanup() {
        licenseChecker.close();
    }

    @Test
    public void testWebpackRequiredFlagsPassedToNodeEnvironment()
            throws IOException, URISyntaxException, TimeoutException {
        Assume.assumeFalse("Test not runnable on Windows",
                FrontendUtils.isWindows());
        Assume.assumeTrue("Test requires /bin/bash",
                new File("/bin/bash").exists());

        // given: "node" stub that exits normally only if expected environment
        // set
        File fakeNode = new File(baseDir, "node");
        try (PrintWriter out = new PrintWriter(fakeNode)) {
            out.println("#!/bin/bash");
            out.println("[ x$NODE_OPTIONS == xexpected ]");
            out.println("exit $?");
        }
        Assert.assertTrue(fakeNode.setExecutable(true));
        Mockito.when(tools.getNodeExecutable())
                .thenReturn(fakeNode.getAbsolutePath());

        Map<String, String> environment = new HashMap<>();
        environment.put("NODE_OPTIONS", "expected");
        Mockito.when(tools.getWebpackNodeEnvironment()).thenReturn(environment);

        // then
        BuildFrontendUtil.runWebpack(adapter, tools);

        // terminates successfully

        // license check for prod build
        licenseChecker.verify(() -> LicenseChecker.checkLicense("flow",
                Version.getFullVersion(), BuildType.PRODUCTION));
    }

    @Test
    public void should_notUseHilla_inPrepareFrontend()
            throws ExecutionFailedException, IOException, URISyntaxException {
        setupPluginAdapterDefaults();

        File openApiJsonFile = new File(new File(baseDir, Constants.TARGET),
                FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE);
        Mockito.when(adapter.openApiJsonFile()).thenReturn(openApiJsonFile);

        BuildFrontendUtil.prepareFrontend(adapter);

        Mockito.verify(lookup, Mockito.never())
                .lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup, Mockito.never()).lookup(TaskGenerateHilla.class);
        Mockito.verify(adapter, Mockito.never()).openApiJsonFile();
    }

    @Test
    public void should_useOldEndpointGenerator_withNodeUpdater()
            throws URISyntaxException, ExecutionFailedException {
        setupPluginAdapterDefaults();

        BuildFrontendUtil.runNodeUpdater(adapter);

        Mockito.verify(lookup).lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup, Mockito.never()).lookup(TaskGenerateHilla.class);
    }

    @Test
    public void should_useHillaEngine_withNodeUpdater()
            throws URISyntaxException, ExecutionFailedException, IOException {
        setupPluginAdapterDefaults();

        MockedConstruction<TaskRunNpmInstall> construction = Mockito
                .mockConstruction(TaskRunNpmInstall.class);

        final TaskGenerateHilla taskGenerateHilla = Mockito
                .mock(TaskGenerateHilla.class);
        Mockito.doReturn(taskGenerateHilla).when(lookup)
                .lookup(TaskGenerateHilla.class);

        FileUtils.write(
                new File(adapter.javaResourceFolder(),
                        FeatureFlags.PROPERTIES_FILENAME),
                "com.vaadin.experimental.hillaEngine=true\n",
                StandardCharsets.UTF_8);

        BuildFrontendUtil.runNodeUpdater(adapter);

        Mockito.verify(lookup, Mockito.never())
                .lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup).lookup(TaskGenerateHilla.class);
        Mockito.verify(taskGenerateHilla).configure(adapter.npmFolder(),
                adapter.buildFolder());

        // Hilla Engine requires npm install, the order of execution is critical
        final TaskRunNpmInstall taskRunNpmInstall = construction.constructed()
                .get(0);
        InOrder inOrder = Mockito.inOrder(taskRunNpmInstall, taskGenerateHilla);
        inOrder.verify(taskRunNpmInstall).execute();
        inOrder.verify(taskGenerateHilla).execute();
    }

    private void setupPluginAdapterDefaults() throws URISyntaxException {
        Mockito.when(adapter.nodeVersion())
                .thenReturn(FrontendTools.DEFAULT_NODE_VERSION);
        Mockito.when(adapter.nodeDownloadRoot()).thenReturn(
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        Mockito.when(adapter.frontendDirectory()).thenReturn(
                new File(baseDir, FrontendUtils.DEFAULT_FRONTEND_DIR));
        Mockito.when(adapter.generatedFolder()).thenReturn(new File(baseDir,
                FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR));
        Mockito.when(adapter.buildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        File javaSourceFolder = new File(baseDir,
                FrontendUtils.DEFAULT_CONNECT_JAVA_SOURCE_FOLDER);
        Assert.assertTrue(javaSourceFolder.mkdirs());
        Mockito.when(adapter.javaSourceFolder()).thenReturn(javaSourceFolder);
        File javaResourceFolder = new File(baseDir, "src/main/resources");
        Assert.assertTrue(javaResourceFolder.mkdirs());
        Mockito.when(adapter.javaResourceFolder())
                .thenReturn(javaResourceFolder);
        Mockito.when(adapter.openApiJsonFile())
                .thenReturn(new File(new File(baseDir, Constants.TARGET),
                        FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE));
        Mockito.when(adapter.getClassFinder())
                .thenReturn(new ClassFinder.DefaultClassFinder(
                        this.getClass().getClassLoader()));
        Mockito.when(adapter.runNpmInstall()).thenReturn(true);
    }
}
