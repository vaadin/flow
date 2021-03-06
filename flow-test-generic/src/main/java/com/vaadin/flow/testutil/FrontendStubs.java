/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import org.apache.commons.io.FileUtils;

/**
 * Utility class for stubbing Node.JS and Webpack scripts.
 */
public class FrontendStubs {

    public static final String WEBPACK_SERVER = "node_modules/webpack-dev-server/bin/webpack-dev-server.js";
    public static final String WEBPACK_TEST_OUT_FILE = "webpack-out.test";

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

        if (stubNpm) {
            File binDir = new File(baseDir, "node/node_modules/npm/bin");
            FileUtils.forceMkdir(binDir);
            String stub = "process.argv.includes('--version') && console.log('6.14.10');";
            FileUtils.writeStringToFile(new File(binDir, "npm-cli.js"), stub,
                    StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(new File(binDir, "npx-cli.js"), stub,
                    StandardCharsets.UTF_8);
        }
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");

        if (stubNode) {
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
                FileUtils.write(node,
                        "#!/bin/sh\n[ \"$1\" = -v ] && echo 8.0.0 || sleep 1\n",
                        "UTF-8");
            }
        }
    }

    /**
     * Creates a stub webpack-dev-server able to output a ready string, sleep
     * for a while and output arguments passed to a file, so as tests can check
     * it.
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
    public static void createStubWebpackServer(String readyString,
            int milliSecondsToRun, String baseDir, boolean enableListening)
            throws IOException {
        File serverFile = new File(baseDir, WEBPACK_SERVER);
        FileUtils.forceMkdirParent(serverFile);

        serverFile.createNewFile();
        serverFile.setExecutable(true);

        StringBuilder sb = new StringBuilder();
        sb.append("#!/user/bin/env node\n");
        sb.append("const args = String(process.argv);\n");
        sb.append("const fs = require('fs');\n");
        sb.append("const http = require('http');\n");
        sb.append("fs.writeFileSync('").append(WEBPACK_TEST_OUT_FILE)
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
     * Creates a stub webpack-dev-server able to output a ready string, sleep
     * for a while and output arguments passed to a file, so as tests can check
     * it.
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
    public static void createStubWebpackServer(String readyString,
            int milliSecondsToRun, String baseDir) throws IOException {
        createStubWebpackServer(readyString, milliSecondsToRun, baseDir, false);
    }
}
