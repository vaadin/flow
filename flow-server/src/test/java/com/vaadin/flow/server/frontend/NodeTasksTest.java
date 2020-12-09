package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;
public class NodeTasksTest {

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
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup,
                new File(userDir))
            .enablePackagesUpdate(false)
            .enableImportsUpdate(true)
            .runNpmInstall(false)
            .withEmbeddableWebComponents(false);

        Assert.assertEquals(
                new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File) getFieldValue(builder, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                new File(userDir, DEFAULT_GENERATED_DIR).getAbsolutePath(),
                ((File) getFieldValue(builder, "generatedFolder"))
                        .getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(
                new File(userDir, DEFAULT_GENERATED_DIR + IMPORTS_NAME)
                        .exists());
    }

    @Test
    public void should_generateServiceWorkerWhenPwa() throws Exception {
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(
                mockedLookup,
                new File(userDir)).enablePackagesUpdate(false)
                        .enableImportsUpdate(true).runNpmInstall(false)
                        .withEmbeddableWebComponents(false);

        Assert.assertEquals(
                new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File) getFieldValue(builder, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                new File(userDir, DEFAULT_GENERATED_DIR).getAbsolutePath(),
                ((File) getFieldValue(builder, "generatedFolder"))
                        .getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(
                new File(userDir, DEFAULT_GENERATED_DIR + IMPORTS_NAME)
                        .exists());
    }

    @Test
    public void should_BeAbleToCustomizeFolders() throws Exception {
        System.setProperty(PARAM_FRONTEND_DIR, "my_custom_sources_folder");
        System.setProperty(PARAM_GENERATED_DIR, "my/custom/generated/folder");

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup,
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
        Assert.assertTrue(
                new File(userDir, "my/custom/generated/folder/" + IMPORTS_NAME)
                        .exists());
    }

    @Test
    public void should_SetIsClientBootstrapMode_When_EnableClientSideBootstrapMode()
            throws ExecutionFailedException, IOException {
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup,
                new File(userDir))
                        .enablePackagesUpdate(false)
                        .withWebpack(new File(userDir, TARGET + "webapp"),
                                new File(userDir, TARGET + "classes"),
                                WEBPACK_CONFIG, WEBPACK_GENERATED, SERVICE_WORKER_SRC)
                        .enableImportsUpdate(true).runNpmInstall(false)
                        .withEmbeddableWebComponents(false)
                        .useV14Bootstrap(false).withFlowResourcesFolder(
                                new File(userDir, TARGET + "flow-frontend"));
        builder.build().execute();
        String webpackGeneratedContent = Files
                .lines(new File(userDir, WEBPACK_GENERATED).toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "useClientSideIndexFileForBootstrapping should be true",
                webpackGeneratedContent.contains(
                        "const useClientSideIndexFileForBootstrapping = true;"));
    }

    private Object getFieldValue(Object obj, String name) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }
}
