package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.utils.LookupImpl;

public class BuildFrontendUtilTest {

    @Test
    public void testWebpackRequiredFlagsPassedToNodeEnvironment()
            throws IOException, URISyntaxException, TimeoutException {
        Assume.assumeFalse("Test not runnable on Windows",
                FrontendUtils.isWindows());
        Assume.assumeTrue("Test requires /bin/bash",
                new File("/bin/bash").exists());

        TemporaryFolder tmpDir = new TemporaryFolder();
        tmpDir.create();
        File baseDir = tmpDir.newFolder();

        // setup: mock a webpack executable
        File webpackBin = new File(baseDir, "node_modules/webpack/bin");
        Assert.assertTrue(webpackBin.mkdirs());
        File webPackExecutableMock = new File(webpackBin, "webpack.js");
        Assert.assertTrue(webPackExecutableMock.createNewFile());

        PluginAdapterBase adapter = Mockito.mock(PluginAdapterBase.class);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        Mockito.when(adapter.projectBaseDirectory())
                .thenReturn(tmpDir.getRoot().toPath());
        ClassFinder classFinder = Mockito.mock(ClassFinder.class);
        Mockito.when(adapter.createLookup(Mockito.any()))
                .thenReturn(new LookupImpl(classFinder));

        FrontendTools tools = Mockito.mock(FrontendTools.class);

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
    }
}
