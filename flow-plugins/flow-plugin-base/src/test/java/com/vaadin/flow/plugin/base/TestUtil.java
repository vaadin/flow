package com.vaadin.flow.plugin.base;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.utils.LookupImpl;

public class TestUtil {
    public static void stubPluginAdapterBase(PluginAdapterBase adapter, File baseDir)
            throws URISyntaxException {
        ClassFinder classFinder = Mockito.mock(ClassFinder.class);
        LookupImpl lookup = Mockito.spy(new LookupImpl(classFinder));
        Mockito.when(adapter.createLookup(Mockito.any())).thenReturn(lookup);
        Mockito.doReturn(classFinder).when(lookup).lookup(ClassFinder.class);

        Mockito.when(adapter.projectBaseDirectory())
                .thenReturn(baseDir.toPath());
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        Mockito.when(adapter.nodeVersion())
                .thenReturn(FrontendTools.DEFAULT_NODE_VERSION);
        Mockito.when(adapter.nodeDownloadRoot()).thenReturn(
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        Mockito.when(adapter.frontendDirectory()).thenReturn(
                new File(baseDir, FrontendUtils.DEFAULT_FRONTEND_DIR));
        Mockito.when(adapter.generatedFolder()).thenReturn(new File(baseDir,
                FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR));
        Mockito.when(adapter.buildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(adapter.javaSourceFolder())
                .thenReturn(new File(baseDir, "src/main/java"));
        Mockito.when(adapter.javaResourceFolder())
                .thenReturn(new File(baseDir, "src/main/resources"));
        Mockito.when(adapter.openApiJsonFile())
                .thenReturn(new File(new File(baseDir, Constants.TARGET),
                        FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE));
        Mockito.when(adapter.getClassFinder())
                .thenReturn(new ClassFinder.DefaultClassFinder(
                        TestUtil.class.getClassLoader()));
    }
}
