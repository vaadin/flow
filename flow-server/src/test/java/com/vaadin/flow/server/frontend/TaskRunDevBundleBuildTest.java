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
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TaskRunDevBundleBuildTest {

    public static final String BLANK_PACKAGE_JSON_WITH_HASH = "{\n \"dependencies\": {},"
            + "\"vaadin\": { \"hash\": \"a5\"} \n}";

    public static final String PACKAGE_JSON_DEPENDENCIES = "packageJsonDependencies";
    public static final String ENTRY_SCRIPTS = "entryScripts";
    public static final String BUNDLE_IMPORTS = "bundleImports";
    public static final String FRONTEND_HASHES = "frontendHashes";
    public static final String THEME_JSON_CONTENTS = "themeJsonContents";
    public static final String PACKAGE_JSON_HASH = "packageJsonHash";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Options options;

    ClassFinder finder;

    @Before
    public void init() {
        options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target");
        options.copyResources(Collections.emptySet());
        finder = Mockito.mock(ClassFinder.class);
    }

    private JsonObject getBasicStats() {
        JsonObject stats = Json.createObject();

        JsonObject packageJsonDependencies = Json.createObject();
        JsonObject frontendHashes = Json.createObject();
        JsonObject themeJsonContents = Json.createObject();

        JsonArray entryScripts = Json.createArray();
        JsonArray bundleImports = Json.createArray();

        stats.put(PACKAGE_JSON_DEPENDENCIES, packageJsonDependencies);
        stats.put(ENTRY_SCRIPTS, entryScripts);
        stats.put(BUNDLE_IMPORTS, bundleImports);
        stats.put(FRONTEND_HASHES, frontendHashes);
        stats.put(THEME_JSON_CONTENTS, themeJsonContents);
        stats.put(PACKAGE_JSON_HASH, "aHash");

        // Add default packageJson dependencies
        for (Map.Entry<String, String> dependency : NodeUpdater
                .getDefaultDependencies().entrySet()) {
            packageJsonDependencies.put(dependency.getKey(),
                    dependency.getValue());
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");

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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");

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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");

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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "1.0.0");

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
    public void packageJsonContainsOldVersion_versionsJsonUpdates_noCompilation()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.4\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        File versions = new File(temporaryFolder.getRoot(),
                Constants.VAADIN_CORE_VERSIONS_JSON);
        versions.createNewFile();
        // @formatter:off
        FileUtils.write(versions, "{"
                + "  \"core\": {\n"
                + "    \"vaadin-router\": {\n"
                + "      \"jsVersion\": \"2.0.3\",\n"
                + "      \"npmName\": \"@vaadin/router\",\n"
                + "      \"releasenotes\": true\n"
                + "    },"
                + "  },"
                + "  \"platform\": \"123-SNAPSHOT\""
                + "}");
        // @formatter:on

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "2.0.3");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            final boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "vaadin-core-versions.json should have updated version to expected.",
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.9.2");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "2.1.0");

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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "1.0.0");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");

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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");

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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.6");

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

            stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                    "1.8.1");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");

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

            stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                    "2.0.0");
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
    public void packageJsonHasOldPlatformDependencies_statsDoesNotHaveThem_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {\"@polymer/iron-list\": \"3.1.0\", "
                        + "\"@vaadin/vaadin-accordion\": \"23.3.7\"}, "
                        + "\"vaadin\": { \"dependencies\": {"
                        + "\"@polymer/iron-list\": \"3.1.0\", "
                        + "\"@vaadin/vaadin-accordion\": \"23.3.7\"}, "
                        + "\"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/accordion",
                "24.0.0.beta2");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse("No compilation expected if package.json has "
                    + "only dependencies from older Vaadin version not "
                    + "presenting in a newer version", needsBuild);
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "1.0.0");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.4");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
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
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
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

    @Test
    public void cssImportWithInline_statsAndImportsMatchAndNoBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File stylesheetFile = new File(temporaryFolder.getRoot(),
                "frontend/my-styles.css");
        FileUtils.forceMkdir(stylesheetFile.getParentFile());
        boolean created = stylesheetFile.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(stylesheetFile, "body{color:yellow}",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonList("Frontend/my-styles.css?inline"));

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0, "Frontend/my-styles.css");
        stats.getObject(FRONTEND_HASHES).put("my-styles.css",
                "0d94fe659d24e1e56872b47fc98d9f09227e19816c62a3db709bad347fbd0cdd");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(
                    () -> FrontendUtils.getJarResourceString("my-styles.css"))
                    .thenReturn("body{color:yellow}");
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "CSS 'inline' suffix should be ignored for imports checking",
                    needsBuild);
        }
    }

    @Test
    public void projectFrontendFileChange_bundleRebuild() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectFrontendFileStub();

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonList("Frontend/views/lit-view.ts"));

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0, "Frontend/views/lit-view.ts");
        stats.getObject(FRONTEND_HASHES).put("views/lit-view.ts", "old_hash");

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
                    "Project frontend file change should trigger rebuild",
                    needsBuild);
        }
    }

    @Test
    public void projectFrontendFileNotChanged_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectFrontendFileStub();

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonList("Frontend/views/lit-view.ts"));

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0, "Frontend/views/lit-view.ts");
        stats.getObject(FRONTEND_HASHES).put("views/lit-view.ts",
                "eaf04adbc43cb363f6b58c45c6e0e8151084941247abac9493beed8d29f08add");

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
                    "No bundle rebuild expected when no changes in frontend file",
                    needsBuild);
        }
    }

    @Test
    public void projectFrontendFileDeleted_bundleRebuild() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonList("Frontend/views/lit-view.ts"));

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0, "Frontend/views/lit-view.ts");
        stats.getObject(FRONTEND_HASHES).put("views/lit-view.ts",
                "eaf04adbc43cb363f6b58c45c6e0e8151084941247abac9493beed8d29f08add");

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
                    "Project frontend file delete should trigger rebuild",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_noReusedThemes_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject basicStats = getBasicStats();
        basicStats.remove(THEME_JSON_CONTENTS);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(basicStats.toJson());

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
        File jarWithTheme = TestUtils.getTestJar("jar-with-no-theme-json.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        // create custom-theme folder with no theme.json
        File jarResourcesFolder = new File(temporaryFolder.getRoot(),
                "frontend/generated/jar-resources/themes/custom-theme");
        boolean created = jarResourcesFolder.mkdirs();
        Assert.assertTrue(created);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(getBasicStats().toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when the new theme has no theme.json",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_noPreviouslyAddedThemes_justAddedNewTheme_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File jarWithTheme = TestUtils
                .getTestJar("jar-with-theme-json-and-assets.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(getBasicStats().toJson());

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

        File jarWithTheme = TestUtils
                .getTestJar("jar-with-theme-json-and-assets.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put("other-theme",
                "other-theme-hash");

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
                    "Should trigger a bundle rebuild when a new reusable theme is added",
                    needsBuild);
        }
    }

    @Test
    public void reusedTheme_previouslyAddedThemes_assetsUpdate_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        // Jar with 'line-awesome' assets
        File jarWithTheme = TestUtils
                .getTestJar("jar-with-theme-json-and-assets.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("reusable-theme", "{\n"
                    + "  \"importCss\": [\"@fortawesome/fontawesome-free/css/all.min.css\"],\n"
                    + "  \"assets\": {\n"
                    + "    \"@fortawesome/fontawesome-free\": {\n"
                    + "      \"svgs/brands/**\": \"fontawesome/svgs/brands\",\n"
                    + "      \"webfonts/**\": \"webfonts\"\n" + "    }\n"
                    + "  }\n" + "}");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

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

        File jarWithTheme = TestUtils
                .getTestJar("jar-with-theme-json-and-assets.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("reusable-theme", "{\n"
                    + "  \"importCss\": [\"@fortawesome/fontawesome-free/css/all.min.css\"],\n"
                    + "  \"assets\": {\n"
                    + "    \"@fortawesome/fontawesome-free\": {\n"
                    + "      \"svgs/brands/**\": \"fontawesome/svgs/brands\",\n"
                    + "      \"webfonts/**\": \"webfonts\"\n" + "    },\n"
                    + "    \"line-awesome\": {\n"
                    + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                    + "      \"dist/line-awesome/fonts/**\": \"line-awesome/dist/line-awesome/fonts\"\n"
                    + "    }\n" + "  }\n" + "}\n");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when the themes not changed",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_statsHasNoThemeJson_projectHasThemeJson_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"lumoImports\": [\"typography\"]}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.remove(THEME_JSON_CONTENTS);

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should trigger a bundle rebuild when no themeJsonContents, but project has theme.json",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_containsParentTheme_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"parent\": \"my-parent-theme\"}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("vaadin-dev-bundle",
                    "{\"lumoImports\": [\"typography\"]}");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when parent theme is used",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_statsHasThemeJson_projectHasNoThemeJson_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("vaadin-dev-bundle",
                    "{\"lumoImports\": [\"typography\"]}");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when project has no theme.json",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_statsAndProjectThemeJsonEquals_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\n" + "  \"boolean-property\": true,\n"
                + "  \"numeric-property\": 42.42,\n"
                + "  \"string-property\": \"foo\",\n"
                + "  \"array-property\": [\"one\", \"two\"],\n"
                + "  \"object-property\": { \"foo\": \"bar\" }\n" + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("my-theme",
                    "{\n\n\n\n\n\n" + "  \"boolean-property\": true,\n"
                            + "  \"numeric-property\": 42.42,\n"
                            + "  \"string-property\": \"foo\",\n"
                            + "  \"array-property\": [\"one\", \"two\"],\n"
                            + "  \"object-property\": { \"foo\": \"bar\" }\n"
                            + "}");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Should not trigger a bundle rebuild when project theme.json has the same content as in the bundle",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_bundleMissesSomeEntries_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\n"
                + "  \"importCss\": [\"@fortawesome/fontawesome-free/css/all.css\"],"
                + "  \"assets\": {\n" + "    \"line-awesome\": {\n"
                + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                + "    }\n" + "  }\n" + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("vaadin-dev-bundle", "{\n"
                    + "  \"lumoImports\": [\"typography\", \"color\", \"spacing\", \"badge\", \"utility\"],\n"
                    + "  \"assets\": {\n" + "    \"line-awesome\": {\n"
                    + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                    + "      \"dist/line-awesome/fonts/**\": \"line-awesome/dist/line-awesome/fonts\"\n"
                    + "    }\n" + "  }\n" + "}");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should rebuild when project theme.json adds extra entries",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_bundleHaveAllEntriesAndMore_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\n"
                + "  \"lumoImports\": [\"typography\", \"color\", \"spacing\", \"badge\", \"utility\"]\n"
                + "}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();
            stats.getObject(THEME_JSON_CONTENTS).put("vaadin-dev-bundle", "{\n"
                    + "  \"lumoImports\": [\"typography\", \"color\", \"spacing\", \"badge\", \"utility\"],\n"
                    + "  \"assets\": {\n" + "    \"line-awesome\": {\n"
                    + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                    + "      \"dist/line-awesome/fonts/**\": \"line-awesome/dist/line-awesome/fonts\"\n"
                    + "    }\n" + "  }\n" + "}");

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertFalse(
                    "Shouldn't re-bundle when the dev bundle already have all"
                            + " the entries defined in the project's theme.json",
                    needsBuild);
        }
    }

    @Test
    public void themeJsonUpdates_noProjectThemeHashInStats_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"lumoImports\": [\"typography\"]}");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            JsonObject stats = getBasicStats();

            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should trigger a bundle rebuild when project has theme"
                            + ".json but stats doesn't",
                    needsBuild);
        }
    }

    @Test
    public void indexTsAdded_rebuildRequired() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = temporaryFolder.newFolder(FrontendUtils.FRONTEND);

        File indexTs = new File(frontendFolder, FrontendUtils.INDEX_TS);
        indexTs.createNewFile();

        FileUtils.write(indexTs, "window.alert('');", StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue("Adding 'index.ts' should require bundling",
                    needsBuild);
        }
    }

    @Test
    public void changeInIndexTs_rebuildRequired() throws IOException {
        createPackageJsonStub("{\"dependencies\": {}, "
                + "\"vaadin\": { \"hash\": \"aHash\"} }");
        File frontendFolder = temporaryFolder.newFolder(FrontendUtils.FRONTEND);

        File indexTs = new File(frontendFolder, FrontendUtils.INDEX_TS);
        indexTs.createNewFile();

        FileUtils.write(indexTs, "window.alert('');", StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();
        stats.getObject(FRONTEND_HASHES).put(FrontendUtils.INDEX_TS,
                "15931fa8c20e3c060c8ea491831e95cc8463962700a9bfb82c8e3844cf608f04");

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
                    "'index.ts' equal content should not require bundling",
                    needsBuild);

            FileUtils.write(indexTs, "window.alert('hello');",
                    StandardCharsets.UTF_8);

            needsBuild = TaskRunDevBundleBuild.needsBuildInternal(options,
                    depScanner, finder);
            Assert.assertTrue(
                    "changed content for 'index.ts' should require bundling",
                    needsBuild);
        }
    }

    @Test
    public void indexTsDeleted_rebuildRequired() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();
        stats.getObject(FRONTEND_HASHES).put(FrontendUtils.INDEX_TS,
                "15931fa8c20e3c060c8ea491831e95cc8463962700a9bfb82c8e3844cf608f04");

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue("'index.ts' delete should require re-bundling",
                    needsBuild);
        }
    }

    @Test
    public void standardVaadinComponent_notAddedToProjectAsJar_noRebuildRequired()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0,
                "Frontend/generated/jar-resources/vaadin-spreadsheet/vaadin-spreadsheet.js");
        stats.getObject(FRONTEND_HASHES).put(
                "vaadin-spreadsheet/vaadin-spreadsheet.js",
                "e545ad23a2d1d4b3a3370a0305dd71c15bbfc645216f50c6e327bd818b7484c4");

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
                    "Should not require bundling if component JS is missing in jar-resources",
                    needsBuild);
        }
    }

    @Test
    public void cssImport_cssInMetaInfResources_notThrow_bundleRequired()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        CssData cssData = new CssData("./addons-styles/my-styles.css", null,
                null, null);

        Mockito.when(depScanner.getCss())
                .thenReturn(Collections.singleton(cssData));

        JsonObject stats = getBasicStats();

        try (MockedStatic<FrontendUtils> utils = Mockito
                .mockStatic(FrontendUtils.class)) {
            utils.when(() -> FrontendUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            utils.when(() -> FrontendUtils
                    .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenReturn(stats.toJson());

            // Should not throw an IllegalStateException:
            // "Failed to find the following css files in the...."
            boolean needsBuild = TaskRunDevBundleBuild
                    .needsBuildInternal(options, depScanner, finder);
            Assert.assertTrue(
                    "Should re-bundle if CSS is imported from META-INF/resources",
                    needsBuild);
        }
    }

    private void createPackageJsonStub(String content) throws IOException {
        File packageJson = new File(temporaryFolder.getRoot(),
                Constants.PACKAGE_JSON);
        boolean created = packageJson.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(packageJson, content, StandardCharsets.UTF_8);
    }

    private void createProjectThemeJsonStub(String content) throws IOException {
        File themeJson = new File(temporaryFolder.getRoot(),
                "frontend/themes/my-theme/theme.json");
        FileUtils.forceMkdir(themeJson.getParentFile());
        boolean created = themeJson.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(themeJson, content, StandardCharsets.UTF_8);
    }

    private void createProjectFrontendFileStub() throws IOException {
        File frontendFile = new File(temporaryFolder.getRoot(),
                "frontend/views/lit-view.ts");
        FileUtils.forceMkdir(frontendFile.getParentFile());
        boolean created = frontendFile.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(frontendFile, "Some codes", StandardCharsets.UTF_8);
    }
}
