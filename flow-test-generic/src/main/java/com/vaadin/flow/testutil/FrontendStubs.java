/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.testutil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

/**
 * Utility class for stubbing Node.JS and frontend tooling.
 */
public class FrontendStubs {

    public static final String VITE_SERVER = "node_modules/vite/bin/vite.js";
    public static final String VITE_PACKAGE_JSON = "node_modules/vite/package.json";
    public static final String VITE_TEST_OUT_FILE = "vite-out.test";

    private static final String NPM_BIN_PATH = System.getProperty("os.name")
            .startsWith("Windows") ? "node/node_modules/npm/bin/"
                    : "node/lib/node_modules/npm/bin/";
    private static final String NPM_CACHE_PATH_STUB = "cache";

    /**
     * Only static methods.
     */
    private FrontendStubs() {
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

    /**
     * Creates a stub vite-dev-server able to output a ready string, sleep for a
     * while and output arguments passed to a file, so as tests can check it.
     *
     * @param readyString
     *            string to output
     * @param milliSecondsToRun
     *            time to keep the server running
     * @param baseDir
     *            parent directory
     * @param enableListening
     *            enable listening to port passed via `--port`
     * @throws IOException
     *             when a file operation fails
     */
    public static void createStubViteServer(String readyString,
            int milliSecondsToRun, String baseDir, boolean enableListening)
            throws IOException {
        createStubDevServer(new File(baseDir, VITE_SERVER), VITE_TEST_OUT_FILE,
                readyString, milliSecondsToRun, enableListening);

        var packageJson = new File(baseDir, VITE_PACKAGE_JSON);
        packageJson.createNewFile();
        FileUtils.write(packageJson, """
                        {
                          "name": "vite",
                          "version": "4.0.0",
                          "bin": {
                            "vite": "bin/vite.js"
                          }
                        }
                """, StandardCharsets.UTF_8);
    }

    /**
     * Creates a stub vite-dev-server able to output a ready string, sleep for a
     * while and output arguments passed to a file, so as tests can check it.
     *
     * @param readyString
     *            string to output
     * @param milliSecondsToRun
     *            time to keep the server running
     * @param baseDir
     *            parent directory
     * @throws IOException
     *             when a file operation fails
     */
    public static void createStubViteServer(String readyString,
            int milliSecondsToRun, String baseDir) throws IOException {
        createStubViteServer(readyString, milliSecondsToRun, baseDir, false);
    }

    private static void createStubDevServer(File serverFile,
            String serverOutputFile, String readyString, int milliSecondsToRun,
            boolean enableListening) throws IOException {
        FileUtils.forceMkdirParent(serverFile);

        serverFile.createNewFile();
        serverFile.setExecutable(true);

        StringBuilder sb = new StringBuilder();
        sb.append("#!/user/bin/env node\n");
        sb.append("const args = String(process.argv);\n");
        sb.append("const fs = require('fs');\n");
        sb.append("const http = require('http');\n");
        sb.append("fs.writeFileSync('").append(serverOutputFile)
                .append("', args);\n");
        if (enableListening) {
            sb.append("const port = Number.parseInt(process.argv[")
                    .append("process.argv.indexOf('--port') + 1")
                    .append("]);\n");
            sb.append("const server = new http.Server((req, res) => {\n");
            sb.append("  res.writeHead(200, {")
                    .append("'Content-Type': 'application/json',")
                    .append("});\n");
            sb.append("  res.write('{}');\n");
            sb.append("  res.end();\n");
            sb.append("});\n");
            sb.append("server.listen(port);\n");
            sb.append("setTimeout(() => server.close(), ")
                    .append(milliSecondsToRun).append(");\n");
        } else {
            sb.append("setTimeout(() => {}, ").append(milliSecondsToRun)
                    .append(");\n");
        }
        sb.append("console.log(args);\n");
        sb.append("console.log('[wps]: ").append(readyString).append(".');\n");
        FileUtils.write(serverFile, sb.toString(), "UTF-8");
    }

    /**
     * Holds an information about build tool to be stubbed.
     */
    public static final class ToolStubInfo {
        private final boolean stubbed;
        private final String script;

        private ToolStubInfo(boolean stubbed, String script) {
            this.stubbed = stubbed;
            assert !stubbed || (script != null && !script.isEmpty())
                    : "Script may not be empty for stubbed tool";
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

        private static final String DEFAULT_NPM_VERSION = "10.9.0";
        private static final String DEFAULT_NODE_VERSION = "22.12.0";

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
