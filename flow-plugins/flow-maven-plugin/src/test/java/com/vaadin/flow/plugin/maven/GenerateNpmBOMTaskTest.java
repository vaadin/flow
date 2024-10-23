package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;

public class GenerateNpmBOMTaskTest {

    private String bomFilename;

    private File resourceOutputDirectory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File jarResourcesSource;
    private File nodeModulesDir;

    private GenerateNpmBOMTask task;

    private Lookup lookup;

    @Before
    public void setUp() throws Exception {

        File projectBase = temporaryFolder.getRoot();
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getBasedir()).thenReturn(projectBase);

        this.task = Mockito.spy(new GenerateNpmBOMTask(project,
                new ReflectionsClassFinder(new URLClassLoader(new URL[0])),
                new SystemStreamLog()));

        File frontendDirectory = new File(projectBase, DEFAULT_FRONTEND_DIR);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        jarResourcesSource = new File(projectBase,
                "jar-resources-source/META-INF/frontend");
        jarResourcesSource.mkdirs();
        bomFilename = new File(resourceOutputDirectory, "bom-npm.json")
                .getAbsolutePath();

        nodeModulesDir = new File(temporaryFolder.getRoot(), "node_modules");
        boolean nodeModulesDirCreated = nodeModulesDir.mkdir();
        Assert.assertTrue(nodeModulesDirCreated);

        String manifestFilePath = new File(projectBase, PACKAGE_JSON)
                .getAbsolutePath();
        // set Mojo properties
        ReflectionUtils.setVariableValueInObject(task, "ignoreNpmErrors",
                false);
        ReflectionUtils.setVariableValueInObject(task, "packageLockOnly",
                false);
        ReflectionUtils.setVariableValueInObject(task, "omit", "dev");
        ReflectionUtils.setVariableValueInObject(task, "flattenComponents",
                false);
        ReflectionUtils.setVariableValueInObject(task, "shortPURLs", false);
        ReflectionUtils.setVariableValueInObject(task, "outputReproducible",
                false);
        ReflectionUtils.setVariableValueInObject(task, "validate", true);
        ReflectionUtils.setVariableValueInObject(task, "mcType", "application");
        ReflectionUtils.setVariableValueInObject(task, "outputFormat", "json");
        ReflectionUtils.setVariableValueInObject(task, "outputFilePath",
                bomFilename);
        ReflectionUtils.setVariableValueInObject(task, "packageManifest",
                manifestFilePath);
        ReflectionUtils.setVariableValueInObject(task, "specVersion", "1.4");
        ReflectionUtils.setVariableValueInObject(task, "project", project);
        ReflectionUtils.setVariableValueInObject(task, "frontendDirectory",
                frontendDirectory);
        ReflectionUtils.setVariableValueInObject(task, "projectBasedir",
                projectBase);
        ReflectionUtils.setVariableValueInObject(task, "projectBuildDir",
                Paths.get(projectBase.toString(), "target").toString());
        ReflectionUtils.setVariableValueInObject(task, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(task, "npmFolder",
                projectBase);
        ReflectionUtils.setVariableValueInObject(task, "productionMode", false);

        Mockito.when(task.getJarFiles()).thenReturn(
                Set.of(jarResourcesSource.getParentFile().getParentFile()));

        FileUtils.fileWrite(manifestFilePath, "UTF-8",
                TestUtils.getInitialPackageJson().toJson());
        lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new TestEndpointGeneratorTaskFactory()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        Mockito.doAnswer(invocation -> {
            Mockito.doReturn(invocation.getArguments()[0]).when(lookup)
                    .lookup(ClassFinder.class);
            return lookup;
        }).when(task).createLookup(Mockito.any(ClassFinder.class));

    }

    @Test
    public void shouldGenerateSBOM() throws Exception {
        Assert.assertFalse(Files.exists(Paths.get(bomFilename)));
        task.execute();
        Assert.assertTrue(Files.exists(Paths.get(bomFilename)));
    }

    @Test(expected = MojoFailureException.class)
    public void shouldFailWhenNoPackageJsonIsPresent() throws Exception {
        ReflectionUtils.setVariableValueInObject(task, "packageManifest", "");
        task.execute();
    }

    @Test
    public void shouldRunNpmInstallIfNodeModulesIsNotPresent()
            throws Exception {
        Assert.assertTrue(nodeModulesDir.delete());
        Assert.assertFalse(nodeModulesDir.exists());
        task.execute();
        Assert.assertTrue(nodeModulesDir.exists());
    }

    @Test
    public void shouldSkipNpmInstallIfNodeModulesIsPresent() throws Exception {
        List<String> originalContent = TestUtils
                .listFilesRecursively(nodeModulesDir);

        task.execute();

        List<String> newContent = TestUtils
                .listFilesRecursively(nodeModulesDir);
        Assert.assertArrayEquals(originalContent.toArray(),
                newContent.toArray());
    }

}
