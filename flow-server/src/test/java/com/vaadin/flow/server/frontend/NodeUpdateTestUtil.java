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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.DevModeHandler.WEBPACK_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;
import static org.junit.Assert.assertNotNull;

public class NodeUpdateTestUtil {

    public static final String WEBPACK_TEST_OUT_FILE = "webpack-out.test";

    static ClassFinder getClassFinder() throws MalformedURLException {
        return new DefaultClassFinder(new URLClassLoader(getClassPath()),
                NodeTestComponents.class.getDeclaredClasses());
    }

    static ClassFinder getClassFinder(Class<?>... classes)
            throws MalformedURLException {
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

    // Creates stub versions of `node` and `npm` in the ./node folder as
    // frontend-maven-plugin does
    // Also creates a stub version of webpack-devmode-server
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
        if (stubNode) {
            File node = new File(baseDir,
                    FrontendUtils.isWindows() ? "node/node.exe" : "node/node");
            node.createNewFile();
            node.setExecutable(true);
            if (FrontendUtils.isWindows()) {
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

    // Creates a stub webpack-dev-server able to output a ready string, sleep
    // for a while and output arguments passed to a file, so as tests can check
    // it
    public static void createStubWebpackServer(String readyString,
            int milliSecondsToRun, String baseDir, boolean enableListening) throws IOException {
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
                    .append("process.argv.indexOf('--port') + 1").append("]);\n");
            sb.append("const server = new http.Server((req, res) => {\n");
            sb.append("  res.writeHead(200, {")
                    .append("'Content-Type': 'application/json',").append("});\n");
            sb.append("  res.write('{}');\n");
            sb.append("  res.end();\n");
            sb.append("});\n");
            sb.append("server.listen(port);\n");
            sb.append("setTimeout(() => server.close(), ").append(milliSecondsToRun).append(");\n");
        } else {
            sb.append("setTimeout(() => {}, ").append(milliSecondsToRun).append(");\n");
        }
        sb.append("console.log(args);\n");
        sb.append("console.log('[wps]: ").append(readyString).append(".');\n");
        FileUtils.write(serverFile, sb.toString(), "UTF-8");
    }

    public static void createStubWebpackServer(String readyString,
            int milliSecondsToRun, String baseDir) throws IOException {
        createStubWebpackServer(readyString, milliSecondsToRun, baseDir, false);
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
                "3rdparty/component.js", "./local-p3-template.js", "./foo.js",
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

}
