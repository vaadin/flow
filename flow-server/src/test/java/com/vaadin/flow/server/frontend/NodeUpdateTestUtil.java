/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.flow.server.frontend.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.DevModeHandler.WEBPACK_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;
import static org.junit.Assert.assertNotNull;
public class NodeUpdateTestUtil {

    public static final String WEBPACK_TEST_OUT_FILE = "webpack-out.test";

    static ClassFinder getClassFinder()
            throws MalformedURLException {
        return new DefaultClassFinder(
                new URLClassLoader(getClassPath()),
                NodeTestComponents.class.getDeclaredClasses());
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
        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (URL url : urls) {
            classPaths.add(url);
        }

        return classPaths.toArray(new URL[0]);
    }

    // Creates stub versions of `node` and `npm` in the ./node folder as
    // frontend-maven-plugin does
    // Also creates a stub version of webpack-devmode-server
    public static void createStubNode(boolean stubNode, boolean stubNpm) throws IOException {
        // Skip in windows because shell script does not work there
        if (File.pathSeparatorChar == ';') {
            return;
        }

        if (stubNpm) {
            File npmCli = new File(getBaseDir(), "node/node_modules/npm/bin/npm-cli.js");
            FileUtils.forceMkdirParent(npmCli);
            npmCli.createNewFile();
        }
        if (stubNode) {
            File node = new File(getBaseDir(), "node/node");
            node.createNewFile();
            node.setExecutable(true);
            FileUtils.write(node, "#!/bin/sh\n[ \"$1\" = -v ] && echo 8.0.0 || sleep 1\n", "UTF-8");
        }
    }

    // Creates a stub webpack-dev-server able to output a ready string, sleep
    // for a while and output arguments passed to a file, so as tests can check it
    public static void createStubWebpackServer(String readyString, int milliSecondsToRun) throws IOException {
        File serverFile = new File(getBaseDir(), WEBPACK_SERVER);
        FileUtils.forceMkdirParent(serverFile);

        serverFile.createNewFile();
        serverFile.setExecutable(true);
        FileUtils.write(serverFile, (
            "#!/usr/bin/env node\n" +
            "const fs = require('fs');\n" +
            "const args = String(process.argv);\n" +
            "fs.writeFileSync('" + WEBPACK_TEST_OUT_FILE + "', args);\n" +
            "console.log(args + '\\n[wps]: "  + readyString + ".');\n" +
            "setTimeout(() => {}, " + milliSecondsToRun + ");\n"), "UTF-8");
    }


    // Creates a `NodeUpdatePackages` instance with a modified
    // `executeNpmInstall` method to speed up tests
    static NodeUpdatePackages createStubUpdater() throws MalformedURLException {
        File tmpRoot = new File(getBaseDir());
        File modules = new File(tmpRoot, "node_modules");

        // Create a spy version of the updater instance
        NodeUpdatePackages spy = Mockito.spy(
                new NodeUpdatePackages(
                        getClassFinder(),
                            tmpRoot, WEBPACK_CONFIG, tmpRoot, modules, true));

        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Exception {
                NodeUpdatePackages updater = (NodeUpdatePackages)invocation.getMock();
                updater.log().info("Skipping `npm install` because of test stub.");
                return null;
            }})
        .when(spy).executeNpmInstall(Mockito.any());
        return spy;
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
}
