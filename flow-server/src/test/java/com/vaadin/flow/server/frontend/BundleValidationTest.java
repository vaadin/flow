package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

@RunWith(Parameterized.class)
public class BundleValidationTest {

    public static final String BLANK_PACKAGE_JSON_WITH_HASH = "{\n \"dependencies\": {},"
            + "\"vaadin\": { \"hash\": \"a5\"} \n}";

    public static final String PACKAGE_JSON_DEPENDENCIES = "packageJsonDependencies";
    public static final String ENTRY_SCRIPTS = "entryScripts";
    public static final String BUNDLE_IMPORTS = "bundleImports";
    public static final String FRONTEND_HASHES = "frontendHashes";
    public static final String THEME_JSON_CONTENTS = "themeJsonContents";
    public static final String PACKAGE_JSON_HASH = "packageJsonHash";

    private static final String THEME_UTIL_JS;
    static {
        try {
            THEME_UTIL_JS = IOUtils.toString(
                    BundleValidationTest.class.getClassLoader()
                            .getResourceAsStream(
                                    "META-INF/frontend/theme-util.js"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

    }

    @Parameterized.Parameters
    public static Collection<Mode> modes() {
        return List.of(Mode.PRODUCTION_PRECOMPILED_BUNDLE,
                Mode.DEVELOPMENT_BUNDLE);
    }

    @Parameterized.Parameter
    public Mode mode;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Options options;

    ClassFinder finder;

    private Map<String, String> jarResources = new HashMap<>();

    private MockedStatic<FrontendUtils> frontendUtils;

    private MockedStatic<DevBundleUtils> devBundleUtils;

    private MockedStatic<BundleValidationUtil> bundleUtils;

    private String bundleLocation;

    @Before
    public void init() {
        options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target");
        options.copyResources(Collections.emptySet());
        options.withProductionMode(mode.isProduction());
        bundleLocation = mode.isProduction() ? Constants.PROD_BUNDLE_NAME
                : Constants.DEV_BUNDLE_NAME;
        finder = Mockito.mock(ClassFinder.class);
        frontendUtils = Mockito.mockStatic(FrontendUtils.class,
                Mockito.CALLS_REAL_METHODS);
        devBundleUtils = Mockito.mockStatic(DevBundleUtils.class,
                Mockito.CALLS_REAL_METHODS);
        bundleUtils = Mockito.mockStatic(BundleValidationUtil.class,
                Mockito.CALLS_REAL_METHODS);
    }

    @After
    public void teardown() {
        frontendUtils.close();
        devBundleUtils.close();
        bundleUtils.close();
        File needsBuildFile = new File(options.getResourceOutputDirectory(),
                Constants.NEEDS_BUNDLE_BUILD_FILE);
        if (needsBuildFile.exists()) {
            needsBuildFile.delete();
        }
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

        bundleImports.set(bundleImports.length(),
                "./generated/jar-resources/theme-util.js");
        frontendHashes.put("theme-util.js",
                BundleValidationUtil.calculateHash(THEME_UTIL_JS));
        jarResources.put("theme-util.js", THEME_UTIL_JS);
        return stats;
    }

    @Test
    public void noDevBundle_bundleCompilationRequires() {
        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), finder, mode);
        Assert.assertTrue("Bundle should require creation if not available",
                needsBuild);
    }

    @Test
    public void devBundleStatsJsonMissing_bundleCompilationRequires() {
        devBundleUtils
                .when(() -> DevBundleUtils.getDevBundleFolder(Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        devBundleUtils
                .when(() -> DevBundleUtils
                        .findBundleStatsJson(temporaryFolder.getRoot()))
                .thenReturn(null);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), finder, mode);
        Assert.assertTrue("Missing stats.json should require bundling",
                needsBuild);
    }

    @Test
    public void hashesMatch_noNpmPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("Matching hashes should not require compilation",
                needsBuild);
    }

    @Test
    public void loadDependenciesOnStartup_annotatedClassInProject_compilationRequiredForProduction()
            throws IOException {
        Assume.assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        Mockito.when(
                finder.getAnnotatedClasses(LoadDependenciesOnStartup.class))
                .thenReturn(Collections.singleton(AllEagerAppConf.class));

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "'LoadDependenciesOnStartup' annotation requires build",
                needsBuild);
    }

    @Test
    public void hashesMatch_statsMissingNpmPackages_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.7.5");
        packages.put("@vaadin/text", "1.0.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Missing npmPackage should require bundling",
                needsBuild);
    }

    @Test
    public void hashesMatch_statsMissingPackageJsonPackage_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.5\", \"@vaadin/text\":\"1.0.0\"}, "
                + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Bundle missing module dependency should rebuild",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonMissingNpmPackages_statsHasJsonPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "1.0.0");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void packageJsonContainsOldVersion_versionsJsonUpdates_noCompilation()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.5\"}, "
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

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "vaadin-core-versions.json should have updated version to expected.",
                needsBuild);
    }

    @Test
    public void packageJsonContainsOldVersionsAfterVersionUpdate_updatedStatsMatches_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"1.7.5\",\n"
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

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void noPackageJsonHashAfterCleanFrontend_statsHasDefaultJsonPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\n" + "  \"name\": \"no-name\",\n"
                + "  \"license\": \"UNLICENSED\",\n" + "  \"dependencies\": {\n"
                + "    \"@vaadin/router\": \"1.7.5\"" + "  },\n"
                + "  \"devDependencies\": {\n" + "  }\n" + "}",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "1.0.0");
        stats.put(PACKAGE_JSON_HASH,
                "af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6");
        stats.getArray(ENTRY_SCRIPTS).set(0,
                "VAADIN/build/indexhtml-aa31f040.js");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void noPackageJsonHashAfterCleanFrontend_statsMissingDefaultJsonPackages_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\n" + "  \"name\": \"no-name\",\n"
                + "  \"license\": \"UNLICENSED\",\n" + "  \"dependencies\": {\n"
                + "    \"@vaadin/router\": \"1.7.5\"" + "  },\n"
                + "  \"devDependencies\": {\n" + "  }\n" + "}",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Missing npmPackage in stats.json should require compilation",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonHasRange_statsHasFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonHasTildeRange_statsHasNewerFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"~1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.6");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("No compilation if tilde range only patch update",
                needsBuild);

        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.1");
        setupFrontendUtilsMock(stats);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner,
                finder, mode);
        Assert.assertTrue(
                "Compilation required if minor version change for tilde range",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonHasCaretRange_statsHasNewerFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "No compilation if caret range only minor version update",
                needsBuild);

        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "2.0.0");
        setupFrontendUtilsMock(stats);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner,
                finder, mode);
        Assert.assertTrue(
                "Compilation required if major version change for caret range",
                needsBuild);
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

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("No compilation expected if package.json has "
                + "only dependencies from older Vaadin version not "
                + "presenting in a newer version", needsBuild);
    }

    @Test
    public void noPackageJson_defaultPackagesAndModulesInStats_noBuildNeeded() {
        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        String defaultHash = BundleValidationUtil
                .getDefaultPackageJson(options, depScanner, finder, null)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/text", "1.0.0");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("Default package.json should be built and validated",
                needsBuild);
    }

    @Test
    public void noPackageJson_defaultPackagesInStats_missingNpmModules_buildNeeded() {
        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        String defaultHash = BundleValidationUtil
                .getDefaultPackageJson(options, depScanner, finder, null)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Missing NpmPackage with default bundle should require rebuild",
                needsBuild);
    }

    @Test
    public void noPackageJson_defaultPackagesInStats_noBuildNeeded() {
        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        String defaultHash = BundleValidationUtil
                .getDefaultPackageJson(options, depScanner, finder, null)
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getString(NodeUpdater.HASH_KEY);

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("Default package.json should be built and validated",
                needsBuild);
    }

    @Test
    public void generatedFlowImports_bundleMissingImports_buildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(0,
                "@Frontend/generated/jar-resources/dndConnector-es6.js");
        bundleImports.set(1, "@polymer/paper-input/paper-input.js");
        bundleImports.set(2, "@vaadin/common-frontend/ConnectionIndicator.js");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Compilation required as stats.json missing import",
                needsBuild);
    }

    @Test
    public void generatedFlowImports_bundleHasAllImports_noBuildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(bundleImports.length(),
                "@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.set(bundleImports.length(),
                "@polymer/paper-input/paper-input.js");
        bundleImports.set(bundleImports.length(),
                "@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports.set(bundleImports.length(),
                "Frontend/generated/jar-resources/dndConnector-es6.js");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("All imports in stats, no compilation required",
                needsBuild);
    }

    @Test
    public void themedGeneratedFlowImports_bundleUsesTheme_noBuildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonMap(ChunkInfo.GLOBAL, Collections
                        .singletonList("@vaadin/grid/src/vaadin-grid.js")));
        Mockito.when(depScanner.getTheme())
                .thenReturn(new NodeTestComponents.LumoTest());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(bundleImports.length(),
                "@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.set(bundleImports.length(),
                "@polymer/paper-input/paper-input.js");
        bundleImports.set(bundleImports.length(),
                "@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports.set(bundleImports.length(),
                "Frontend/generated/jar-resources/dndConnector-es6.js");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "All themed imports in stats, no compilation required",
                needsBuild);
    }

    @Test
    public void frontendFileHashMatches_noBundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonMap(ChunkInfo.GLOBAL, Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js")));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(bundleImports.length(),
                "./generated/jar-resources/TodoTemplate.js");
        stats.getObject(FRONTEND_HASHES).put("TodoTemplate.js",
                BundleValidationUtil.calculateHash(fileContent));
        jarResources.put("TodoTemplate.js", fileContent);

        setupFrontendUtilsMock(stats);
        devBundleUtils
                .when(() -> DevBundleUtils.getDevBundleFolder(Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("Jar fronted file content hash should match.",
                needsBuild);
    }

    @Test
    public void noFrontendFileHash_bundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonMap(ChunkInfo.GLOBAL, Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js")));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
        stats.getArray(BUNDLE_IMPORTS).set(0,
                "Frontend/generated/jar-resources/TodoTemplate.js");

        devBundleUtils
                .when(() -> DevBundleUtils.getDevBundleFolder(Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        frontendUtils
                .when(() -> FrontendUtils.getJarResourceString(
                        Mockito.eq("TodoTemplate.js"),
                        Mockito.any(ClassFinder.class)))
                .thenReturn(fileContent);
        devBundleUtils
                .when(() -> DevBundleUtils
                        .findBundleStatsJson(temporaryFolder.getRoot()))
                .thenReturn(stats.toJson());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Content should not have been validated.",
                needsBuild);
    }

    @Test
    public void frontendFileHashMissmatch_bundleRebuild() throws IOException {
        String fileContent = "TodoContent2";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"@vaadin/router\": \"^1.7.5\"}, "
                        + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonMap(ChunkInfo.GLOBAL, Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js")));

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.8.6");
        stats.getArray(BUNDLE_IMPORTS).set(0,
                "Frontend/generated/jar-resources/TodoTemplate.js");
        stats.getObject(FRONTEND_HASHES).put("TodoTemplate.js",
                "dea5180dd21d2f18d1472074cd5305f60b824e557dae480fb66cdf3ea73edc65");

        devBundleUtils
                .when(() -> DevBundleUtils.getDevBundleFolder(Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        frontendUtils
                .when(() -> FrontendUtils.getJarResourceString(
                        Mockito.eq("TodoTemplate.js"),
                        Mockito.any(ClassFinder.class)))
                .thenReturn(fileContent);
        devBundleUtils
                .when(() -> DevBundleUtils
                        .findBundleStatsJson(temporaryFolder.getRoot()))
                .thenReturn(stats.toJson());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Jar fronted file content hash should not be a match.",
                needsBuild);
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
                Collections.singletonMap(ChunkInfo.GLOBAL, Collections
                        .singletonList("Frontend/my-styles.css?inline")));

        JsonObject stats = getBasicStats();
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(bundleImports.length(), "Frontend/my-styles.css");
        stats.getObject(FRONTEND_HASHES).put("my-styles.css",
                "0d94fe659d24e1e56872b47fc98d9f09227e19816c62a3db709bad347fbd0cdd");

        setupFrontendUtilsMock(stats);
        jarResources.put("my-styles.css", "body{color:yellow}");

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "CSS 'inline' suffix should be ignored for imports checking",
                needsBuild);
    }

    @Test
    public void projectFrontendFileChange_bundleRebuild() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectFrontendFileStub();

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonMap(ChunkInfo.GLOBAL, Collections
                        .singletonList("Frontend/views/lit-view.ts")));

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0, "Frontend/views/lit-view.ts");
        stats.getObject(FRONTEND_HASHES).put("views/lit-view.ts", "old_hash");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Project frontend file change should trigger rebuild",
                needsBuild);
    }

    @Test
    public void projectFrontendFileNotChanged_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectFrontendFileStub();

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonMap(ChunkInfo.GLOBAL, Collections
                        .singletonList("Frontend/views/lit-view.ts")));

        JsonObject stats = getBasicStats();
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(bundleImports.length(), "Frontend/views/lit-view.ts");
        stats.getObject(FRONTEND_HASHES).put("views/lit-view.ts",
                "eaf04adbc43cb363f6b58c45c6e0e8151084941247abac9493beed8d29f08add");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "No bundle rebuild expected when no changes in frontend file",
                needsBuild);
    }

    @Test
    public void projectFrontendFileDeleted_bundleRebuild() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonMap(ChunkInfo.GLOBAL, Collections
                        .singletonList("Frontend/views/lit-view.ts")));

        JsonObject stats = getBasicStats();
        stats.getArray(BUNDLE_IMPORTS).set(0, "Frontend/views/lit-view.ts");
        stats.getObject(FRONTEND_HASHES).put("views/lit-view.ts",
                "eaf04adbc43cb363f6b58c45c6e0e8151084941247abac9493beed8d29f08add");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Project frontend file delete should trigger rebuild",
                needsBuild);
    }

    @Test
    public void reusedTheme_noReusedThemes_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();
        stats.remove(THEME_JSON_CONTENTS);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("Shouldn't rebuild the bundle if no reused themes",
                needsBuild);
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

        setupFrontendUtilsMock(getBasicStats());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when the new theme has no theme.json",
                needsBuild);
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

        setupFrontendUtilsMock(getBasicStats());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should trigger a bundle rebuild when a new reusable theme is added",
                needsBuild);
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

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should trigger a bundle rebuild when a new reusable theme is added",
                needsBuild);
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

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put("reusable-theme", "{\n"
                + "  \"importCss\": [\"@fortawesome/fontawesome-free/css/all.min.css\"],\n"
                + "  \"assets\": {\n"
                + "    \"@fortawesome/fontawesome-free\": {\n"
                + "      \"svgs/brands/**\": \"fontawesome/svgs/brands\",\n"
                + "      \"webfonts/**\": \"webfonts\"\n" + "    }\n" + "  }\n"
                + "}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should trigger a bundle rebuild when the assets updated",
                needsBuild);
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

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when the themes not changed",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_statsHasNoThemeJson_projectHasThemeJson_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"lumoImports\": [\"typography\"]}",
                "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();
        stats.remove(THEME_JSON_CONTENTS);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should trigger a bundle rebuild when no themeJsonContents, but project has theme.json",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_containsParentTheme_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"parent\": \"my-parent-theme\"}",
                "my-theme");
        new File(temporaryFolder.getRoot(), "frontend/themes/my-parent-theme")
                .mkdirs();

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put(bundleLocation,
                "{\"lumoImports\": [\"typography\"]}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when parent theme is used",
                needsBuild);
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
        new File(temporaryFolder.getRoot(), "frontend/themes/my-theme")
                .mkdirs();

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put(bundleLocation,
                "{\"lumoImports\": [\"typography\"]}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when project has no theme.json",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_statsAndProjectThemeJsonEquals_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub(
                "{\n" + "  \"boolean-property\": true,\n"
                        + "  \"numeric-property\": 42.42,\n"
                        + "  \"string-property\": \"foo\",\n"
                        + "  \"array-property\": [\"one\", \"two\"],\n"
                        + "  \"object-property\": { \"foo\": \"bar\" }\n" + "}",
                "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put("my-theme",
                "{\n\n\n\n\n\n" + "  \"boolean-property\": true,\n"
                        + "  \"numeric-property\": 42.42,\n"
                        + "  \"string-property\": \"foo\",\n"
                        + "  \"array-property\": [\"one\", \"two\"],\n"
                        + "  \"object-property\": { \"foo\": \"bar\" }\n"
                        + "}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when project theme.json has the same content as in the bundle",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_bundleMissesSomeEntries_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\n"
                + "  \"importCss\": [\"@fortawesome/fontawesome-free/css/all.css\"],"
                + "  \"assets\": {\n" + "    \"line-awesome\": {\n"
                + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                + "    }\n  }\n}", "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put(bundleLocation, "{\n"
                + "  \"lumoImports\": [\"typography\", \"color\", \"spacing\", \"badge\", \"utility\"],\n"
                + "  \"assets\": {\n" + "    \"line-awesome\": {\n"
                + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                + "      \"dist/line-awesome/fonts/**\": \"line-awesome/dist/line-awesome/fonts\"\n"
                + "    }\n" + "  }\n" + "}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should rebuild when project theme.json adds extra entries",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_bundleHaveAllEntriesAndMore_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\n"
                + "  \"lumoImports\": [\"typography\", \"color\", \"spacing\", \"badge\", \"utility\"]\n"
                + "}", "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put(bundleLocation, "{\n"
                + "  \"lumoImports\": [\"typography\", \"color\", \"spacing\", \"badge\", \"utility\"],\n"
                + "  \"assets\": {\n" + "    \"line-awesome\": {\n"
                + "      \"dist/line-awesome/css/**\": \"line-awesome/dist/line-awesome/css\",\n"
                + "      \"dist/line-awesome/fonts/**\": \"line-awesome/dist/line-awesome/fonts\"\n"
                + "    }\n" + "  }\n" + "}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Shouldn't re-bundle when the dev bundle already have all"
                        + " the entries defined in the project's theme.json",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_noProjectThemeHashInStats_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"lumoImports\": [\"typography\"]}",
                "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should trigger a bundle rebuild when project has theme"
                        + ".json but stats doesn't",
                needsBuild);
    }

    @Test
    public void parentThemeInFrontend_parentHasEntriesInJson_bundleMissesSomeEntries_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{ \"importCss\": [\"foo\"]}",
                "parent-theme");
        createProjectThemeJsonStub("{\"parent\": \"parent-theme\"}",
                "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        JsonObject stats = getBasicStats();
        stats.getObject(THEME_JSON_CONTENTS).put(bundleLocation, "{}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should rebuild when 'theme.json' from parent theme in "
                        + "frontend folder adds extra entries",
                needsBuild);
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

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("Adding 'index.ts' should require bundling",
                needsBuild);
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

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "'index.ts' equal content should not require bundling",
                needsBuild);

        FileUtils.write(indexTs, "window.alert('hello');",
                StandardCharsets.UTF_8);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner,
                finder, mode);
        Assert.assertTrue(
                "changed content for 'index.ts' should require bundling",
                needsBuild);
    }

    @Test
    public void indexTsDeleted_rebuildRequired() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();
        stats.getObject(FRONTEND_HASHES).put(FrontendUtils.INDEX_TS,
                "15931fa8c20e3c060c8ea491831e95cc8463962700a9bfb82c8e3844cf608f04");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue("'index.ts' delete should require re-bundling",
                needsBuild);
    }

    @Test
    public void standardVaadinComponent_notAddedToProjectAsJar_noRebuildRequired()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();
        JsonArray bundleImports = stats.getArray(BUNDLE_IMPORTS);
        bundleImports.set(bundleImports.length(),
                "Frontend/generated/jar-resources/vaadin-spreadsheet/vaadin-spreadsheet.js");
        stats.getObject(FRONTEND_HASHES).put(
                "vaadin-spreadsheet/vaadin-spreadsheet.js",
                "e545ad23a2d1d4b3a3370a0305dd71c15bbfc645216f50c6e327bd818b7484c4");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Should not require bundling if component JS is missing in jar-resources",
                needsBuild);
    }

    @Test
    public void cssImport_cssInMetaInfResources_notThrow_bundleRequired()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        CssData cssData = new CssData("./addons-styles/my-styles.css", null,
                null, null);

        Mockito.when(depScanner.getCss()).thenReturn(Collections.singletonMap(
                ChunkInfo.GLOBAL, Collections.singletonList(cssData)));

        JsonObject stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        // Should not throw an IllegalStateException:
        // "Failed to find the following css files in the...."
        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertTrue(
                "Should re-bundle if CSS is imported from META-INF/resources",
                needsBuild);
    }

    @Test
    public void flowFrontendPackageInPackageJson_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(
                "{\"dependencies\": {\"@vaadin/flow-frontend\": \"./target/flow-frontend\"}, \"vaadin\": { \"hash\": \"aHash\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        JsonObject stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse(
                "Shouldn't re-bundle when old @vaadin/flow-frontend package is in package.json",
                needsBuild);
    }

    @Test
    public void bundleMissesSomeEntries_devMode_skipBundleBuildSet_noBundleRebuild()
            throws IOException {
        Assume.assumeTrue(mode == Mode.DEVELOPMENT_BUNDLE);
        options.skipDevBundleBuild(true);

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.5\", \"@vaadin/text\":\"1.0.0\"}, "
                + "\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        JsonObject stats = getBasicStats();
        stats.getObject(PACKAGE_JSON_DEPENDENCIES).put("@vaadin/router",
                "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, finder, mode);
        Assert.assertFalse("Rebuild should be skipped", needsBuild);
    }

    @Test
    public void forceProductionBundle_bundleRequired() {
        Assume.assumeTrue(mode.isProduction());

        options.withForceProductionBuild(true);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), finder, mode);
        Assert.assertTrue(
                "Production bundle required due to force.production.bundle flag.",
                needsBuild);
    }

    private void createPackageJsonStub(String content) throws IOException {
        File packageJson = new File(temporaryFolder.getRoot(),
                Constants.PACKAGE_JSON);
        boolean created = packageJson.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(packageJson, content, StandardCharsets.UTF_8);
    }

    private void createProjectThemeJsonStub(String content, String theme)
            throws IOException {
        File themeJson = new File(temporaryFolder.getRoot(),
                "frontend/themes/" + theme + "/theme.json");
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

    private void setupFrontendUtilsMock(JsonObject stats) {
        if (mode.isProduction()) {
            bundleUtils
                    .when(() -> BundleValidationUtil.findProdBundleStatsJson(
                            Mockito.any(ClassFinder.class)))
                    .thenReturn(stats.toJson());
        } else {
            devBundleUtils.when(
                    () -> DevBundleUtils.getDevBundleFolder(Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            devBundleUtils
                    .when(() -> DevBundleUtils
                            .findBundleStatsJson(temporaryFolder.getRoot()))
                    .thenAnswer(q -> stats.toJson());
        }
        frontendUtils
                .when(() -> FrontendUtils.getJarResourceString(
                        Mockito.anyString(), Mockito.any(ClassFinder.class)))
                .thenAnswer(q -> jarResources.get(q.getArgument(0)));
    }

    @LoadDependenciesOnStartup
    static class AllEagerAppConf implements AppShellConfigurator {

    }
}
