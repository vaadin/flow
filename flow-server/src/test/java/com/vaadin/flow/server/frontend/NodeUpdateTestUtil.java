/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.DevModeHandler.WEBPACK_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;
import static org.junit.Assert.assertNotNull;

public class NodeUpdateTestUtil {

    public static final String WEBPACK_TEST_OUT_FILE = "webpack-out.test";

    private static final String NPM_BIN_PATH = "node/node_modules/npm/bin";
    private static final String NPM_CACHE_PATH_STUB = "cache";

    static ClassFinder getClassFinder() throws MalformedURLException {
        return new DefaultClassFinder(new URLClassLoader(getClassPath()),
                NodeTestComponents.class.getDeclaredClasses());
    }

    static ClassFinder getClassFinder(Class<?>... classes) throws MalformedURLException {
        return new DefaultClassFinder(new URLClassLoader(getClassPath()),
                classes);
    }

    static URL[] getClassPath() throws MalformedURLException {
        // Add folder with test classes
        List<URL> classPaths = new ArrayList<>();

        classPaths.add(new File("target/test-classes").toURI().toURL());

        // Add this test jar which has some frontend resources used in tests
        URL jar = getTestResource("jar-with-frontend-resources.jar");
        classPaths.add(jar);

        // Add other paths already present in the system classpath
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL[] urls = URLClassLoader.newInstance(new URL[] {}, classLoader)
                .getURLs();
        for (URL url : urls) {
            classPaths.add(url);
        }
        return classPaths.toArray(new URL[0]);
    }

    /**
     * Creates stub versions of `node` and `npm` in the ./node folder as
     * frontend-maven-plugin does.
     *
     * @param stubNode
     *            node stub information, including whether `node/node`
     *            (`node/node.exe`) stub should be created, and what version
     *            should it output if '-v' || '--version' flag are set.
     * @param stubNpm
     *            npm stub information, including whether
     *            `node/node_modules/npm/bin/npm-cli` and `npx-cli` should be
     *            created, and what version should it output if '-v' ||
     *            '--version' flag are set.
     * @param baseDir
     *            parent to create `node` dir in
     * @throws IOException
     *             when a file operation fails
     */
    public static void createStubNode(ToolStubInfo stubNode,
                                      ToolStubInfo stubNpm, String baseDir) throws IOException {

        Objects.requireNonNull(stubNode);
        Objects.requireNonNull(stubNpm);

        if (stubNpm.isStubbed()) {
            File binDir = new File(baseDir, NPM_BIN_PATH);
            FileUtils.forceMkdir(binDir);
            String stub = stubNpm.getScript().replace("\\", "\\\\");
            FileUtils.writeStringToFile(new File(binDir, "npm-cli.js"), stub,
                    StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(new File(binDir, "npx-cli.js"), stub,
                    StandardCharsets.UTF_8);
        }
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");

        if (stubNode.isStubbed()) {
            File nodeDir = new File(baseDir, "node");
            FileUtils.forceMkdir(nodeDir);
            File node = new File(baseDir,
                    isWindows ? "node/node.exe" : "node/node");
            node.createNewFile();
            node.setExecutable(true);
            if (isWindows) {
                // Commented out until a node.exe is created that is not flagged
                // by Windows defender.
                // FileUtils.copyFile(new File(
                // getClassFinder().getClass().getClassLoader().getResource("test_node.exe").getFile()
                // ), node);
            } else {
                FileUtils.write(node, stubNode.getScript(), "UTF-8");
            }
        }
    }

    /**
     * Creates stub versions of `node` and `npm` in the ./node folder as
     * frontend-maven-plugin does. This method creates a default stubs for
     * tool's versions.
     *
     * @param stubNode
     *            whether `node/node` (`node/node.exe`) stub should be created
     * @param stubNpm
     *            whether `node/node_modules/npm/bin/npm-cli` and `npx-cli`
     *            should be created
     * @param baseDir
     *            parent to create `node` dir in
     * @throws IOException
     *             when a file operation fails
     */
    public static void createStubNode(boolean stubNode, boolean stubNpm,
                                      String baseDir) throws IOException {
        final File defaultNpmCacheDirStub = new File(
                new File(baseDir, NPM_BIN_PATH), NPM_CACHE_PATH_STUB);
        FileUtils.forceMkdir(defaultNpmCacheDirStub);

        ToolStubInfo nodeStubInfo = stubNode ? ToolStubInfo.builder(Tool.NODE)
                .withCacheDir(defaultNpmCacheDirStub.getAbsolutePath()).build()
                : ToolStubInfo.none();
        ToolStubInfo npmStubInfo = stubNpm ? ToolStubInfo.builder(Tool.NPM)
                .withCacheDir(defaultNpmCacheDirStub.getAbsolutePath()).build()
                : ToolStubInfo.none();
        createStubNode(nodeStubInfo, npmStubInfo, baseDir);
    }

    // Creates a stub webpack-dev-server able to output a ready string, sleep
    // for a while and output arguments passed to a file, so as tests can check
    // it
    public static void createStubWebpackServer(String readyString,
            int milliSecondsToRun, String baseDir) throws IOException {
        File serverFile = new File(baseDir, WEBPACK_SERVER);
        FileUtils.forceMkdirParent(serverFile);

        serverFile.createNewFile();
        serverFile.setExecutable(true);
        FileUtils.write(serverFile,
                ("#!/usr/bin/env node\n" + "const fs = require('fs');\n"
                        + "const args = String(process.argv);\n"
                        + "fs.writeFileSync('" + WEBPACK_TEST_OUT_FILE
                        + "', args);\n" + "console.log(args + '\\n[wps]: "
                        + readyString + ".');\n" + "setTimeout(() => {}, "
                        + milliSecondsToRun + ");\n"),
                "UTF-8");
    }

    static URL getTestResource(String resourceName) {
        URL resourceUrl = NodeUpdateTestUtil.class.getClassLoader()
                .getResource(resourceName);
        assertNotNull(String.format(
                "Expect the test resource to be present in test resource folder with name = '%s'",
                resourceName), resourceUrl);
        return resourceUrl;
    }

    void sleep(int ms) throws InterruptedException {
        Thread.sleep(ms); // NOSONAR
    }

    List<String> getExpectedImports() {
        return Arrays.asList("@polymer/iron-icon/iron-icon.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/icons.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-date-picker/theme/lumo/vaadin-date-picker.js",
                "@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js",
                "@vaadin/vaadin-element-mixin/vaadin-element-mixin.js",
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else.js",
                "./theme/lumo/vaadin-custom-themed-component.js",
                "@vaadin/flow-frontend/ExampleConnector.js",
                "3rdparty/component.js", "./foo-dir/javascript-lib.js",
                "./frontend-p3-template.js", "./local-p3-template.js",
                "./foo.js",
                "./vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "./local-template.js", "./foo-dir/vaadin-npm-component.js",
                "./foo.css", "@vaadin/vaadin-mixed-component/bar.css",
                "./common-js-file.js");
    }

    void createExpectedImports(File directoryWithImportsJs,
            File nodeModulesPath) throws IOException {
        for (String expectedImport : getExpectedImports()) {
            File newFile = resolveImportFile(directoryWithImportsJs,
                    nodeModulesPath, expectedImport);
            newFile.getParentFile().mkdirs();
            Assert.assertTrue(newFile.createNewFile());
        }
    }

    void deleteExpectedImports(File directoryWithImportsJs,
            File nodeModulesPath) {
        for (String expectedImport : getExpectedImports()) {
            Assert.assertTrue(resolveImportFile(directoryWithImportsJs,
                    nodeModulesPath, expectedImport).delete());
        }
    }

    File resolveImportFile(File directoryWithImportsJs, File nodeModulesPath,
            String jsImport) {
        File root = jsImport.startsWith("./") ? directoryWithImportsJs
                : nodeModulesPath;
        return new File(root, jsImport);
    }

    String addWebpackPrefix(String s) {
        if (s.startsWith("./")) {
            return WEBPACK_PREFIX_ALIAS + s.substring(2);
        }
        return s;
    }

    /**
     * Holds an information about build tool to be stubbed.
     */
    public static final class ToolStubInfo {
        private final boolean stubbed;
        private final String script;

        private ToolStubInfo(boolean stubbed, String script) {
            this.stubbed = stubbed;
            assert !stubbed || (script != null && !script
                    .isEmpty()) : "Script may not be empty for stubbed tool";
            this.script = script;
        }

        /**
         * Creates a new builder for constructing a new instance of tool stub
         * info.
         *
         * @param tool
         *            the build tool to create a stub for.
         *
         * @return a new tool stub info builder.
         */
        public static ToolStubBuilder builder(Tool tool) {
            Objects.requireNonNull(tool, "Build tool may not be empty");
            return new ToolStubBuilder(tool);
        }

        /**
         * Returns a dummy tool stub info, which denotes no stub is used.
         *
         * @return a tool stub info for non-used stub.
         */
        public static ToolStubInfo none() {
            return new ToolStubInfo(false, "");
        }

        public boolean isStubbed() {
            return stubbed;
        }

        public String getScript() {
            return script;
        }
    }

    /**
     * Builds a new instance of {@link ToolStubInfo}.
     */
    public static class ToolStubBuilder {

        private static final String DEFAULT_NPM_VERSION = "6.14.10";
        private static final String DEFAULT_NODE_VERSION = "8.0.0";

        private String version;
        private String cacheDir;
        private final Tool tool;

        private ToolStubBuilder(Tool tool) {
            this.tool = tool;
        }

        /**
         * Adds a stub for tool version. The version will be returned if a tool
         * executable is called with '-v' or '--version' argument. If no value
         * is set, the default one is used.
         *
         * @param version
         *            a tool version to stub.
         * @return a tool stub info builder.
         */
        public ToolStubBuilder withVersion(String version) {
            this.version = version;
            return this;
        }

        /**
         * Adds a stub for npm cache directory. The path to cache will be
         * returned if a tool executable is called with 'cache' argument. If no
         * value is set, the default one is used.
         *
         * @param cacheDir
         *            a npm cache dir to stub.
         *
         * @return a tool stub info builder.
         */
        public ToolStubBuilder withCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        /**
         * Builds a new instance of tool stub info.
         *
         * @return a new tool stub info instance.
         */
        public ToolStubInfo build() {
            String script;
            final StringBuilder scriptBuilder = new StringBuilder();

            switch (tool) {
                case NPM:
                    script = generateNpmScript(scriptBuilder);
                    break;
                case NODE:
                    script = generateNodeScript(scriptBuilder);
                    break;
                default:
                    throw new IllegalStateException("Unknown build tool");
            }
            return new ToolStubInfo(true, script);
        }

        private String generateNodeScript(StringBuilder scriptBuilder) {
            String script;
            // default version used in tests
            version = version == null ? DEFAULT_NODE_VERSION : version;
            scriptBuilder.append(
                    "  if [ \"$arg\" = \"--version\" ] || [ \"$arg\" = \"-v\" ]; then\n")
                    .append("    echo ").append(version).append("\n")
                    .append("    break\n").append("  fi\n");
            if (cacheDir != null) {
                scriptBuilder.append("  if [ \"$arg\" = \"cache\" ]; then\n")
                        .append("    echo ").append(cacheDir).append("\n")
                        .append("    break\n").append("  fi\n");
            }
            // @formatter:off
            script = String.format(
                    "#!/bin/sh%n"
                    + "for arg in \"$@\"%n"
                    + "do%n"
                    + "%s"
                    + "done%n"
                    + "sleep 1", scriptBuilder.toString());
            // @formatter:on
            return script;
        }

        private String generateNpmScript(StringBuilder scriptBuilder) {
            String script;
            // default version used in tests
            version = version == null ? DEFAULT_NPM_VERSION : version;
            scriptBuilder.append(
                    "process.argv.includes('--version') && console.log('")
                    .append(version).append("');\n");
            if (cacheDir != null) {
                scriptBuilder.append(
                        "process.argv.includes('cache') && console.log('")
                        .append(cacheDir).append("');\n");
            }
            script = scriptBuilder.toString();
            return script;
        }
    }

    /**
     * Types of build tools.
     */
    public enum Tool {
        NODE, NPM
    }

}
