package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class TaskRunDevBundleBuildTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Options options;

    ClassFinder finder;

    @Before
    public void init() {
        options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target");
        finder = Mockito.mock(ClassFinder.class);
    }

    @Test
    public void noDevBundle_bundleCompilationRequires() throws IOException {
        final boolean needsBuild = TaskRunDevBundleBuild.needsBuildInternal(
                options, Mockito.mock(FrontendDependenciesScanner.class),
                finder);
        Assert.assertTrue("Bundle should require creation if not available",
                needsBuild);
    }

    @Test
    public void devBundleStatsJsonMissing_bundleCompilationRequires()
            throws IOException {
        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(null);

            final boolean needsBuild = TaskRunDevBundleBuild.needsBuildInternal(
                    options, Mockito.mock(FrontendDependenciesScanner.class),
                    finder);
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
                    .needsBuildInternal(options, depScanner, finder);
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
                    .needsBuildInternal(options, depScanner, finder);
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
                    .needsBuildInternal(options, depScanner, finder);
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
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Not missing npmPackage in stats.json should not require compilation",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_packageJsonHasRange_statsHasFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
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
                            + "  \"@vaadin/router\": \"1.7.4\"" + "},\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Not missing npmPackage in stats.json should not require compilation",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_packageJsonHasTildeRange_statsHasNewerFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"~1.7.4\"}, "
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
                            + "  \"@vaadin/router\": \"1.7.6\"" + "},\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "No compilation if tilde range only patch update",
                    needsBuild);

            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.8.1\"" + "},\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            needsBuild = TaskRunDevBundleBuild.needsBuildInternal(options,
                    depScanner, finder);
            Assert.assertTrue(
                    "Compilation required if minor version change for tilde range",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_packageJsonHasCaretRange_statsHasNewerFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
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
                            + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "No compilation if caret range only minor version update",
                    needsBuild);

            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"2.0.0\"" + "},\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            needsBuild = TaskRunDevBundleBuild.needsBuildInternal(options,
                    depScanner, finder);
            Assert.assertTrue(
                    "Compilation required if major version change for caret range",
                    needsBuild);
        }
    }

    @Test
    public void noPackageJson_defaultPackagesAndModulesInStats_noBuildNeeded()
            throws IOException {
        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        String defaultHash = TaskRunDevBundleBuild
                .getDefaultPackageJson(options, depScanner, finder)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "    \"@polymer/polymer\": \"3.5.1\",\n"
                            + "    \"@vaadin/common-frontend\": \"0.0.17\",\n"
                            + "    \"@vaadin/router\": \"1.7.4\",\n"
                            + "    \"construct-style-sheets-polyfill\": \"3.1.0\",\n"
                            + "    \"lit\": \"2.4.1\",\n"
                            + "    \"@vaadin/text\": \"1.0.0\"\n},\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n" + " \"packageJsonHash\": \"" + defaultHash
                            + "\"\n" + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Default package.json should be built and validated",
                    needsBuild);
        }
    }

    @Test
    public void noPackageJson_defaultPackagesInStats_missingNpmModules_buildNeeded()
            throws IOException {
        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        String defaultHash = TaskRunDevBundleBuild
                .getDefaultPackageJson(options, depScanner, finder)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "    \"@polymer/polymer\": \"3.5.1\",\n"
                            + "    \"@vaadin/common-frontend\": \"0.0.17\",\n"
                            + "    \"@vaadin/router\": \"1.7.4\",\n"
                            + "    \"construct-style-sheets-polyfill\": \"3.1.0\",\n"
                            + "    \"lit\": \"2.4.1\"\n" + " },\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n" + " \"packageJsonHash\": \"" + defaultHash
                            + "\"\n" + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Missing NpmPackage with default bundle should require rebuild",
                    needsBuild);
        }
    }

    @Test
    public void noPackageJson_defaultPackagesInStats_noBuildNeeded()
            throws IOException {
        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        String defaultHash = TaskRunDevBundleBuild
                .getDefaultPackageJson(options, depScanner, finder)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "    \"@polymer/polymer\": \"3.5.1\",\n"
                            + "    \"@vaadin/common-frontend\": \"0.0.17\",\n"
                            + "    \"@vaadin/router\": \"1.7.4\",\n"
                            + "    \"construct-style-sheets-polyfill\": \"3.1.0\",\n"
                            + "    \"lit\": \"2.4.1\"\n" + " },\n"
                            + " \"entryScripts\": [\n"
                            + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n"
                            + " ],\n" + " \"packageJsonHash\": \"" + defaultHash
                            + "\"\n" + "}");

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Default package.json should be built and validated",
                    needsBuild);
        }
    }

    @Test
    public void generatedFlowImports_bundleMissingImports_buildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonList("@polymer/paper-checkbox/paper-checkbox.js"));

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                            + " \"bundleImports\": [\n"
                            + "  \"@polymer/paper-input/paper-input.js\",\n"
                            + "  \"@vaadin/common-frontend/ConnectionIndicator.js\",\n"
                            + "  \"Frontend/generated/jar-resources/dndConnector-es6.js\"\n"
                            + " ],\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Compilation required as stats.json missing import",
                    needsBuild);
        }

    }

    @Test
    public void generatedFlowImports_bundleHasAllImports_noBuildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonList("@polymer/paper-checkbox/paper-checkbox.js"));

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn("{\n" + " \"npmModules\": {\n"
                            + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                            + " \"bundleImports\": [\n"
                            + "  \"@polymer/paper-checkbox/paper-checkbox.js\",\n"
                            + "  \"@polymer/paper-input/paper-input.js\",\n"
                            + "  \"@vaadin/common-frontend/ConnectionIndicator.js\",\n"
                            + "  \"Frontend/generated/jar-resources/dndConnector-es6.js\"\n"
                            + " ],\n"
                            + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                            + "}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("All imports in stats, no compilation required",
                    needsBuild);
        }

    }
}
