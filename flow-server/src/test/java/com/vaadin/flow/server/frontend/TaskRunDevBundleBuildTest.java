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

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TaskRunDevBundleBuildTest {

    public static final String NPM_MODULES = "npmModules";
    public static final String ENTRY_SCRIPTS = "entryScripts";
    public static final String BUNDLE_IMPORTS = "bundleImports";
    public static final String FRONTEND_HASHES = "frontendHashes";
    public static final String PACKAGE_JSON_HASH = "packageJsonHash";
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

    private JsonObject getBasicStats() {
        JsonObject stats = Json.createObject();

        JsonObject npmModules = Json.createObject();
        JsonObject frontendHashes = Json.createObject();

        JsonArray entryScripts = Json.createArray();
        JsonArray bundleImports = Json.createArray();

        stats.put(NPM_MODULES, npmModules);
        stats.put(ENTRY_SCRIPTS, entryScripts);
        stats.put(BUNDLE_IMPORTS, bundleImports);
        stats.put(FRONTEND_HASHES, frontendHashes);
        stats.put(PACKAGE_JSON_HASH, "aHash");

        // Add default npmModules
        for (Map.Entry<String, String> dependency : NodeUpdater
                .getDefaultDependencies().entrySet()) {
            npmModules.put(dependency.getKey(), dependency.getValue());
        }

        return stats;
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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.7.4");
        packages.put("@vaadin/text", "1.0.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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
                + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");
        stats.getObject(NPM_MODULES).put("@vaadin/text", "1.0.0");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Not missing npmPackage in stats.json should not require compilation",
                    needsBuild);
        }
    }

    @Test
    public void packageJsonContainsOldVersionsAfterVersionUpdate_updatedStatsMatches_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.4\",\n"
                        + "\"@vaadin/text\": \"1.0.0\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.9.2");
        packages.put("@vaadin/text", "2.1.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.9.2");
        stats.getObject(NPM_MODULES).put("@vaadin/text", "2.1.0");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Not missing npmPackage in stats.json should not require compilation",
                    needsBuild);
        }
    }

    @Test
    public void noPackageJsonHashAfterCleanFrontend_statsHasDefaultJsonPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\n" + "  \"name\": \"no-name\",\n"
                + "  \"license\": \"UNLICENSED\",\n" + "  \"dependencies\": {\n"
                + "    \"@vaadin/router\": \"1.7.4\"" + "  },\n"
                + "  \"devDependencies\": {\n" + "  }\n" + "}",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");
        stats.getObject(NPM_MODULES).put("@vaadin/text", "1.0.0");
        stats.put(PACKAGE_JSON_HASH,
                "af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6");
        stats.getArray(ENTRY_SCRIPTS).set(0,
                "VAADIN/build/indexhtml-aa31f040.js");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Not missing npmPackage in stats.json should not require compilation",
                    needsBuild);
        }
    }

    @Test
    public void noPackageJsonHashAfterCleanFrontend_statsMissingDefaultJsonPackages_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\n" + "  \"name\": \"no-name\",\n"
                + "  \"license\": \"UNLICENSED\",\n" + "  \"dependencies\": {\n"
                + "    \"@vaadin/router\": \"1.7.4\"" + "  },\n"
                + "  \"devDependencies\": {\n" + "  }\n" + "}",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Missing npmPackage in stats.json should require compilation",
                    needsBuild);
        }
    }

    @Test
    public void hashesMatch_packageJsonHasRange_statsHasFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"~1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.6");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "No compilation if tilde range only patch update",
                    needsBuild);

            stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.1");
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "No compilation if caret range only minor version update",
                    needsBuild);

            stats.getObject(NPM_MODULES).put("@vaadin/router", "2.0.0");
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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
                .getDefaultPackageJson(options, depScanner, finder, null)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");
        stats.getObject(NPM_MODULES).put("@vaadin/text", "1.0.0");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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
                .getDefaultPackageJson(options, depScanner, finder, null)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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
                .getDefaultPackageJson(options, depScanner, finder, null)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.7.4");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonList("@polymer/paper-checkbox/paper-checkbox.js"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(0,
                "@Frontend/generated/jar-resources/dndConnector-es6.js");
        bundleImports.set(1, "@polymer/paper-input/paper-input.js");
        bundleImports.set(2, "@vaadin/common-frontend/ConnectionIndicator.js");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonList("@polymer/paper-checkbox/paper-checkbox.js"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(0, "@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.set(1, "@polymer/paper-input/paper-input.js");
        bundleImports.set(2, "@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports.set(3,
                "Frontend/generated/jar-resources/dndConnector-es6.js");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("All imports in stats, no compilation required",
                    needsBuild);
        }

    }

    @Test
    public void themedGeneratedFlowImports_bundleUsesTheme_noBuildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonList("@vaadin/grid/src/vaadin-grid.js"));
        Mockito.when(depScanner.getTheme())
                .thenReturn(new NodeTestComponents.LumoTest());

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(0, "@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.set(1, "@polymer/paper-input/paper-input.js");
        bundleImports.set(2, "@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports.set(3,
                "Frontend/generated/jar-resources/dndConnector-es6.js");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");
        stats.getArray(BUNDLE_IMPORTS).set(0,
                "Frontend/generated/jar-resources/TodoTemplate.js");
        stats.getObject(FRONTEND_HASHES).put("TodoTemplate.js",
                "dea5180dd21d2f18d1472074cd5305f60b824e557dae480fb66cdf3ea73edc65");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(
                    () -> FrontendUtils.getJarResourceString("TodoTemplate.js"))
                    .thenReturn(fileContent);
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("Jar fronted file content hash should match.",
                    needsBuild);
        }

    }

    @Test
    public void noFrontendFileHash_bundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");
        stats.getArray(BUNDLE_IMPORTS).set(0,
                "Frontend/generated/jar-resources/TodoTemplate.js");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(
                    () -> FrontendUtils.getJarResourceString("TodoTemplate.js"))
                    .thenReturn(fileContent);
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue("Content should not have been validated.",
                    needsBuild);
        }
    }

    @Test
    public void frontendFileHashMissmatch_bundleRebuild() throws IOException {
        String fileContent = "TodoContent2";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js"));

        JsonObject stats = getBasicStats();
        stats.getObject(NPM_MODULES).put("@vaadin/router", "1.8.6");
        stats.getArray(BUNDLE_IMPORTS).set(0,
                "Frontend/generated/jar-resources/TodoTemplate.js");
        stats.getObject(FRONTEND_HASHES).put("TodoTemplate.js",
                "dea5180dd21d2f18d1472074cd5305f60b824e557dae480fb66cdf3ea73edc65");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(
                    () -> FrontendUtils.getJarResourceString("TodoTemplate.js"))
                    .thenReturn(fileContent);
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Jar fronted file content hash should not be a match.",
                    needsBuild);
        }
    }
}
