package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.TaskGenerateEndpoint;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;
import com.vaadin.flow.server.frontend.TaskRunNpmInstall;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.utils.LookupImpl;
import com.vaadin.pro.licensechecker.Product;

import elemental.json.Json;
import elemental.json.JsonObject;

public class BuildFrontendUtilTest {

    private File baseDir;

    private PluginAdapterBuild adapter;

    private Lookup lookup;

    private File statsJson;

    @Before
    public void setup() throws IOException {
        TemporaryFolder tmpDir = new TemporaryFolder();
        tmpDir.create();
        baseDir = tmpDir.newFolder();

        adapter = Mockito.mock(PluginAdapterBuild.class);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        Mockito.when(adapter.generatedTsFolder())
                .thenReturn(new File(baseDir, "frontend/generated"));
        Mockito.when(adapter.projectBaseDirectory())
                .thenReturn(tmpDir.getRoot().toPath());
        ClassFinder classFinder = Mockito.mock(ClassFinder.class);
        lookup = Mockito.spy(new LookupImpl(classFinder));
        Mockito.when(adapter.createLookup(Mockito.any())).thenReturn(lookup);
        Mockito.doReturn(classFinder).when(lookup).lookup(ClassFinder.class);

        // setup: mock a vite executable
        File viteBin = new File(baseDir, "node_modules/vite/bin");
        Assert.assertTrue(viteBin.mkdirs());
        File viteExecutableMock = new File(viteBin, "vite.js");
        Assert.assertTrue(viteExecutableMock.createNewFile());

        File resourceOutput = new File(baseDir, "resOut");
        Mockito.when(adapter.servletResourceOutputDirectory())
                .thenReturn(resourceOutput);
        statsJson = new File(new File(resourceOutput, "config"), "stats.json");
        statsJson.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(statsJson)) {
            IOUtils.write("{\"npmModules\":{}}", out, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void should_notUseHilla_inPrepareFrontend()
            throws ExecutionFailedException, IOException, URISyntaxException {
        setupPluginAdapterDefaults();

        File openApiJsonFile = new File(new File(baseDir, Constants.TARGET),
                "classes/dev/hilla/openapi.json");
        Mockito.when(adapter.openApiJsonFile()).thenReturn(openApiJsonFile);

        BuildFrontendUtil.prepareFrontend(adapter);

        Mockito.verify(lookup).lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateOpenAPI.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateEndpoint.class);
        Mockito.verify(adapter, Mockito.never()).openApiJsonFile();
    }

    @Test
    public void should_useHillaEngine_withNodeUpdater()
            throws URISyntaxException, ExecutionFailedException, IOException {
        setupPluginAdapterDefaults();

        MockedConstruction<TaskRunNpmInstall> construction = Mockito
                .mockConstruction(TaskRunNpmInstall.class);

        final EndpointGeneratorTaskFactory endpointGeneratorTaskFactory = Mockito
                .mock(EndpointGeneratorTaskFactory.class);
        Mockito.doReturn(endpointGeneratorTaskFactory).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        final TaskGenerateOpenAPI taskGenerateOpenAPI = Mockito
                .mock(TaskGenerateOpenAPI.class);
        Mockito.doReturn(taskGenerateOpenAPI).when(endpointGeneratorTaskFactory)
                .createTaskGenerateOpenAPI(Mockito.any());

        final TaskGenerateEndpoint taskGenerateEndpoint = Mockito
                .mock(TaskGenerateEndpoint.class);
        Mockito.doReturn(taskGenerateEndpoint)
                .when(endpointGeneratorTaskFactory)
                .createTaskGenerateEndpoint(Mockito.any());

        BuildFrontendUtil.runNodeUpdater(adapter);

        Mockito.verify(lookup).lookup(EndpointGeneratorTaskFactory.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateOpenAPI.class);
        Mockito.verify(lookup, Mockito.never())
                .lookup(TaskGenerateEndpoint.class);

        // Hilla Engine requires npm install, the order of execution is critical
        final TaskRunNpmInstall taskRunNpmInstall = construction.constructed()
                .get(0);
        InOrder inOrder = Mockito.inOrder(taskRunNpmInstall,
                taskGenerateOpenAPI, taskGenerateEndpoint);
        inOrder.verify(taskRunNpmInstall).execute();
        inOrder.verify(taskGenerateOpenAPI).execute();
        inOrder.verify(taskGenerateEndpoint).execute();
    }

    @Test
    public void detectsUsedCommercialComponents() {

        String statsJson = """
                    {
                        "cvdlModules": {
                        "component": {
                            "name": "component",
                            "version":"1.2.3"
                           },
                           "comm-component": {
                            "name":"comm-comp",
                            "version":"4.6.5"
                           },
                           "comm-component2": {
                            "name":"comm-comp2",
                            "version":"4.6.5"
                           }
                        }
                    }
                """;

        final FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("comm-component", "4.6.5");
        packages.put("comm-component2", "4.6.5");
        packages.put("@vaadin/button", "1.2.1");
        Mockito.when(scanner.getPackages()).thenReturn(packages);

        List<String> modules = new ArrayList<>();
        modules.add("comm-component/foo.js");
        Map<ChunkInfo, List<String>> modulesMap = Collections
                .singletonMap(ChunkInfo.GLOBAL, modules);
        Mockito.when(scanner.getModules()).thenReturn(modulesMap);

        List<Product> components = BuildFrontendUtil
                .findCommercialFrontendComponents(scanner, statsJson);
        // Two components are included, only one is used
        Assert.assertEquals(1, components.size());
        Assert.assertEquals("comm-comp", components.get(0).getName());
        Assert.assertEquals("4.6.5", components.get(0).getVersion());
    }

    private void writePackageJson(File nodeModulesFolder, String name,
            String version, String cvdlName) throws IOException {
        File componentFolder = new File(nodeModulesFolder, name);
        componentFolder.mkdirs();
        JsonObject json = Json.createObject();
        json.put("name", name);
        json.put("version", version);
        if (cvdlName == null) {
            json.put("license", "MIT");
        } else {
            json.put("license", "CVDL");
            json.put("cvdlName", cvdlName);
        }
        FileUtils.write(new File(componentFolder, "package.json"),
                json.toJson(), StandardCharsets.UTF_8);

    }

    private void setupPluginAdapterDefaults() throws URISyntaxException {
        Mockito.when(adapter.nodeVersion())
                .thenReturn(FrontendTools.DEFAULT_NODE_VERSION);
        Mockito.when(adapter.nodeDownloadRoot()).thenReturn(
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        Mockito.when(adapter.frontendDirectory()).thenReturn(
                new File(baseDir, FrontendUtils.DEFAULT_FRONTEND_DIR));
        Mockito.when(adapter.buildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        File javaSourceFolder = new File(baseDir, "src/main/java");
        Assert.assertTrue(javaSourceFolder.mkdirs());
        Mockito.when(adapter.javaSourceFolder()).thenReturn(javaSourceFolder);
        File javaResourceFolder = new File(baseDir, "src/main/resources");
        Assert.assertTrue(javaResourceFolder.mkdirs());
        Mockito.when(adapter.javaResourceFolder())
                .thenReturn(javaResourceFolder);
        Mockito.when(adapter.openApiJsonFile())
                .thenReturn(new File(new File(baseDir, Constants.TARGET),
                        "classes/dev/hilla/openapi.json"));
        Mockito.when(adapter.getClassFinder())
                .thenReturn(new ClassFinder.DefaultClassFinder(
                        this.getClass().getClassLoader()));
        Mockito.when(adapter.runNpmInstall()).thenReturn(true);
    }
}
