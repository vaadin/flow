package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class TaskRunDevBundleBuildTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void noDevBundle_bundleCompilationRequires() {
        final boolean needsBuild = TaskRunDevBundleBuild.needsBuild(
                temporaryFolder.getRoot(),
                Mockito.mock(FrontendDependenciesScanner.class));
        Assert.assertTrue("Bundle should require creation if not available",
                needsBuild);
    }

    @Test
    public void devBundleStatsJsonMissing_bundleCompilationRequires() {
        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(null);

            final boolean needsBuild = TaskRunDevBundleBuild.needsBuild(
                    temporaryFolder.getRoot(),
                    Mockito.mock(FrontendDependenciesScanner.class));
            Assert.assertTrue("Missing stats.json should require bundling",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_noNpmPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.7.4\",\n" + " },\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuild(temporaryFolder.getRoot(), depScanner);
            Assert.assertFalse("Missing stats.json should require bundling",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_statsMissingNpmPackages_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.7.4");
        packages.put("@vaadin/text", "1.0.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.7.4\",\n" + " },\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuild(temporaryFolder.getRoot(), depScanner);
            Assert.assertTrue("Missing npmPackage should require bundling",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_statsMissingPackageJsonPackage_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\", \"@vaadin/text\":\"1.0.0\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.7.4\",\n" + " },\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuild(temporaryFolder.getRoot(), depScanner);
            Assert.assertTrue("Bundle missing module dependency should rebuild",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_packageJsonMissingNpmPackages_statsHasJsonPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.7.4\",\n"
                            + "  \"@vaadin/text\": \"1.0.0\",\n" + " },\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuild(temporaryFolder.getRoot(), depScanner);
            Assert.assertFalse(
                    "Not missing npmPackage in stats.json should not require compilation",
                    needsBuild);
        }
    }
}
