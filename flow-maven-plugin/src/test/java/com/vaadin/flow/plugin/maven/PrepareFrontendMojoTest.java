package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import elemental.json.JsonObject;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.assertContainsPackage;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getPackageJson;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

public class PrepareFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final PrepareFrontendMojo mojo = new PrepareFrontendMojo();
    private File projectFrontendResourcesDirectory;
    private File nodeModulesPath;
    private File flowPackagePath;
    private String webpackConfig;
    private String packageJson;
    private File projectBase;
    private File webpackOutputDirectory;


    @Before
    public void setup() throws Exception {
        projectBase = temporaryFolder.getRoot();

        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getBasedir()).thenReturn(projectBase);

        projectFrontendResourcesDirectory = new File(projectBase,
                "flow_resources");

        Assert.assertTrue("Failed to create a test project resources",
                projectFrontendResourcesDirectory.mkdirs());
        Assert.assertTrue("Failed to create a test project file",
                new File(projectFrontendResourcesDirectory,
                        "test_project_resource.js").createNewFile());

        nodeModulesPath = new File(projectBase, NODE_MODULES);
        flowPackagePath = new File(nodeModulesPath, FLOW_NPM_PACKAGE_NAME);
        webpackConfig = new File(projectBase, WEBPACK_CONFIG).getAbsolutePath();
        packageJson = new File(projectBase, PACKAGE_JSON).getAbsolutePath();
        webpackOutputDirectory = new File(projectBase, VAADIN_SERVLET_RESOURCES);

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendResourcesDirectory", projectFrontendResourcesDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "jarResourcePathsToCopy", RESOURCES_FRONTEND_DEFAULT);
        ReflectionUtils.setVariableValueInObject(mojo, "includes", "**/*.js,**/*.css");
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackTemplate", WEBPACK_CONFIG);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedFolder", projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory", webpackOutputDirectory);

        Assert.assertTrue(flowPackagePath.mkdirs());
        setProject(mojo, projectBase);
    }

    @Test
    public void should_copyProjectFrontendResources() {
        Assert.assertTrue("There should be no modules before the mojo is run",
                gatherFiles(nodeModulesPath).isEmpty());
        mojo.execute();

        Set<String> projectFrontendResources = Stream
                .of(projectFrontendResourcesDirectory.listFiles())
                .map(File::getName).collect(Collectors.toSet());
        List<File> filesInNodeModules = gatherFiles(nodeModulesPath);

        Assert.assertEquals(
                "All project resources should be copied into the node_modules",
                projectFrontendResources.size(), filesInNodeModules.size());

        filesInNodeModules.forEach(file -> Assert.assertTrue(String.format(
                "Expected the copied file '%s' to be in the project resources",
                file), projectFrontendResources.contains(file.getName())));
    }

    @Test
    public void mavenGoal_when_packageJsonMissing() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(packageJson));
        mojo.execute();
        assertPackageJsonContent();
        Assert.assertTrue(FileUtils.fileExists(webpackConfig));
    }

    @Test
    public void mavenGoal_when_packageWebpackConfigMissing() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(webpackConfig));
        mojo.execute();
        Assert.assertTrue(FileUtils.fileExists(webpackConfig));
    }

    @Test
    public void should_keepDependencies_when_packageJsonExists() throws Exception {
        FileUtils.fileWrite(packageJson, "{\"dependencies\":{\"foo\":\"bar\"}}");
        mojo.execute();
        assertPackageJsonContent();

        JsonObject packageJsonObject = getPackageJson(packageJson);
        assertContainsPackage(packageJsonObject.getObject("dependencies"), "foo");
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJsonObject = getPackageJson(packageJson);

        assertContainsPackage(packageJsonObject.getObject("dependencies"),
                "@webcomponents/webcomponentsjs",
                "@polymer/polymer");

        assertContainsPackage(packageJsonObject.getObject("devDependencies"),
                "webpack",
                "webpack-cli",
                "webpack-dev-server",
                "webpack-babel-multi-target-plugin",
                "copy-webpack-plugin");
    }


    private List<File> gatherFiles(File root) {
        if (root.isFile()) {
            return Collections.singletonList(root);
        } else {
            File[] subdirectoryFiles = root.listFiles();
            if (subdirectoryFiles != null) {
                List<File> files = new ArrayList<>();
                for (File subdirectoryFile : subdirectoryFiles) {
                    files.addAll(gatherFiles(subdirectoryFile));
                }
                return files;
            }
            return Collections.emptyList();
        }
    }
}
