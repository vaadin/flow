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
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class TaskRunDevBundleBuildTest {
    public static final String BLANK_STATS_JSON_WITH_PACKAGE_HASH = "{\n \"npmModules\": {},\n"
            + " \"bundleImports\": [],\n" + " \"frontendHashes\": {},\n"
            + " \"packageJsonHash\": \"a5\"\n}";

    public static final String BLANK_PACKAGE_JSON_WITH_HASH = "{\n \"dependencies\": {},"
            + "\"vaadin\": { \"hash\": \"a5\"} \n}";

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
            mockStatsJson(utils, null);

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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.7.4\",\n" + " },\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.7.4");
        packages.put("@vaadin/text", "1.0.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.7.4\",\n" + " },\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\", \"@vaadin/text\":\"1.0.0\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.7.4\",\n" + " },\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.7.4\",\n"
                    + "  \"@vaadin/text\": \"1.0.0\",\n" + " },\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"~1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
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
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "    \"@polymer/polymer\": \"3.5.1\",\n"
                    + "    \"@vaadin/common-frontend\": \"0.0.17\",\n"
                    + "    \"@vaadin/router\": \"1.7.4\",\n"
                    + "    \"construct-style-sheets-polyfill\": \"3.1.0\",\n"
                    + "    \"lit\": \"2.6.1\",\n"
                    + "    \"@vaadin/text\": \"1.0.0\"\n},\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
                    + " \"packageJsonHash\": \"" + defaultHash + "\"\n" + "}");

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
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "    \"@polymer/polymer\": \"3.5.1\",\n"
                    + "    \"@vaadin/common-frontend\": \"0.0.17\",\n"
                    + "    \"@vaadin/router\": \"1.7.4\",\n"
                    + "    \"construct-style-sheets-polyfill\": \"3.1.0\",\n"
                    + "    \"lit\": \"2.4.1\"\n" + " },\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
                    + " \"packageJsonHash\": \"" + defaultHash + "\"\n" + "}");

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
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "    \"@polymer/polymer\": \"3.5.1\",\n"
                    + "    \"@vaadin/common-frontend\": \"0.0.17\",\n"
                    + "    \"@vaadin/router\": \"1.7.4\",\n"
                    + "    \"construct-style-sheets-polyfill\": \"3.1.0\",\n"
                    + "    \"lit\": \"2.6.1\"\n" + " },\n"
                    + " \"entryScripts\": [\n"
                    + "  \"VAADIN/build/indexhtml-aa31f040.js\"\n" + " ],\n"
                    + " \"packageJsonHash\": \"" + defaultHash + "\"\n" + "}");

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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonList("@polymer/paper-checkbox/paper-checkbox.js"));

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
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

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonList("@polymer/paper-checkbox/paper-checkbox.js"));

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
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

    @Test
    public void themedGeneratedFlowImports_bundleUsesTheme_noBuildRequired()
            throws IOException {

        createPackageJsonStub("{\"dependencies\": {"
                + "\"@vaadin/router\": \"^1.7.4\"}, "
                + "\"vaadin\": { \"hash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonList("@vaadin/grid/src/vaadin-grid.js"));
        Mockito.when(depScanner.getTheme())
                .thenReturn(new NodeTestComponents.LumoTest());

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                    + " \"bundleImports\": [\n"
                    + "  \"@polymer/paper-checkbox/paper-checkbox.js\",\n"
                    + "  \"@polymer/paper-input/paper-input.js\",\n"
                    + "  \"@vaadin/grid/theme/lumo/vaadin-grid.js\",\n"
                    + "  \"Frontend/generated/jar-resources/dndConnector-es6.js\"\n"
                    + " ],\n"
                    + " \"packageJsonHash\": \"af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6\"\n"
                    + "}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "All themed imports in stats, no compilation required",
                    needsBuild);
        }

    }

    @Test
    public void frontendFileHashMatches_noBundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        createPackageJsonStub(
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"a5\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                    + " \"bundleImports\": [\n"
                    + "  \"Frontend/generated/jar-resources/TodoTemplate.js\"\n"
                    + " ],\n" + "\n" + " \"frontendHashes\": {\n"
                    + "  \"TodoTemplate.js\": \"dea5180dd21d2f18d1472074cd5305f60b824e557dae480fb66cdf3ea73edc65\",\n"
                    + " }," + " \"packageJsonHash\": \"a5\"\n" + "}");
            utils.when(
                    () -> FrontendUtils.getJarResourceString("TodoTemplate.js"))
                    .thenReturn(fileContent);

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("Jar fronted file content hash should match.",
                    needsBuild);
        }

    }

    @Test
    public void noFrontendFileHash_bundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        createPackageJsonStub(
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"a5\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                    + " \"bundleImports\": [\n"
                    + "  \"Frontend/generated/jar-resources/TodoTemplate.js\"\n"
                    + " ],\n" + "\n" + " \"frontendHashes\": {\n" + " },"
                    + " \"packageJsonHash\": \"a5\"\n" + "}");
            utils.when(
                    () -> FrontendUtils.getJarResourceString("TodoTemplate.js"))
                    .thenReturn(fileContent);

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("Jar fronted file content hash should match.",
                    needsBuild);
        }
    }

    @Test
    public void frontendFileHashMissmatch_bundleRebuild() throws IOException {
        String fileContent = "TodoContent2";

        createPackageJsonStub(
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"a5\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n" + " \"npmModules\": {\n"
                    + "  \"@vaadin/router\": \"1.8.6\"" + "},\n"
                    + " \"bundleImports\": [\n"
                    + "  \"Frontend/generated/jar-resources/TodoTemplate.js\"\n"
                    + " ],\n" + "\n" + " \"frontendHashes\": {\n"
                    + "  \"TodoTemplate.js\": \"dea5180dd21d2f18d1472074cd5305f60b824e557dae480fb66cdf3ea73edc65\",\n"
                    + " }," + " \"packageJsonHash\": \"a5\"\n" + "}");
            utils.when(
                    () -> FrontendUtils.getJarResourceString("TodoTemplate.js"))
                    .thenReturn(fileContent);

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("Jar fronted file content hash should match.",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_noReusedThemes_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, BLANK_STATS_JSON_WITH_PACKAGE_HASH);

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Shouldn't rebuild the bundle if no reused themes",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_newlyAddedTheme_noThemeJson_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        // create custom-theme folder with no theme.json
        File jarResourcesFolder = new File(temporaryFolder.getRoot(),
                "frontend/generated/jar-resources/themes/custom-theme");
        boolean created = jarResourcesFolder.mkdirs();
        Assert.assertTrue(created);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils,
                    "{\n \"npmModules\": {},\n" + " \"bundleImports\": [],\n"
                            + " \"frontendHashes\": {},\n"
                            + " \"packageJsonHash\": \"a5\"\n}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when the new theme has no theme.json",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_newlyAddedTheme_noAssets_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        mockThemeJson(
                "{\"lumoImports\":[\"typography\",\"color\",\"spacing\",\"badge\",\"utility\"]}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils,
                    "{\n \"npmModules\": {},\n" + " \"bundleImports\": [],\n"
                            + " \"frontendHashes\": {},\n"
                            + " \"packageJsonHash\": \"a5\"\n}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when the new theme has no assets",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_noPreviouslyAddedThemes_justAddedNewTheme_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        mockThemeJson("{\n" + "  \"assets\": {\n"
                + "    \"@my-asset/my-asset-subdir\": {\n"
                + "      \"my-asset-rules\": \"my-asset-target-folder\"\n"
                + "    }\n" + "  },\n" + "  \"hash\": \"custom-theme-hash\"\n"
                + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, BLANK_STATS_JSON_WITH_PACKAGE_HASH);

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should trigger a bundle rebuild when a new reusable theme is added",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_previouslyAddedThemes_justAddedNewTheme_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        mockThemeJson("{\n" + "  \"assets\": {\n"
                + "    \"@my-asset/my-asset-subdir\": {\n"
                + "      \"my-asset-rules\": \"my-asset-target-folder\"\n"
                + "    }\n" + "  },\n" + "  \"hash\": \"custom-theme-hash\"\n"
                + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils,
                    "{\n \"npmModules\": {},\n" + " \"bundleImports\": [],\n"
                            + " \"frontendHashes\": {},\n"
                            + " \"themeJsonHashes\": {\n"
                            + "   \"other-theme\": \"other-theme-hash\",\n"
                            + " },\n" + " \"packageJsonHash\": \"a5\"\n}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should trigger a bundle rebuild when a new reusable theme is added",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_previouslyAddedThemes_assetsUpdate_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        mockThemeJson("{\n" + "  \"assets\": {\n"
                + "    \"@my-asset/my-asset-subdir\": {\n"
                + "      \"my-asset-new-rules\": \"my-asset-target-folder\"\n"
                + "    }\n" + "  },\n"
                + "  \"hash\": \"custom-theme-new-hash\"\n" + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils, "{\n \"npmModules\": {},\n"
                    + " \"bundleImports\": [],\n" + " \"frontendHashes\": {},\n"
                    + " \"themeJsonHashes\": {\n"
                    + "   \"custom-theme\": \"custom-theme-old-hash\",\n"
                    + " },\n" + " \"packageJsonHash\": \"a5\"\n}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should trigger a bundle rebuild when the assets updated",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_previouslyAddedThemes_noUpdates_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        mockThemeJson("{\n" + "  \"assets\": {\n"
                + "    \"@my-asset/my-asset-subdir\": {\n"
                + "      \"my-asset-rules\": \"my-asset-target-folder\"\n"
                + "    }\n" + "  },\n" + "  \"hash\": \"custom-theme-hash\"\n"
                + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            mockStatsJson(utils,
                    "{\n \"npmModules\": {},\n" + " \"bundleImports\": [],\n"
                            + " \"frontendHashes\": {},\n"
                            + " \"themeJsonHashes\": {\n"
                            + "   \"custom-theme\": \"custom-theme-hash\",\n"
                            + " },\n" + " \"packageJsonHash\": \"a5\"\n}");

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when the themes not changed",
                    needsBuild);
        }
    }

    private void mockStatsJson(MockedStatic<FrontendUtils> utils,
            String statsJson) {
        utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        utils.when(() -> FrontendUtils
                .findBundleStatsJson(temporaryFolder.getRoot()))
                .thenReturn(statsJson);
    }

    private void createPackageJsonStub(String content) throws IOException {
        File packageJson = new File(temporaryFolder.getRoot(),
                Constants.PACKAGE_JSON);
        boolean created = packageJson.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(packageJson, content, StandardCharsets.UTF_8);
    }

    private void mockThemeJson(String content) throws IOException {
        File jarResourcesFolder = new File(temporaryFolder.getRoot(),
                "frontend/generated/jar-resources");
        boolean created = jarResourcesFolder.mkdirs();
        Assert.assertTrue(created);

        File themeJson = new File(jarResourcesFolder,
                "/themes/custom-theme/theme.json");
        FileUtils.createParentDirectories(themeJson);
        created = themeJson.createNewFile();
        Assert.assertTrue(created);

        FileUtils.write(themeJson, content, StandardCharsets.UTF_8);
        options.withJarFrontendResourcesFolder(jarResourcesFolder);
    }
}
