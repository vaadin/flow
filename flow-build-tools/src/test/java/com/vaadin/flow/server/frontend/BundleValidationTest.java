/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.DevBundleUtils;
import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.FrontendUtils;
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

import static com.vaadin.flow.internal.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.internal.FrontendUtils.INDEX_HTML;
import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;
import static com.vaadin.flow.server.Constants.PROD_BUNDLE_JAR_PATH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BundleValidationTest {

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

    static Stream<Mode> modes() {
        return Stream.of(Mode.PRODUCTION_PRECOMPILED_BUNDLE,
                Mode.DEVELOPMENT_BUNDLE);
    }

    private Mode mode;

    @TempDir
    File temporaryFolder;

    private Options options;

    ClassFinder finder;

    private Map<String, String> jarResources = new HashMap<>();

    private MockedStatic<FrontendBuildUtils> frontendBuildUtils;

    private MockedStatic<DevBundleUtils> devBundleUtils;

    private MockedStatic<ProdBundleUtils> prodBundleUtils;

    private MockedStatic<BundleValidationUtil> bundleUtils;

    private MockedStatic<FileIOUtils> ioUtils;

    private String bundleLocation;

    @BeforeEach
    void init() {
        finder = Mockito.spy(new ClassFinder.DefaultClassFinder(
                this.getClass().getClassLoader()));
        options = new MockOptions(finder, temporaryFolder)
                .withBuildDirectory("target");
        options.copyResources(Collections.emptySet());
        frontendBuildUtils = Mockito.mockStatic(FrontendBuildUtils.class,
                Mockito.CALLS_REAL_METHODS);
        devBundleUtils = Mockito.mockStatic(DevBundleUtils.class,
                Mockito.CALLS_REAL_METHODS);
        prodBundleUtils = Mockito.mockStatic(ProdBundleUtils.class,
                Mockito.CALLS_REAL_METHODS);
        bundleUtils = Mockito.mockStatic(BundleValidationUtil.class,
                Mockito.CALLS_REAL_METHODS);
        ioUtils = Mockito.mockStatic(FileIOUtils.class,
                Mockito.CALLS_REAL_METHODS);
    }

    private void setupMode(Mode mode) {
        this.mode = mode;
        options.withProductionMode(mode.isProduction());
        bundleLocation = mode.isProduction() ? Constants.PROD_BUNDLE_NAME
                : Constants.DEV_BUNDLE_NAME;
    }

    @AfterEach
    void teardown() {
        frontendBuildUtils.close();
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

    @ParameterizedTest
    @MethodSource("modes")
    void noDevBundle_bundleCompilationRequires(Mode mode) {
        setupMode(mode);
        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), mode);
        assertTrue(needsBuild,
                "Bundle should require creation if not available");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void devBundleStatsJsonMissing_bundleCompilationRequires(Mode mode) {
        setupMode(mode);
        devBundleUtils.when(() -> DevBundleUtils
                .getDevBundleFolder(Mockito.any(), Mockito.any()))
                .thenReturn(temporaryFolder);
        devBundleUtils.when(() -> DevBundleUtils
                .findBundleStatsJson(temporaryFolder, "target"))
                .thenReturn(null);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), mode);
        assertTrue(needsBuild, "Missing stats.json should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_noNpmPackages_noCompilationRequired(Mode mode)
            throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "Matching hashes should not require compilation");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void loadDependenciesOnStartup_annotatedClassInProject_compilationRequiredForProduction(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertTrue(needsBuild,
                "'LoadDependenciesOnStartup' annotation requires build");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_statsMissingNpmPackages_compilationRequired(Mode mode)
            throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertTrue(needsBuild, "Missing npmPackage should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_statsMissingPackageJsonPackage_compilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertTrue(needsBuild,
                "Bundle missing module dependency should rebuild");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_packageJsonMissingNpmPackages_statsHasJsonPackages_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "Not missing npmPackage in stats.json should not require compilation");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void packageJsonContainsOldVersion_versionsJsonUpdates_noCompilation(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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

        File versions = new File(temporaryFolder,
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
        assertFalse(needsBuild,
                "vaadin-core-versions.json should have updated version to expected.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void packageJsonContainsOldVersionsAfterVersionUpdate_updatedStatsMatches_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "Not missing npmPackage in stats.json should not require compilation");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noPackageJsonHashAfterCleanFrontend_statsHasDefaultJsonPackages_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "Not missing npmPackage in stats.json should not require compilation");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noPackageJsonHashAfterCleanFrontend_statsMissingDefaultJsonPackages_compilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertTrue(needsBuild,
                "Missing npmPackage in stats.json should require compilation");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_packageJsonHasRange_statsHasFixed_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "Not missing npmPackage in stats.json should not require compilation");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_packageJsonHasTildeRange_statsHasNewerFixed_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "No compilation if tilde range only patch update");

        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "1.8.1");
        setupFrontendUtilsMock(stats);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner, mode);
        assertTrue(needsBuild,
                "Compilation required if minor version change for tilde range");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void hashesMatch_packageJsonHasCaretRange_statsHasNewerFixed_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "No compilation if caret range only minor version update");

        ((ObjectNode) stats.get(PACKAGE_JSON_DEPENDENCIES))
                .put("@vaadin/router", "2.0.0");
        setupFrontendUtilsMock(stats);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner, mode);
        assertTrue(needsBuild,
                "Compilation required if major version change for caret range");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void packageJsonHasOldPlatformDependencies_statsDoesNotHaveThem_noCompilationRequired(
            Mode mode) throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "No compilation expected if package.json has "
                        + "only dependencies from older Vaadin version not "
                        + "presenting in a newer version");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noPackageJson_defaultPackagesAndModulesInStats_noBuildNeeded(
            Mode mode) {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "Default package.json should be built and validated");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noPackageJson_defaultPackagesInStats_missingNpmModules_buildNeeded(
            Mode mode) {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Missing NpmPackage with default bundle should require rebuild");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noPackageJson_defaultPackagesInStats_noBuildNeeded(Mode mode) {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "Default package.json should be built and validated");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void generatedFlowImports_bundleMissingImports_buildRequired(Mode mode)
            throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertTrue(needsBuild,
                "Compilation required as stats.json missing import");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void generatedFlowImports_bundleHasAllImports_noBuildRequired(Mode mode)
            throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "All imports in stats, no compilation required");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themedGeneratedFlowImports_bundleUsesTheme_noBuildRequired(Mode mode)
            throws IOException {
        setupMode(mode);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild,
                "All themed imports in stats, no compilation required");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void frontendFileHashMatches_noBundleRebuild(Mode mode) throws IOException {
        setupMode(mode);
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder, "package.json");
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
                .thenReturn(temporaryFolder);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild, "Jar fronted file content hash should match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noFrontendFileHash_bundleRebuild(Mode mode) throws IOException {
        setupMode(mode);
        String fileContent = "TodoContent";

        File packageJson = new File(temporaryFolder, "package.json");
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
                .thenReturn(temporaryFolder);
        frontendBuildUtils
                .when(() -> FrontendBuildUtils.getJarResourceString(
                        Mockito.eq("TodoTemplate.js"),
                        Mockito.any(ClassFinder.class)))
                .thenReturn(fileContent);
        devBundleUtils.when(() -> DevBundleUtils
                .findBundleStatsJson(temporaryFolder, "target"))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild, "Content should not have been validated.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void frontendFileHashMissmatch_bundleRebuild(Mode mode) throws IOException {
        setupMode(mode);
        String fileContent = "TodoContent2";

        File packageJson = new File(temporaryFolder, "package.json");
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
                .thenReturn(temporaryFolder);
        frontendBuildUtils
                .when(() -> FrontendBuildUtils.getJarResourceString(
                        Mockito.eq("TodoTemplate.js"),
                        Mockito.any(ClassFinder.class)))
                .thenReturn(fileContent);
        devBundleUtils.when(() -> DevBundleUtils
                .findBundleStatsJson(temporaryFolder, "target"))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "Jar fronted file content hash should not be a match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void cssImportWithInline_statsAndImportsMatchAndNoBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File stylesheetFile = new File(temporaryFolder,
                DEFAULT_FRONTEND_DIR + "my-styles.css");
        FileUtils.forceMkdir(stylesheetFile.getParentFile());
        boolean created = stylesheetFile.createNewFile();
        assertTrue(created);
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
        assertFalse(needsBuild,
                "CSS 'inline' suffix should be ignored for imports checking");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectFrontendFileChange_bundleRebuild(Mode mode) throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Project frontend file change should trigger rebuild");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectFrontendFileNotChanged_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "No bundle rebuild expected when no changes in frontend file");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectFrontendFileDeleted_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Project frontend file delete should trigger rebuild");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reusedTheme_noReusedThemes_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        stats.remove(THEME_JSON_CONTENTS);

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild,
                "Shouldn't rebuild the bundle if no reused themes");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reusedTheme_newlyAddedTheme_noThemeJson_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        File jarWithTheme = TestUtils.getTestJar("jar-with-no-theme-json.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        // create custom-theme folder with no theme.json
        File jarResourcesFolder = new File(temporaryFolder, DEFAULT_FRONTEND_DIR
                + "generated/jar-resources/themes/custom-theme");
        boolean created = jarResourcesFolder.mkdirs();
        assertTrue(created);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        setupFrontendUtilsMock(getBasicStats());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild,
                "Should not trigger a bundle rebuild when the new theme has no theme.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reusedTheme_noPreviouslyAddedThemes_justAddedNewTheme_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File jarWithTheme = TestUtils
                .getTestJar("jar-with-theme-json-and-assets.jar");
        options.copyResources(Collections.singleton(jarWithTheme));

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        setupFrontendUtilsMock(getBasicStats());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "Should trigger a bundle rebuild when a new reusable theme is added");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reusedTheme_previouslyAddedThemes_justAddedNewTheme_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should trigger a bundle rebuild when a new reusable theme is added");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reusedTheme_previouslyAddedThemes_assetsUpdate_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should trigger a bundle rebuild when the assets updated");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reusedTheme_previouslyAddedThemes_noUpdates_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "Should not trigger a bundle rebuild when the themes not changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_statsHasNoThemeJson_projectHasThemeJson_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should trigger a bundle rebuild when no themeJsonContents, but project has theme.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_containsParentTheme_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("{\"parent\": \"my-parent-theme\"}",
                "my-theme");
        new File(temporaryFolder,
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
        assertFalse(needsBuild,
                "Should not trigger a bundle rebuild when parent theme is used");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_statsHasThemeJson_projectHasNoThemeJson_noBundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        final ThemeDefinition themeDefinition = Mockito
                .mock(ThemeDefinition.class);
        Mockito.when(themeDefinition.getName()).thenReturn("my-theme");
        Mockito.when(depScanner.getThemeDefinition())
                .thenReturn(themeDefinition);
        new File(temporaryFolder, DEFAULT_FRONTEND_DIR + "themes/my-theme")
                .mkdirs();

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                "{\"lumoImports\": [\"typography\"]}");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild,
                "Should not trigger a bundle rebuild when project has no theme.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_statsAndProjectThemeJsonEquals_noBundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "Should not trigger a bundle rebuild when project theme.json has the same content as in the bundle");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_bundleMissesSomeEntries_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub(
                """
                        {
                          "importCss": ["@fortawesome/fontawesome-free/css/all.css",
                            "@vaadin/vaadin-lumo-styles/utility.css"],
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
                          "importCss": ["@vaadin/vaadin-lumo-styles/utility.css"],
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
        assertTrue(needsBuild,
                "Should rebuild when project theme.json adds extra entries");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_bundleHaveAllEntriesAndMore_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);
        createProjectThemeJsonStub("""
                {
                  "importCss": ["@vaadin/vaadin-lumo-styles/utility.css"]
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
        ((ObjectNode) stats.get(THEME_JSON_CONTENTS)).put(bundleLocation,
                """
                        {
                          "importCss": ["@vaadin/vaadin-lumo-styles/utility.css"],
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
        assertFalse(needsBuild,
                "Shouldn't re-bundle when the dev bundle already have all"
                        + " the entries defined in the project's theme.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void themeJsonUpdates_noProjectThemeHashInStats_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should trigger a bundle rebuild when project has theme"
                        + ".json but stats doesn't");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void parentThemeInFrontend_parentHasEntriesInJson_bundleMissesSomeEntries_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should rebuild when 'theme.json' from parent theme in "
                        + "frontend folder adds extra entries");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectThemeComponentsCSS_contentsAdded_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                false, false);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in '" + DEFAULT_FRONTEND_DIR
                        + "<theme>/components' folder");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectThemeComponentsCSS_contentsChanged_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                true, true);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents have changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectThemeComponentsCSS_contentsNotChanged_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                false, true);
        assertFalse(needsBuild,
                "Should not rebuild when Shadow DOM Stylesheets contents have not changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectThemeComponentsCSS_removedFromProject_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForProjectThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents have been removed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void jarResourceThemeComponentsCSS_contentsAdded_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                false, false);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in 'frontend/generated/jar-resources/<theme>/components' folder");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void jarResourceThemeComponentsCSS_contentsChanged_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                true, true);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents have changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void jarResourceThemeComponentsCSS_contentsNotChanged_noBundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                false, true);
        assertFalse(needsBuild,
                "Should not rebuild when Shadow DOM Stylesheets contents have not changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void jarResourceThemeComponentsCSS_removedFromProject_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents have been removed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentThemeComponentsCSS_contentsAdded_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                false, false);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in parent theme folder");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentThemeComponentsCSS_contentsChanged_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                true, true);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentThemeComponentsCSS_contentsNotChanged_noBundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                false, true);
        assertFalse(needsBuild,
                "Should not rebuild when Shadow DOM Stylesheets contents in parent theme have not changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentThemeComponentsCSS_removedFromProject_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForParentProjectThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have been removed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentInJarThemeComponentsCSS_contentsAdded_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                false, false);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets are present "
                        + " in parent theme folder");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentInJarThemeComponentsCSS_contentsChanged_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                true, true);
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentInJarThemeComponentsCSS_contentsNotChanged_noBundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                false, true);
        assertFalse(needsBuild,
                "Should not rebuild when Shadow DOM Stylesheets contents in parent theme have not changed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectParentInJarThemeComponentsCSS_removedFromProject_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        boolean needsBuild = checkBundleRebuildForJarPackagedParentThemeComponentsCSS(
                false, true, "deleted-component-stylesheet.css");
        assertTrue(needsBuild,
                "Should rebuild when Shadow DOM Stylesheets contents in parent theme have been removed");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void projectThemeComponentsCSS_noThemeJson_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        // Theme has components/ folder with CSS but no theme.json
        String cssTemplate = "[part=\"input-field\"]{background: %s; }";
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        // Create components CSS file without creating theme.json
        String themeLocation = "themes/my-theme/components/";
        File stylesheetFile = new File(temporaryFolder,
                DEFAULT_FRONTEND_DIR + themeLocation + "vaadin-text-field.css");
        FileUtils.forceMkdir(stylesheetFile.getParentFile());
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
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "Should rebuild when theme has components CSS but no theme.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void indexTsAdded_rebuildRequired(Mode mode) throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = newFolder(temporaryFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR);

        File indexTs = new File(frontendFolder, FrontendUtils.INDEX_TS);
        indexTs.createNewFile();

        FileUtils.write(indexTs, "window.alert('');", StandardCharsets.UTF_8);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild, "Adding 'index.ts' should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void changeInIndexTs_rebuildRequired(Mode mode) throws IOException {
        setupMode(mode);
        createPackageJsonStub(
                "{\"dependencies\": {}, \"vaadin\": { \"hash\": \"aHash\"} }");
        File frontendFolder = newFolder(temporaryFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR);

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
        assertFalse(needsBuild,
                "'index.ts' equal content should not require bundling");

        FileUtils.write(indexTs, "window.alert('hello');",
                StandardCharsets.UTF_8);

        needsBuild = BundleValidationUtil.needsBuild(options, depScanner, mode);
        assertTrue(needsBuild,
                "changed content for 'index.ts' should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void indexTsDeleted_rebuildRequired(Mode mode) throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(FrontendUtils.INDEX_TS,
                "15931fa8c20e3c060c8ea491831e95cc8463962700a9bfb82c8e3844cf608f04");

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild, "'index.ts' delete should require re-bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void indexHtmlNotChanged_rebuildNotRequired(Mode mode) throws IOException {
        setupMode(mode);
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = newFolder(temporaryFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR);

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
        assertFalse(needsBuild,
                "Default 'index.html' should not require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void indexHtmlChanged_productionMode_rebuildRequired(Mode mode)
            throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = newFolder(temporaryFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR);

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
        assertTrue(needsBuild,
                "In production mode, custom 'index.html' should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void indexHtmlChanged_developmentMode_rebuildNotRequired(Mode mode)
            throws IOException {
        setupMode(mode);
        assumeFalse(mode.isProduction());
        createPackageJsonStub(BLANK_PACKAGE_JSON_WITH_HASH);

        File frontendFolder = newFolder(temporaryFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR);

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
        assertFalse(needsBuild,
                "In dev mode, custom 'index.html' should not require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void standardVaadinComponent_notAddedToProjectAsJar_noRebuildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "Should not require bundling if component JS is missing in jar-resources");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void cssImport_cssInMetaInfResources_notThrow_bundleRequired(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should re-bundle if CSS is imported from META-INF/resources");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void flowFrontendPackageInPackageJson_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
        createPackageJsonStub(
                "{\"dependencies\": {\"@vaadin/flow-frontend\": \"./target/flow-frontend\"}, \"vaadin\": { \"hash\": \"aHash\"} }");

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();

        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild,
                "Shouldn't re-bundle when old @vaadin/flow-frontend package is in package.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void localPackageInPackageJson_notChanged_noBundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertFalse(needsBuild,
                "Shouldn't re-bundle when referencing local packages in package.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void localPackageInPackageJson_differentReference_bundleRebuild(Mode mode)
            throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should re-bundle when local packages have different values");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void localPackageInPackageJson_parsableVersionInStats_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should re-bundle when local package in package.json but parsable version in stats");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void localPackageInStats_parsableVersionInPackageJson_bundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
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
        assertTrue(needsBuild,
                "Should re-bundle when local package in stats but parsable version in package.json");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void bundleMissesSomeEntries_devMode_skipBundleBuildSet_noBundleRebuild(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode == Mode.DEVELOPMENT_BUNDLE);
        options.skipDevBundleBuild(true);

        File packageJson = new File(temporaryFolder, "package.json");
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
        assertFalse(needsBuild, "Rebuild should be skipped");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void forceProductionBundle_bundleRequired(Mode mode) {
        setupMode(mode);
        assumeTrue(mode.isProduction());

        options.withForceProductionBuild(true);

        final boolean needsBuild = BundleValidationUtil.needsBuild(options,
                Mockito.mock(FrontendDependenciesScanner.class), mode);
        assertTrue(needsBuild,
                "Production bundle required due to force.production.bundle flag.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noDevFolder_compressedDevBundleExists_noBuildRequired(Mode mode)
            throws IOException {
        setupMode(mode);
        assumeTrue(!mode.isProduction());

        File packageJson = new File(temporaryFolder, "package.json");
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

        File bundleSourceFolder = newFolder(temporaryFolder, "compiled");

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

        DevBundleUtils.compressBundle(temporaryFolder, bundleSourceFolder);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild, "Jar fronted file content hash should match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void compressedProdBundleExists_noBuildRequired(Mode mode)
            throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder, "package.json");
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

        File bundleSourceFolder = newFolder(temporaryFolder, "compiled");

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

        ProdBundleUtils.compressBundle(temporaryFolder, bundleSourceFolder);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild, "Jar fronted file content hash should match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void noFileBundleOrJar_compressedBundleExists_noBuildRequired(Mode mode)
            throws IOException {
        setupMode(mode);
        File packageJson = new File(temporaryFolder, "package.json");
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

        File bundleSourceFolder = newFolder(temporaryFolder, "compiled");

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
            ProdBundleUtils.compressBundle(temporaryFolder, bundleSourceFolder);
            Mockito.when(finder
                    .getResource(PROD_BUNDLE_JAR_PATH + "config/stats.json"))
                    .thenReturn(null);
        } else {
            DevBundleUtils.compressBundle(temporaryFolder, bundleSourceFolder);
            Mockito.when(finder
                    .getResource(DEV_BUNDLE_JAR_PATH + "config/stats.json"))
                    .thenReturn(null);
        }

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild, "Jar frontend file content hash should match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void defaultDevBundleExists_noCompressedDevBundleFile_reactDisabled_buildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        options.withReact(false);
        assumeTrue(!mode.isProduction());

        File packageJson = new File(temporaryFolder, "package.json");
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
        ioUtils.when(() -> FileIOUtils.urlToString(url))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "Dev bundle build is expected when react is disabled and using otherwise default dev bundle.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void defaultProdBundleExists_noCompressedProdBundleFile_noBuildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder, "package.json");
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
        ioUtils.when(() -> FileIOUtils.urlToString(url))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild, "Jar frontend file content hash should match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void defaultProdBundleExists_noCompressedProdBundleFileAndWithVersionsJsonExclusions_noBuildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());
        frontendBuildUtils.when(
                () -> FrontendBuildUtils.isReactModuleAvailable(Mockito.any()))
                .thenAnswer(q -> true);

        File packageJson = new File(temporaryFolder, "package.json");
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

        File versions = new File(temporaryFolder,
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
        ioUtils.when(() -> FileIOUtils.urlToString(url))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild, "Jar frontend file content hash should match.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void defaultProdBundleExists_noCompressedProdBundleFile_reactDisabled_buildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        options.withReact(false);
        assumeTrue(mode.isProduction());

        File packageJson = new File(temporaryFolder, "package.json");
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
        ioUtils.when(() -> FileIOUtils.urlToString(url))
                .thenReturn(stats.toString());

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "Prod bundle build is expected when react is disabled and using otherwise default prod bundle.");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void commercialBannerBuild_commercialBannerComponentMissing_rebuildRequired(
            Mode mode) {
        setupMode(mode);
        assumeTrue(mode.isProduction());
        options.withCommercialBanner(true);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        ObjectNode stats = getBasicStats();
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "In commercial banner build mode, missing 'commercial-banner.js' should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void commercialBannerBuild_commercialBannerComponentChanged_rebuildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());
        options.withCommercialBanner(true);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        String defaultCommercialBannerJS = new String(getClass()
                .getResourceAsStream(FrontendUtils.COMMERCIAL_BANNER_JS)
                .readAllBytes(), StandardCharsets.UTF_8);
        String oldCommercialBannerJS = defaultCommercialBannerJS.replace(
                "vaadin-commercial-banner", "vaadin-commercial-banner-old");
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(
                FrontendUtils.GENERATED + FrontendUtils.COMMERCIAL_BANNER_JS,
                BundleValidationUtil.calculateHash(oldCommercialBannerJS));
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "In commercial banner build mode, modified 'commercial-banner.js' should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void commercialBannerBuild_commercialBannerComponentNotChanged_rebuildNotRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());
        options.withCommercialBanner(true);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        String defaultCommercialBannerJS = new String(getClass()
                .getResourceAsStream(FrontendUtils.COMMERCIAL_BANNER_JS)
                .readAllBytes(), StandardCharsets.UTF_8);
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(
                FrontendUtils.GENERATED + FrontendUtils.COMMERCIAL_BANNER_JS,
                BundleValidationUtil.calculateHash(defaultCommercialBannerJS));
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild,
                "In commercial banner build mode, unmodified 'commercial-banner.js' should not require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void nonCommercialBannerBuild_commercialBannerComponentPresent_rebuildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(mode.isProduction());
        options.withCommercialBanner(false);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        File frontendGeneratedFolder = newFolder(temporaryFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR + FrontendUtils.GENERATED);
        File commercialBannerJS = new File(frontendGeneratedFolder,
                FrontendUtils.COMMERCIAL_BANNER_JS);
        commercialBannerJS.createNewFile();
        String defaultCommercialBannerJS = new String(getClass()
                .getResourceAsStream(FrontendUtils.COMMERCIAL_BANNER_JS)
                .readAllBytes(), StandardCharsets.UTF_8);
        FileUtils.write(commercialBannerJS, defaultCommercialBannerJS,
                StandardCharsets.UTF_8);
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(
                FrontendUtils.GENERATED + FrontendUtils.COMMERCIAL_BANNER_JS,
                BundleValidationUtil.calculateHash(defaultCommercialBannerJS));
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "In non commercial banner build mode, presence of 'commercial-banner.js' should require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void developmentMode_commercialBannerComponentNotPresent_rebuildNotRequired(
            Mode mode) {
        setupMode(mode);
        assumeTrue(!mode.isProduction());
        options.withCommercialBanner(true);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        ObjectNode stats = getBasicStats();
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertFalse(needsBuild,
                "In development mode, absence of 'commercial-banner.js' should not require bundling");
    }

    @ParameterizedTest
    @MethodSource("modes")
    void developmentMode_commercialBannerComponentPresent_rebuildRequired(
            Mode mode) throws IOException {
        setupMode(mode);
        assumeTrue(!mode.isProduction());
        options.withCommercialBanner(true);

        final FrontendDependenciesScanner depScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        String defaultCommercialBannerJS = new String(getClass()
                .getResourceAsStream(FrontendUtils.COMMERCIAL_BANNER_JS)
                .readAllBytes(), StandardCharsets.UTF_8);
        ObjectNode stats = getBasicStats();
        ((ObjectNode) stats.get(FRONTEND_HASHES)).put(
                FrontendUtils.GENERATED + FrontendUtils.COMMERCIAL_BANNER_JS,
                BundleValidationUtil.calculateHash(defaultCommercialBannerJS));
        setupFrontendUtilsMock(stats);

        boolean needsBuild = BundleValidationUtil.needsBuild(options,
                depScanner, mode);
        assertTrue(needsBuild,
                "In development mode, presence of 'commercial-banner.js' should require bundling");
    }

    private void createPackageJsonStub(String content) throws IOException {
        File packageJson = new File(temporaryFolder, Constants.PACKAGE_JSON);
        boolean created = packageJson.createNewFile();
        assertTrue(created);
        FileUtils.write(packageJson, content, StandardCharsets.UTF_8);
    }

    private void createProjectThemeJsonStub(String content, String theme)
            throws IOException {
        createThemeJsonStub(content, theme, true);
    }

    private void createThemeJsonStub(String content, String theme,
            boolean projectTheme) throws IOException {
        String themeLocation = projectTheme ? "" : "generated/jar-resources/";
        File themeJson = new File(temporaryFolder, DEFAULT_FRONTEND_DIR
                + themeLocation + "themes/" + theme + "/theme.json");
        FileUtils.forceMkdir(themeJson.getParentFile());
        boolean created = themeJson.createNewFile();
        assertTrue(created);
        FileUtils.write(themeJson, content, StandardCharsets.UTF_8);
    }

    private void createProjectFrontendFileStub() throws IOException {
        File frontendFile = new File(temporaryFolder,
                DEFAULT_FRONTEND_DIR + "views/lit-view.ts");
        FileUtils.forceMkdir(frontendFile.getParentFile());
        boolean created = frontendFile.createNewFile();
        assertTrue(created);
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
            devBundleUtils.when(() -> DevBundleUtils
                    .getDevBundleFolder(Mockito.any(), Mockito.any()))
                    .thenReturn(temporaryFolder);
            devBundleUtils
                    .when(() -> DevBundleUtils
                            .findBundleStatsJson(temporaryFolder, "target"))
                    .thenAnswer(q -> stats.toString());
        }
        frontendBuildUtils
                .when(() -> FrontendBuildUtils.getJarResourceString(
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
        File stylesheetFile = new File(temporaryFolder,
                DEFAULT_FRONTEND_DIR + themeLocation + "vaadin-text-field.css");
        FileUtils.forceMkdir(stylesheetFile.getParentFile());
        boolean created = stylesheetFile.createNewFile();
        assertTrue(created);
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

    private static File newFolder(File parent, String... paths) {
        File folder = new File(parent, String.join(File.separator, paths));
        folder.mkdirs();
        return folder;
    }

}
