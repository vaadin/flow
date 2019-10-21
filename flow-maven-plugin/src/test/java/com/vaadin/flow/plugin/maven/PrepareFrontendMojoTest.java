package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.assertContainsPackage;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getPackageJson;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_CLIENT_SIDE_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

public class PrepareFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final PrepareFrontendMojo mojo = new PrepareFrontendMojo();
    private File nodeModulesPath;
    private File flowPackagePath;
    private String webpackConfig;
    private String openApiJsonFile;
    private String packageJson;
    private File projectBase;
    private File webpackOutputDirectory;
    private File tokenFile;
    private File defaultJavaSource;
    private File generatedTsFolder;

    @Before
    public void setup() throws Exception {

        projectBase = temporaryFolder.getRoot();

        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + TOKEN_FILE);

        nodeModulesPath = new File(projectBase, NODE_MODULES);
        flowPackagePath = new File(nodeModulesPath, FLOW_NPM_PACKAGE_NAME);
        webpackConfig = new File(projectBase, WEBPACK_CONFIG).getAbsolutePath();
        packageJson = new File(projectBase, PACKAGE_JSON).getAbsolutePath();
        openApiJsonFile = new File(projectBase,
                "target/generated-resources/openapi.json").getAbsolutePath();
        webpackOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        defaultJavaSource = new File(".", "src/test/resources/java");
        generatedTsFolder = new File(projectBase, "frontend/generated");

        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackTemplate",
                WEBPACK_CONFIG);
        ReflectionUtils.setVariableValueInObject(mojo,
                "webpackGeneratedTemplate", WEBPACK_GENERATED);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedFolder",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                webpackOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                new File(projectBase, "frontend"));

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                new File(projectBase,
                        "target/generated-resources/openapi.json"));
        ReflectionUtils.setVariableValueInObject(mojo, "javaSourceFolder",
                defaultJavaSource);

        ReflectionUtils.setVariableValueInObject(mojo, "generatedTsFolder",
                generatedTsFolder);

        Assert.assertTrue(flowPackagePath.mkdirs());
        setProject(mojo, projectBase);
    }

    @Test
    public void tokenFileShouldExist_noDevModeTokenVisible()
            throws IOException, MojoExecutionException, MojoFailureException {
        mojo.execute();
        Assert.assertTrue("No token file could be found", tokenFile.exists());

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        JsonObject buildInfo = JsonUtil.parse(json);
        Assert.assertNull("No devMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_ENABLE_DEV_SERVER));
        Assert.assertNotNull("compatibilityMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_COMPATIBILITY_MODE));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
        Assert.assertNotNull("clientSideMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_CLIENT_SIDE_MODE));
    }

    @Test
    public void existingTokenFile_enableDevServerShouldBeRemoved()
            throws IOException, MojoExecutionException, MojoFailureException {

        JsonObject initialBuildInfo = Json.createObject();
        initialBuildInfo.put(SERVLET_PARAMETER_COMPATIBILITY_MODE, false);
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(SERVLET_PARAMETER_CLIENT_SIDE_MODE, false);
        initialBuildInfo.put(SERVLET_PARAMETER_ENABLE_DEV_SERVER, false);
        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                JsonUtil.stringify(initialBuildInfo, 2) + "\n", "UTF-8");

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        JsonObject buildInfo = JsonUtil.parse(json);
        Assert.assertNull("No devMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_ENABLE_DEV_SERVER));
        Assert.assertNotNull("compatibilityMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_COMPATIBILITY_MODE));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
        Assert.assertNotNull("clientSideMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_CLIENT_SIDE_MODE));
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
    public void mavenGoal_generateOpenApiJson_when_itIsInClientSideMode()
            throws Exception {
        Assert.assertFalse(FileUtils.fileExists(openApiJsonFile));
        mojo.execute();
        Assert.assertTrue(FileUtils.fileExists(openApiJsonFile));
    }

    @Test
    public void mavenGoal_notGenerateOpenApiJson_when_itIsNotInClientSideMode()
            throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "clientSideMode",
                "false");
        Assert.assertFalse(FileUtils.fileExists(openApiJsonFile));
        mojo.execute();
        Assert.assertFalse(FileUtils.fileExists(openApiJsonFile));
    }

    @Test
    public void mavenGoal_generateTsFiles_when_enabled()
            throws Exception {
        File connectClientApi = new File(generatedTsFolder, "connect-client.default.ts");
        File serviceClientApi = new File(generatedTsFolder, "MyVaadinServices.ts");

        Assert.assertFalse(connectClientApi.exists());
        Assert.assertFalse(serviceClientApi.exists());
        mojo.execute();
        Assert.assertTrue(connectClientApi.exists());
        Assert.assertTrue(serviceClientApi.exists());
    }

    @Test
    public void should_keepDependencies_when_packageJsonExists()
            throws Exception {
        FileUtils.fileWrite(packageJson,
                "{\"dependencies\":{\"foo\":\"bar\"}}");
        mojo.execute();
        assertPackageJsonContent();

        JsonObject packageJsonObject = getPackageJson(packageJson);
        assertContainsPackage(packageJsonObject.getObject("dependencies"),
                "foo");
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJsonObject = getPackageJson(packageJson);

        assertContainsPackage(packageJsonObject.getObject("dependencies"),
                "@webcomponents/webcomponentsjs", "@polymer/polymer");

        assertContainsPackage(packageJsonObject.getObject("devDependencies"),
                "webpack", "webpack-cli", "webpack-dev-server",
                "webpack-babel-multi-target-plugin", "copy-webpack-plugin",
                "html-webpack-plugin");
    }
}
