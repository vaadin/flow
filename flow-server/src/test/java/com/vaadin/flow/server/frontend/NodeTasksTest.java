package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.connect.Endpoint;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;
import static org.junit.Assert.assertTrue;

public class NodeTasksTest {

    @Endpoint
    public static class ConnectEndpointsForTesting {
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    String userDir;

    @Before
    public void setup() {
        userDir = temporaryFolder.getRoot().getAbsolutePath();
        System.setProperty("user.dir", userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);
        System.clearProperty(PARAM_GENERATED_DIR);
    }

    @Test
    public void should_UseDefaultFolders()throws Exception {
        Builder builder = new Builder(
                new DefaultClassFinder(this.getClass().getClassLoader()),
                new File(userDir))
            .enablePackagesUpdate(false)
            .enableImportsUpdate(true)
            .runNpmInstall(false)
            .withEmbeddableWebComponents(false);


        Assert.assertEquals(new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File)getFieldValue(builder, "frontendDirectory")).getAbsolutePath());
        Assert.assertEquals(new File(userDir, DEFAULT_GENERATED_DIR).getAbsolutePath(),
                ((File)getFieldValue(builder, "generatedFolder")).getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(new File(userDir, DEFAULT_GENERATED_DIR + IMPORTS_NAME).exists());
    }

    @Test
    public void should_BeAbleToCustomizeFolders() throws Exception {
        System.setProperty(PARAM_FRONTEND_DIR, "my_custom_sources_folder");
        System.setProperty(PARAM_GENERATED_DIR, "my/custom/generated/folder");

        Builder builder = new Builder(
                new DefaultClassFinder(this.getClass().getClassLoader()),
                new File(userDir))
            .enablePackagesUpdate(false)
            .enableImportsUpdate(true)
            .runNpmInstall(false)
            .withEmbeddableWebComponents(false);

        Assert.assertEquals(new File(userDir, "my_custom_sources_folder").getAbsolutePath(),
                ((File)getFieldValue(builder, "frontendDirectory")).getAbsolutePath());
        Assert.assertEquals(new File(userDir, "my/custom/generated/folder").getAbsolutePath(),
                ((File)getFieldValue(builder, "generatedFolder")).getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(new File(userDir, "my/custom/generated/folder/" + IMPORTS_NAME).exists());
    }

    @Test
    public void should_SetIsClientBootstrapMode_When_EnableClientSideBootstrapMode()
            throws ExecutionFailedException, IOException {
        Builder builder = new Builder(
                new DefaultClassFinder(this.getClass().getClassLoader()),
                new File(userDir))
                        .enablePackagesUpdate(false)
                        .withWebpack(new File(userDir, TARGET + "classes"),
                                WEBPACK_CONFIG, WEBPACK_GENERATED)
                        .enableImportsUpdate(true).runNpmInstall(false)
                        .withEmbeddableWebComponents(false)
                        .enableClientSideMode(true);
        builder.build().execute();
        String webpackGeneratedContent = Files
                .lines(new File(userDir, WEBPACK_GENERATED).toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "useClientSideIndexFileForBootstrapping should be true",
                webpackGeneratedContent.contains(
                        "const useClientSideIndexFileForBootstrapping = true;"));
    }

    @Test
    public void should_Generate_Connect_Files() throws Exception {
        File src = new File(getClass().getClassLoader().getResource("java").getFile());
        File dir = new File(userDir);
        File json = new File(dir, "api-file.json");

        Builder builder = new Builder(
                new DefaultClassFinder(
                    Collections.singleton(ConnectEndpointsForTesting.class)), dir)
                        .enablePackagesUpdate(false)
                        .enableImportsUpdate(false)
                        .withEmbeddableWebComponents(false)
                        .enableClientSideMode(true)
                        .withConnectJavaSourceFolder(src)
                        .withConnectGeneratedOpenApiJson(json)
                        .withConnectClientTsApiFolder(new File(dir, "api"));

        builder.build().execute();

        Arrays.asList(
                // enableClientSide
                "target/index.html",
                "target/index.ts",
                // withConnectJavaSourceFolder and withConnectGeneratedOpenApiJson
                "api-file.json",
                // withConnectClientTsApiFolder
                "api/connect-client.default.ts",
                "api/MyEndpoint.ts")
                .forEach(name -> assertTrue(name + " not created.", new File(dir, name).exists()));
    }

    private Object getFieldValue(Object obj, String name) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }
}
