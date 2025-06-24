package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.flow.theme.ThemeDefinition;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;
import static com.vaadin.flow.server.Constants.PROD_BUNDLE_JAR_PATH;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;

@RunWith(Parameterized.class)
public class BundleValidationTest {

    public static final String BLANK_PACKAGE_JSON_WITH_HASH = "{\n \"dependencies\": {}, \"vaadin\": { \"hash\": \"a5\"} \n}";

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

    private MockedStatic<ProdBundleUtils> prodBundleUtils;

    private MockedStatic<BundleValidationUtil> bundleUtils;

    private MockedStatic<IOUtils> ioUtils;

    private String bundleLocation;

    @Before
    public void init() {
        finder = Mockito.spy(new ClassFinder.DefaultClassFinder(
                this.getClass().getClassLoader()));
        options = new MockOptions(finder, temporaryFolder.getRoot())
                .withBuildDirectory("target");
        options.copyResources(Collections.emptySet());
        options.withProductionMode(mode.isProduction());
        bundleLocation = mode.isProduction() ? Constants.PROD_BUNDLE_NAME
                : Constants.DEV_BUNDLE_NAME;
        frontendUtils = Mockito.mockStatic(FrontendUtils.class,
                Mockito.CALLS_REAL_METHODS);
        devBundleUtils = Mockito.mockStatic(DevBundleUtils.class,
                Mockito.CALLS_REAL_METHODS);
        prodBundleUtils = Mockito.mockStatic(ProdBundleUtils.class,
                Mockito.CALLS_REAL_METHODS);
        bundleUtils = Mockito.mockStatic(BundleValidationUtil.class,
                Mockito.CALLS_REAL_METHODS);
        ioUtils = Mockito.mockStatic(IOUtils.class, Mockito.CALLS_REAL_METHODS);
    }

    @After
    public void teardown() {
        frontendUtils.close();
        devBundleUtils.close();
        prodBundleUtils.close();
        bundleUtils.close();
        ioUtils.close();
        File needsBuildFile = new File(options.getResourceOutputDirectory(),
                Constants.NEEDS_BUNDLE_BUILD_FILE);
        if (needsBuildFile.exists()) {
            needsBuildFile.delete();
        }
    }

    private ObjectNode getBasicStats() {
        ObjectNode stats = JacksonUtils.createObjectNode();

        ObjectNode packageJsonDependencies = JacksonUtils.createObjectNode();
        ObjectNode frontendHashes = JacksonUtils.createObjectNode();
        ObjectNode themeJsonContents = JacksonUtils.createObjectNode();

        ArrayNode entryScripts = JacksonUtils.createArrayNode();
        ArrayNode bundleImports = JacksonUtils.createArrayNode();

        stats.set(PACKAGE_JSON_DEPENDENCIES, packageJsonDependencies);
        stats.set(ENTRY_SCRIPTS, entryScripts);
        stats.set(BUNDLE_IMPORTS, bundleImports);
        stats.set(FRONTEND_HASHES, frontendHashes);
        stats.set(THEME_JSON_CONTENTS, themeJsonContents);
        stats.put(PACKAGE_JSON_HASH, "aHash");

        NodeUpdater nodeUpdater = new NodeUpdater(
                Mockito.mock(FrontendDependenciesScanner.class), options) {
            @Override
            public void execute() {
                // NO-OP
            }
        };

        // Add default packageJson dependencies
        for (Map.Entry<String, String> dependency : nodeUpdater
                .getDefaultDependencies().entrySet()) {
            packageJsonDependencies.put(dependency.getKey(),
                    dependency.getValue());
        }

        bundleImports.add("./generated/jar-resources/theme-util.js");
        frontendHashes.put("theme-util.js",
                BundleValidationUtil.calculateHash(THEME_UTIL_JS));
        jarResources.put("theme-util.js", THEME_UTIL_JS);
        return stats;
    }

    @Test
    public void noDevBundle_bundleCompilationRequires() {
        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), mode);
        Assert.assertTrue("Bundle should require creation if not available",
                needsBuild);
    }

    @Test
    public void devBundleStatsJsonMissing_bundleCompilationRequires() {
        devBundleUtils.when(() -> DevBundleUtils
                .getDevBundleFolder(Mockito.any(), Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        devBundleUtils.when(() -> DevBundleUtils
                .findBundleStatsJson(temporaryFolder.getRoot(), "target"))
                .thenReturn(null);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), mode);
        Assert.assertTrue("Missing stats.json should require bundling",
                needsBuild);
    }

    @Test
    public void hashesMatch_noNpmPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.5\"}, \"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Matching hashes should not require compilation",
                needsBuild);
    }

    @Test
    public void loadDependenciesOnStartup_annotatedClassInProject_compilationRequiredForProduction()
            throws IOException {
        Assume.assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.5\"}, \"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        Mockito.when(
                finder.getAnnotatedClasses(LoadDependenciesOnStartup.class))
                .thenReturn(Collections.singleton(AllEagerAppConf.class));

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "'LoadDependenciesOnStartup' annotation requires build",
                needsBuild);
    }

    @Test
    public void hashesMatch_statsMissingNpmPackages_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.5\"}, \"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.7.5");
        packages.put("@vaadin/text", "1.0.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("Missing npmPackage should require bundling",
                needsBuild);
    }

    @Test
    public void hashesMatch_statsMissingPackageJsonPackage_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "1.7.5",
                    "@vaadin/text":"1.0.0"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("Bundle missing module dependency should rebuild",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonMissingNpmPackages_statsHasJsonPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"dependencies\": {"
                + "\"@vaadin/router\": \"1.7.5\"}, \"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("@vaadin/text",
                "1.0.0");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void packageJsonContainsOldVersion_versionsJsonUpdates_noCompilation()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        File versions = new File(temporaryFolder.getRoot(),
                Constants.VAADIN_CORE_VERSIONS_JSON);
        versions.createNewFile();
        FileUtils.write(versions, """
                {
                  "core": {
                    "vaadin-router": {
                      "jsVersion": "2.0.3",
                      "npmName": "@vaadin/router",
                      "releasenotes": true
                    }
                  },
                  "platform": "123-SNAPSHOT"
                }
                """, StandardCharsets.UTF_8);

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "2.0.3");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "vaadin-core-versions.json should have updated version to expected.",
                needsBuild);
    }

    @Test
    public void packageJsonContainsOldVersionsAfterVersionUpdate_updatedStatsMatches_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "1.7.5",
                    "@vaadin/text": "1.0.0"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("@vaadin/router", "1.9.2");
        packages.put("@vaadin/text", "2.1.0");
        Mockito.when(depScanner.getPackages()).thenReturn(packages);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.9.2");
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("@vaadin/text",
                "2.1.0");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void noPackageJsonHashAfterCleanFrontend_statsHasDefaultJsonPackages_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "name": "no-name",
                  "license": "UNLICENSED",
                  "dependencies": {
                    "@vaadin/router": "1.7.5"
                  },
                  "devDependencies": {}
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("@vaadin/text",
                "1.0.0");
        stats.put(PACKAGE_JSON_HASH,
                "af45419b27dcb44b875197df4347b97316cc8fa6055458223a73aedddcfe7cc6");
        ((ArrayNode) stats.get(ENTRY_SCRIPTS))
                .add("VAADIN/build/indexhtml-aa31f040.js");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void noPackageJsonHashAfterCleanFrontend_statsMissingDefaultJsonPackages_compilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "name": "no-name",
                  "license": "UNLICENSED",
                  "dependencies": {
                    "@vaadin/router": "1.7.5"
                  },
                  "devDependencies": {}
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.singletonMap("@vaadin/text", "1.0.0"));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Missing npmPackage in stats.json should require compilation",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonHasRange_statsHasFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Not missing npmPackage in stats.json should not require compilation",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonHasTildeRange_statsHasNewerFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "~1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.6");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("No compilation if tilde range only patch update",
                needsBuild);

        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.1");
        setupFrontendUtilsMock(stats);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner, mode);
        Assert.assertTrue(
                "Compilation required if minor version change for tilde range",
                needsBuild);
    }

    @Test
    public void hashesMatch_packageJsonHasCaretRange_statsHasNewerFixed_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "No compilation if caret range only minor version update",
                needsBuild);

        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "2.0.0");
        setupFrontendUtilsMock(stats);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner, mode);
        Assert.assertTrue(
                "Compilation required if major version change for caret range",
                needsBuild);
    }

    @Test
    public void packageJsonHasOldPlatformDependencies_statsDoesNotHaveThem_noCompilationRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@polymer/iron-list": "3.1.0",
                    "@vaadin/vaadin-accordion": "23.3.7"
                  },
                  "vaadin": {
                    "dependencies": {
                      "@polymer/iron-list": "3.1.0",
                      "@vaadin/vaadin-accordion": "23.3.7"
                    },
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/accordion", "24.0.0.beta2");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
                .getDefaultPackageJson(options, depScanner, null)
                .get(NodeUpdater.VAADIN_DEP_KEY).get(NodeUpdater.HASH_KEY)
                .textValue();

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("@vaadin/text",
                "1.0.0");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
                .getDefaultPackageJson(options, depScanner, null)
                .get(NodeUpdater.VAADIN_DEP_KEY).get(NodeUpdater.HASH_KEY)
                .textValue();

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
                .getDefaultPackageJson(options, depScanner, null)
                .get(NodeUpdater.VAADIN_DEP_KEY).get(NodeUpdater.HASH_KEY)
                .textValue();

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");
        stats.put(PACKAGE_JSON_HASH, defaultHash);

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Default package.json should be built and validated",
                needsBuild);
    }

    @Test
    public void generatedFlowImports_bundleMissingImports_buildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports
                .add("@Frontend/generated/jar-resources/dndConnector-es6.js");
        bundleImports.add("@polymer/paper-input/paper-input.js");
        bundleImports.add("@vaadin/common-frontend/ConnectionIndicator.js");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("Compilation required as stats.json missing import",
                needsBuild);
    }

    @Test
    public void generatedFlowImports_bundleHasAllImports_noBuildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.add("@polymer/paper-input/paper-input.js");
        bundleImports.add("@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports
                .add("Frontend/generated/jar-resources/dndConnector-es6.js");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("All imports in stats, no compilation required",
                needsBuild);
    }

    @Test
    public void themedGeneratedFlowImports_bundleUsesTheme_noBuildRequired()
            throws IOException {

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules()).thenReturn(
                Collections.singletonMap(ChunkInfo.GLOBAL, Collections
                        .singletonList("@vaadin/grid/src/vaadin-grid.js")));
        Mockito.when(depScanner.getTheme())
                .thenReturn(new NodeTestComponents.LumoTest());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.add("@polymer/paper-input/paper-input.js");
        bundleImports.add("@vaadin/grid/src/vaadin-grid.js");
        bundleImports
                .add("Frontend/generated/jar-resources/dndConnector-es6.js");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "All themed imports in stats, no compilation required",
                needsBuild);
    }

    @Test
    public void frontendFileHashMatches_noBundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonMap(ChunkInfo.GLOBAL, Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js")));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("./generated/jar-resources/TodoTemplate.js");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put("TodoTemplate.js",
                BundleValidationUtil.calculateHash(fileContent));
        jarResources.put("TodoTemplate.js", fileContent);

        setupFrontendUtilsMock(stats);
        devBundleUtils.when(() -> DevBundleUtils
                .getDevBundleFolder(Mockito.any(), Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Jar fronted file content hash should match.",
                needsBuild);
    }

    @Test
    public void noFrontendFileHash_bundleRebuild() throws IOException {
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonMap(ChunkInfo.GLOBAL, Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js")));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ((ArrayNode) stats.get(BUNDLE_IMPORTS))
                .add("Frontend/generated/jar-resources/TodoTemplate.js");

        devBundleUtils.when(() -> DevBundleUtils
                .getDevBundleFolder(Mockito.any(), Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        frontendUtils
                .when(() -> FrontendUtils.getJarResourceString(
                        Mockito.eq("TodoTemplate.js"),
                        Mockito.any(ClassFinder.class)))
                .thenReturn(fileContent);
        devBundleUtils
                .when(() -> DevBundleUtils.findBundleStatsJson(
                        temporaryFolder.getRoot(), "target"))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("Content should not have been validated.",
                needsBuild);
    }

    @Test
    public void frontendFileHashMissmatch_bundleRebuild() throws IOException {
        String fileContent = "TodoContent2";

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getModules()).thenReturn(Collections
                .singletonMap(ChunkInfo.GLOBAL, Collections.singletonList(
                        "Frontend/generated/jar-resources/TodoTemplate.js")));

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ((ArrayNode) (ArrayNode) stats.get(BUNDLE_IMPORTS))
                .add("Frontend/generated/jar-resources/TodoTemplate.js");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put("TodoTemplate.js",
                "dea5180dd21d2f18d1472074cd5305f60b824e557dae480fb66cdf3ea73edc65");

        devBundleUtils.when(() -> DevBundleUtils
                .getDevBundleFolder(Mockito.any(), Mockito.any()))
                .thenReturn(temporaryFolder.getRoot());
        frontendUtils
                .when(() -> FrontendUtils.getJarResourceString(
                        Mockito.eq("TodoTemplate.js"),
                        Mockito.any(ClassFinder.class)))
                .thenReturn(fileContent);
        devBundleUtils
                .when(() -> DevBundleUtils.findBundleStatsJson(
                        temporaryFolder.getRoot(), "target"))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Jar fronted file content hash should not be a match.",
                needsBuild);
    }

    @Test
    public void cssImportWithInline_statsAndImportsMatchAndNoBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File stylesheetFile = new File(temporaryFolder.getRoot(),
                DEFAULT_FRONTEND_DIR + "my-styles.css");
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

        ObjectNode stats = getBasicStats();
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("Frontend/my-styles.css");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put("my-styles.css",
                "0d94fe659d24e1e56872b47fc98d9f09227e19816c62a3db709bad347fbd0cdd");

        setupFrontendUtilsMock(stats);
        jarResources.put("my-styles.css", "body{color:yellow}");

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        ((ArrayNode) stats.get(BUNDLE_IMPORTS))
                .add("Frontend/views/lit-view.ts");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put("views/lit-view.ts",
                "old_hash");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("Frontend/views/lit-view.ts");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put("views/lit-view.ts",
                "eaf04adbc43cb363f6b58c45c6e0e8151084941247abac9493beed8d29f08add");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        ((ArrayNode) stats.get(BUNDLE_IMPORTS))
                .add("Frontend/views/lit-view.ts");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put("views/lit-view.ts",
                "eaf04adbc43cb363f6b58c45c6e0e8151084941247abac9493beed8d29f08add");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("Project frontend file delete should trigger rebuild",
                needsBuild);
    }

    @Test
    public void reusedTheme_noReusedThemes_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        stats.remove(THEME_JSON_CONTENTS);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
                DEFAULT_FRONTEND_DIR
                        + "generated/jar-resources/themes/custom-theme");
        boolean created = jarResourcesFolder.mkdirs();
        Assert.assertTrue(created);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        setupFrontendUtilsMock(getBasicStats());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
                depScanner, mode);
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
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("other-theme",
                "other-theme-hash");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("reusable-theme",
                """
                        {
                          "importCss": ["@fortawesome/fontawesome-free/css/all.min.css"],
                          "assets": {
                            "@fortawesome/fontawesome-free": {
                              "svgs/brands/**": "fontawesome/svgs/brands",
                              "webfonts/**": "webfonts"
                            }
                          }
                        }
                        """);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("reusable-theme",
                """
                        {
                          "importCss": ["@fortawesome/fontawesome-free/css/all.min.css"],
                          "assets": {
                            "@fortawesome/fontawesome-free": {
                              "svgs/brands/**": "fontawesome/svgs/brands",
                              "webfonts/**": "webfonts"
                            },
                            "line-awesome": {
                              "dist/line-awesome/css/**": "line-awesome/dist/line-awesome/css",
                              "dist/line-awesome/fonts/**": "line-awesome/dist/line-awesome/fonts"
                            }
                          }
                        }
                        """);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        stats.remove(THEME_JSON_CONTENTS);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
        new File(temporaryFolder.getRoot(),
                DEFAULT_FRONTEND_DIR + "themes/my-parent-theme").mkdirs();

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                "{\"lumoImports\": [\"typography\"]}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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
        new File(temporaryFolder.getRoot(),
                DEFAULT_FRONTEND_DIR + "themes/my-theme").mkdirs();

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                "{\"lumoImports\": [\"typography\"]}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when project has no theme.json",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_statsAndProjectThemeJsonEquals_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("""
                {
                  "boolean-property": true,
                  "numeric-property": 42.42,
                  "string-property": "foo",
                  "array-property": ["one", "two"],
                  "object-property": {
                    "foo": "bar"
                  }
                }
                """, "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("my-theme", """
                {





                  "boolean-property": true,
                  "numeric-property": 42.42,
                  "string-property": "foo",
                  "array-property": ["one", "two"],
                  "object-property": {
                    "foo": "bar"
                  }
                }
                """);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Should not trigger a bundle rebuild when project theme.json has the same content as in the bundle",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_bundleMissesSomeEntries_bundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub(
                """
                        {
                          "importCss": ["@fortawesome/fontawesome-free/css/all.css"],
                          "assets": {
                            "line-awesome": {
                              "dist/line-awesome/css/**": "line-awesome/dist/line-awesome/css",
                            }
                          }
                        }
                        """,
                "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                """
                        {
                          "lumoImports": ["typography", "color", "spacing", "badge", "utility"],
                          "assets": {
                            "line-awesome": {
                              "dist/line-awesome/css/**": "line-awesome/dist/line-awesome/css",
                              "dist/line-awesome/fonts/**": "line-awesome/dist/line-awesome/fonts"
                            }
                          }
                        }
                        """);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Should rebuild when project theme.json adds extra entries",
                needsBuild);
    }

    @Test
    public void themeJsonUpdates_bundleHaveAllEntriesAndMore_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub(
                """
                        {
                          "lumoImports": ["typography", "color", "spacing", "badge", "utility"]
                        }
                        """,
                "my-theme");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                """
                        {
                          "lumoImports": ["typography", "color", "spacing", "badge", "utility"],
                          "assets": {
                            "line-awesome": {
                              "dist/line-awesome/css/**": "line-awesome/dist/line-awesome/css",
                              "dist/line-awesome/fonts/**": "line-awesome/dist/line-awesome/fonts"
                            }
                          }
                        }
                        """);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation, "{}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Should rebuild when 'theme.json' from parent theme in "
                        + "frontend folder adds extra entries",
                needsBuild);
    }

    @Test
    public void projectThemeComponentsCSS_contentsAdded_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                false, false);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in '" + DEFAULT_FRONTEND_DIR
                        + "<theme>/components' folder",
                needsBuild);
    }

    @Test
    public void projectThemeComponentsCSS_contentsChanged_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                true, true);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents have changed",
                needsBuild);
    }

    @Test
    public void projectThemeComponentsCSS_contentsNotChanged_noBundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                false, true);
        Assert.assertFalse(
                "Should not rebuild when Shadow DOM Stylesheets contents have not changed",
                needsBuild);
    }

    @Test
    public void projectThemeComponentsCSS_removedFromProject_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents have been removed",
                needsBuild);
    }

    @Test
    public void jarResourceThemeComponentsCSS_contentsAdded_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                false, false);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in 'frontend/generated/jar-resources/<theme>/components' folder",
                needsBuild);
    }

    @Test
    public void jarResourceThemeComponentsCSS_contentsChanged_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                true, true);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents have changed",
                needsBuild);
    }

    @Test
    public void jarResourceThemeComponentsCSS_contentsNotChanged_noBundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                false, true);
        Assert.assertFalse(
                "Should not rebuild when Shadow DOM Stylesheets contents have not changed",
                needsBuild);
    }

    @Test
    public void jarResourceThemeComponentsCSS_removedFromProject_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents have been removed",
                needsBuild);
    }

    @Test
    public void projectParentThemeComponentsCSS_contentsAdded_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                false, false);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in parent theme folder",
                needsBuild);
    }

    @Test
    public void projectParentThemeComponentsCSS_contentsChanged_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                true, true);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have changed",
                needsBuild);
    }

    @Test
    public void projectParentThemeComponentsCSS_contentsNotChanged_noBundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                false, true);
        Assert.assertFalse(
                "Should not rebuild when Shadow DOM Stylesheets contents in parent theme have not changed",
                needsBuild);
    }

    @Test
    public void projectParentThemeComponentsCSS_removedFromProject_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have been removed",
                needsBuild);
    }

    @Test
    public void projectParentInJarThemeComponentsCSS_contentsAdded_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                false, false);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in parent theme folder",
                needsBuild);
    }

    @Test
    public void projectParentInJarThemeComponentsCSS_contentsChanged_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                true, true);
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have changed",
                needsBuild);
    }

    @Test
    public void projectParentInJarThemeComponentsCSS_contentsNotChanged_noBundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                false, true);
        Assert.assertFalse(
                "Should not rebuild when Shadow DOM Stylesheets contents in parent theme have not changed",
                needsBuild);
    }

    @Test
    public void projectParentInJarThemeComponentsCSS_removedFromProject_bundleRebuild()
            throws IOException {
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        Assert.assertTrue(
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have been removed",
                needsBuild);
    }

    @Test
    public void indexTsAdded_rebuildRequired() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = temporaryFolder
                .newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);

        File indexTs = new File(frontendFolder, FrontendUtils.INDEX_TS);
        indexTs.createNewFile();

        FileUtils.write(indexTs, "window.alert('');", StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("Adding 'index.ts' should require bundling",
                needsBuild);
    }

    @Test
    public void changeInIndexTs_rebuildRequired() throws IOException {
        createPackageJsonStub(
                "{\"dependencies\": {}, \"vaadin\": { \"hash\": \"aHash\"} }");
        File frontendFolder = temporaryFolder
                .newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);

        File indexTs = new File(frontendFolder, FrontendUtils.INDEX_TS);
        indexTs.createNewFile();

        FileUtils.write(indexTs, "window.alert('');", StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(FrontendUtils.INDEX_TS,
                "15931fa8c20e3c060c8ea491831e95cc8463962700a9bfb82c8e3844cf608f04");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "'index.ts' equal content should not require bundling",
                needsBuild);

        FileUtils.write(indexTs, "window.alert('hello');",
                StandardCharsets.UTF_8);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner, mode);
        Assert.assertTrue(
                "changed content for 'index.ts' should require bundling",
                needsBuild);
    }

    @Test
    public void indexTsDeleted_rebuildRequired() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(FrontendUtils.INDEX_TS,
                "15931fa8c20e3c060c8ea491831e95cc8463962700a9bfb82c8e3844cf608f04");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue("'index.ts' delete should require re-bundling",
                needsBuild);
    }

    @Test
    public void indexHtmlNotChanged_rebuildNotRequired() throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = temporaryFolder
                .newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);

        File indexHtml = new File(frontendFolder, FrontendUtils.INDEX_HTML);
        indexHtml.createNewFile();
        String defaultIndexHtml = new String(TaskGenerateIndexHtml.class
                .getResourceAsStream(INDEX_HTML).readAllBytes(),
                StandardCharsets.UTF_8);
        FileUtils.write(indexHtml, defaultIndexHtml, StandardCharsets.UTF_8);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(INDEX_HTML,
                BundleValidationUtil.calculateHash(defaultIndexHtml));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Default 'index.html' should not require bundling",
                needsBuild);
    }

    @Test
    public void indexHtmlChanged_productionMode_rebuildRequired()
            throws IOException {
        Assume.assumeTrue(mode.isProduction());
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = temporaryFolder
                .newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);

        File indexHtml = new File(frontendFolder, FrontendUtils.INDEX_HTML);
        indexHtml.createNewFile();
        String defaultIndexHtml = new String(
                getClass().getResourceAsStream(INDEX_HTML).readAllBytes(),
                StandardCharsets.UTF_8);
        String customIndexHtml = defaultIndexHtml.replace("<body>",
                "<body><div>custom content</div>");
        FileUtils.write(indexHtml, customIndexHtml, StandardCharsets.UTF_8);
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(INDEX_HTML,
                BundleValidationUtil.calculateHash(defaultIndexHtml));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "In production mode, custom 'index.html' should require bundling",
                needsBuild);
    }

    @Test
    public void indexHtmlChanged_developmentMode_rebuildNotRequired()
            throws IOException {
        Assume.assumeFalse(mode.isProduction());
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = temporaryFolder
                .newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);

        File indexHtml = new File(frontendFolder, FrontendUtils.INDEX_HTML);
        indexHtml.createNewFile();
        String defaultIndexHtml = new String(
                getClass().getResourceAsStream(INDEX_HTML).readAllBytes(),
                StandardCharsets.UTF_8);
        String customIndexHtml = defaultIndexHtml.replace("<body>",
                "<body><div>custom content</div>");
        FileUtils.write(indexHtml, customIndexHtml, StandardCharsets.UTF_8);
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(INDEX_HTML,
                BundleValidationUtil.calculateHash(defaultIndexHtml));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "In dev mode, custom 'index.html' should not require bundling",
                needsBuild);
    }

    @Test
    public void standardVaadinComponent_notAddedToProjectAsJar_noRebuildRequired()
            throws IOException {
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add(
                "Frontend/generated/jar-resources/vaadin-spreadsheet/vaadin-spreadsheet.js");
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(
                "vaadin-spreadsheet/vaadin-spreadsheet.js",
                "e545ad23a2d1d4b3a3370a0305dd71c15bbfc645216f50c6e327bd818b7484c4");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        // Should not throw an IllegalStateException:
        // "Failed to find the following css files in the...."
        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
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

        ObjectNode stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Shouldn't re-bundle when old @vaadin/flow-frontend package is in package.json",
                needsBuild);
    }

    @Test
    public void localPackageInPackageJson_notChanged_noBundleRebuild()
            throws IOException {
        createPackageJsonStub(
                "{\"dependencies\": {\"my-pkg\": \"file:my-pkg\"}, \"vaadin\": { \"hash\": \"aHash\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("my-pkg",
                "file:my-pkg");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse(
                "Shouldn't re-bundle when referencing local packages in package.json",
                needsBuild);
    }

    @Test
    public void localPackageInPackageJson_differentReference_bundleRebuild()
            throws IOException {
        createPackageJsonStub(
                "{\"dependencies\": {\"my-pkg\": \"file:my-pkg\"}, \"vaadin\": { \"hash\": \"aHash\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("my-pkg",
                "./another-folder");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Should re-bundle when local packages have different values",
                needsBuild);
    }

    @Test
    public void localPackageInPackageJson_parsableVersionInStats_bundleRebuild()
            throws IOException {
        createPackageJsonStub(
                "{\"dependencies\": {\"my-pkg\": \"file:my-pkg\"}, \"vaadin\": { \"hash\": \"aHash\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("my-pkg",
                "1.0.0");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Should re-bundle when local package in package.json but parsable version in stats",
                needsBuild);
    }

    @Test
    public void localPackageInStats_parsableVersionInPackageJson_bundleRebuild()
            throws IOException {
        createPackageJsonStub(
                "{\"dependencies\": {\"my-pkg\": \"1.0.0\"}, \"vaadin\": { \"hash\": \"aHash\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES)).put("my-pkg",
                "file:my-pkg");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Should re-bundle when local package in stats but parsable version in package.json",
                needsBuild);
    }

    @Test
    public void bundleMissesSomeEntries_devMode_skipBundleBuildSet_noBundleRebuild()
            throws IOException {
        Assume.assumeTrue(mode == Mode.DEVELOPMENT_BUNDLE);
        options.skipDevBundleBuild(true);

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "1.7.5", "@vaadin/text":"1.0.0"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.7.5");

        setupFrontendUtilsMock(stats);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Rebuild should be skipped", needsBuild);
    }

    @Test
    public void forceProductionBundle_bundleRequired() {
        Assume.assumeTrue(mode.isProduction());

        options.withForceProductionBuild(true);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), mode);
        Assert.assertTrue(
                "Production bundle required due to force.production.bundle flag.",
                needsBuild);
    }

    @Test
    public void noDevFolder_compressedDevBundleExists_noBuildRequired()
            throws IOException {
        Assume.assumeTrue(!mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        File bundleSourceFolder = temporaryFolder.newFolder("compiled");

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.add("@polymer/paper-input/paper-input.js");
        bundleImports.add("@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports
                .add("Frontend/generated/jar-resources/dndConnector-es6.js");

        File configFolder = new File(bundleSourceFolder, "config/");
        configFolder.mkdir();

        File statsFile = new File(configFolder, "stats.json");
        FileUtils.write(statsFile, stats.toString(), StandardCharsets.UTF_8);

        DevBundleUtils.compressBundle(temporaryFolder.getRoot(),
                bundleSourceFolder);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Jar fronted file content hash should match.",
                needsBuild);
    }

    @Test
    public void compressedProdBundleExists_noBuildRequired()
            throws IOException {
        Assume.assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        File bundleSourceFolder = temporaryFolder.newFolder("compiled");

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.add("@polymer/paper-input/paper-input.js");
        bundleImports.add("@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports
                .add("Frontend/generated/jar-resources/dndConnector-es6.js");

        File configFolder = new File(bundleSourceFolder, "config/");
        configFolder.mkdir();

        File statsFile = new File(configFolder, "stats.json");
        FileUtils.write(statsFile, stats.toString(), StandardCharsets.UTF_8);

        ProdBundleUtils.compressBundle(temporaryFolder.getRoot(),
                bundleSourceFolder);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Jar fronted file content hash should match.",
                needsBuild);
    }

    @Test
    public void noFileBundleOrJar_compressedBundleExists_noBuildRequired()
            throws IOException {
        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "@vaadin/router": "^1.7.5"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages())
                .thenReturn(Collections.emptyMap());
        Mockito.when(depScanner.getModules())
                .thenReturn(Collections.singletonMap(ChunkInfo.GLOBAL,
                        Collections.singletonList(
                                "@polymer/paper-checkbox/paper-checkbox.js")));

        File bundleSourceFolder = temporaryFolder.newFolder("compiled");

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.6");
        ArrayNode bundleImports = (ArrayNode) stats.get(BUNDLE_IMPORTS);
        bundleImports.add("@polymer/paper-checkbox/paper-checkbox.js");
        bundleImports.add("@polymer/paper-input/paper-input.js");
        bundleImports.add("@vaadin/grid/theme/lumo/vaadin-grid.js");
        bundleImports
                .add("Frontend/generated/jar-resources/dndConnector-es6.js");

        File configFolder = new File(bundleSourceFolder, "config/");
        configFolder.mkdir();

        File statsFile = new File(configFolder, "stats.json");
        FileUtils.write(statsFile, stats.toString(), StandardCharsets.UTF_8);

        if (mode.isProduction()) {
            ProdBundleUtils.compressBundle(temporaryFolder.getRoot(),
                    bundleSourceFolder);
            Mockito.when(finder
                    .getResource(PROD_BUNDLE_JAR_PATH + "config/stats.json"))
                    .thenReturn(null);
        } else {
            DevBundleUtils.compressBundle(temporaryFolder.getRoot(),
                    bundleSourceFolder);
            Mockito.when(finder
                    .getResource(DEV_BUNDLE_JAR_PATH + "config/stats.json"))
                    .thenReturn(null);
        }

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Jar frontend file content hash should match.",
                needsBuild);
    }

    @Test
    public void defaultDevBundleExists_noCompressedDevBundleFile_reactDisabled_buildRequired()
            throws IOException {
        options.withReact(false);
        Assume.assumeTrue(!mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();

        URL url = Mockito.mock(URL.class);
        Mockito.when(
                finder.getResource(DEV_BUNDLE_JAR_PATH + "config/stats.json"))
                .thenReturn(url);
        ioUtils.when(() -> IOUtils.toString(url, StandardCharsets.UTF_8))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Dev bundle build is expected when react is disabled and using otherwise default dev bundle.",
                needsBuild);
    }

    @Test
    public void defaultProdBundleExists_noCompressedProdBundleFile_noBuildRequired()
            throws IOException {
        Assume.assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "react": "18.2.0",
                    "react-dom": "18.2.0",
                    "react-router": "7.0.0"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();

        URL url = Mockito.mock(URL.class);
        Mockito.when(
                finder.getResource(PROD_BUNDLE_JAR_PATH + "config/stats.json"))
                .thenReturn(url);
        ioUtils.when(() -> IOUtils.toString(url, StandardCharsets.UTF_8))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Jar frontend file content hash should match.",
                needsBuild);
    }

    @Test
    public void defaultProdBundleExists_noCompressedProdBundleFileAndWithVersionsJsonExclusions_noBuildRequired()
            throws IOException {
        Assume.assumeTrue(mode.isProduction());
        frontendUtils
                .when(() -> FrontendUtils.isReactModuleAvailable(Mockito.any()))
                .thenAnswer(q -> true);

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, """
                {
                  "dependencies": {
                    "react": "18.2.0",
                    "react-dom": "18.2.0",
                    "react-router": "7.0.0"
                  },
                  "vaadin": {
                    "hash": "aHash"
                  }
                }
                """, StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(depScanner.getPackages()).thenReturn(
                Collections.singletonMap("@vaadin/button", "2.0.0"));

        File versions = new File(temporaryFolder.getRoot(),
                Constants.VAADIN_CORE_VERSIONS_JSON);
        versions.createNewFile();
        FileUtils.write(versions, """
                {
                  "core": {
                    "vaadin-button": {
                      "jsVersion": "2.0.0",
                      "npmName": "@vaadin/button"
                    }
                  },
                  "react": {
                    "react-components": {
                      "exclusions": ["@vaadin/button"],
                      "jsVersion": "24.4.0",
                      "mode": "react",
                      "npmName": "@vaadin/react-components"
                    }
                  },
                  "platform": "123-SNAPSHOT"
                }
                """, StandardCharsets.UTF_8);

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/react-components", "24.4.0");

        URL url = Mockito.mock(URL.class);
        Mockito.when(
                finder.getResource(PROD_BUNDLE_JAR_PATH + "config/stats.json"))
                .thenReturn(url);
        ioUtils.when(() -> IOUtils.toString(url, StandardCharsets.UTF_8))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertFalse("Jar frontend file content hash should match.",
                needsBuild);
    }

    @Test
    public void defaultProdBundleExists_noCompressedProdBundleFile_reactDisabled_buildRequired()
            throws IOException {
        options.withReact(false);
        Assume.assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder.getRoot(), "package.json");
        packageJson.createNewFile();

        FileUtils.write(packageJson, "{\"vaadin\": { \"hash\": \"aHash\"} }",
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();

        URL url = Mockito.mock(URL.class);
        Mockito.when(
                finder.getResource(PROD_BUNDLE_JAR_PATH + "config/stats.json"))
                .thenReturn(url);
        ioUtils.when(() -> IOUtils.toString(url, StandardCharsets.UTF_8))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        Assert.assertTrue(
                "Prod bundle build is expected when react is disabled and using otherwise default prod bundle.",
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
        createThemeJsonStub(content, theme, true);
    }

    private void createThemeJsonStub(String content, String theme,
            boolean projectTheme) throws IOException {
        String themeLocation = projectTheme ? "" : "generated/jar-resources/";
        File themeJson = new File(temporaryFolder.getRoot(),
                DEFAULT_FRONTEND_DIR + themeLocation + "themes/" + theme
                        + "/theme.json");
        FileUtils.forceMkdir(themeJson.getParentFile());
        boolean created = themeJson.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(themeJson, content, StandardCharsets.UTF_8);
    }

    private void createProjectFrontendFileStub() throws IOException {
        File frontendFile = new File(temporaryFolder.getRoot(),
                DEFAULT_FRONTEND_DIR + "views/lit-view.ts");
        FileUtils.forceMkdir(frontendFile.getParentFile());
        boolean created = frontendFile.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(frontendFile, "Some codes", StandardCharsets.UTF_8);
    }

    private void setupFrontendUtilsMock(ObjectNode stats) {
        if (mode.isProduction()) {
            prodBundleUtils
                    .when(() -> ProdBundleUtils.findBundleStatsJson(
                            Mockito.any(File.class),
                            Mockito.any(ClassFinder.class)))
                    .thenReturn(stats.toString());
        } else {
            devBundleUtils
                    .when(() -> DevBundleUtils.getDevBundleFolder(Mockito.any(),
                            Mockito.any()))
                    .thenReturn(temporaryFolder.getRoot());
            devBundleUtils
                    .when(() -> DevBundleUtils.findBundleStatsJson(
                            temporaryFolder.getRoot(), "target"))
                    .thenAnswer(q -> stats.toString());
        }
        frontendUtils
                .when(() -> FrontendUtils.getJarResourceString(
                        Mockito.anyString(), Mockito.any(ClassFinder.class)))
                .thenAnswer(q -> jarResources.get(q.getArgument(0)));
    }

    @LoadDependenciesOnStartup
    static class AllEagerAppConf implements AppShellConfigurator {

    }

    private boolean checkBundleRebuildForProjectThemeComponentsCSS(
            boolean contentChanged, boolean bundled,
            String... otherBundledComponentCss) throws IOException {
        return checkBundleRebuildForThemeComponentsCSS(true, false,
                contentChanged, bundled, otherBundledComponentCss);
    }

    private boolean checkBundleRebuildForJarPackagedThemeComponentsCSS(
            boolean contentChanged, boolean bundled,
            String... otherBundledComponentCss) throws IOException {
        return checkBundleRebuildForThemeComponentsCSS(false, false,
                contentChanged, bundled, otherBundledComponentCss);
    }

    private boolean checkBundleRebuildForParentProjectThemeComponentsCSS(
            boolean contentChanged, boolean bundled,
            String... otherBundledComponentCss) throws IOException {
        return checkBundleRebuildForThemeComponentsCSS(true, true,
                contentChanged, bundled, otherBundledComponentCss);
    }

    private boolean checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
            boolean contentChanged, boolean bundled,
            String... otherBundledComponentCss) throws IOException {
        return checkBundleRebuildForThemeComponentsCSS(false, true,
                contentChanged, bundled, otherBundledComponentCss);
    }

    private boolean checkBundleRebuildForThemeComponentsCSS(
            boolean projectTheme, boolean useParentTheme,
            boolean contentChanged, boolean bundled,
            String... otherBundledComponentCss) throws IOException {
        String cssTemplate = "[part=\"input-field\"]{background: %s; }";
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        String themeContents = "{ \"importCss\": [\"foo\"] }";
        String themeWithParentContents = "{\"parent\": \"parent-theme\"}";
        if (useParentTheme) {
            createThemeJsonStub(themeContents, "parent-theme", projectTheme);
            createThemeJsonStub(themeWithParentContents, "my-theme",
                    projectTheme);
        } else {
            createThemeJsonStub(themeContents, "my-theme", projectTheme);
        }

        String themeLocation = (projectTheme ? "" : "generated/jar-resources/")
                + "themes/" + ((useParentTheme) ? "parent-theme" : "my-theme")
                + "/components/";
        File stylesheetFile = new File(temporaryFolder.getRoot(),
                DEFAULT_FRONTEND_DIR + themeLocation + "vaadin-text-field.css");
        FileUtils.forceMkdir(stylesheetFile.getParentFile());
        boolean created = stylesheetFile.createNewFile();
        Assert.assertTrue(created);
        FileUtils.write(stylesheetFile, String.format(cssTemplate, "blue"),
                StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);

        ObjectNode stats = getBasicStats();
        if (useParentTheme) {
            ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("parent-theme",
                    themeContents);
            ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("my-theme",
                    themeWithParentContents);
        } else {
            ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put("my-theme",
                    themeContents);
        }
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                themeContents);
        if (bundled) {
            ((ObjectNode) stats.get(FRONTEND_HASHES))
                    .put(themeLocation + "vaadin-text-field.css",
                            BundleValidationUtil.calculateHash(String.format(
                                    cssTemplate,
                                    (contentChanged) ? "red" : "blue")));
        }
        for (String path : otherBundledComponentCss) {
            ((ObjectNode) stats.get(FRONTEND_HASHES)).put(themeLocation + path,
                    BundleValidationUtil.calculateHash(
                            "[part=\"input-field\"]{background: green; }"));
        }

        setupFrontendUtilsMock(stats);

        return BundleValidationUtil.needsBuild(options, depScanner, mode);
    }

}
