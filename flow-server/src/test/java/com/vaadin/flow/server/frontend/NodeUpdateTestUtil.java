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
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.ClassFinder.DefaultClassFinder;

import static org.junit.Assert.assertNotNull;

public class NodeUpdateTestUtil {

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


    static void createStubNode(TemporaryFolder tmpDir) throws IOException {
        // Skip in windows because shell script does not work there
        if (File.pathSeparatorChar == ';') {
            return;
        }

        File npmCli = new File(tmpDir.getRoot(), "node/node_modules/npm/bin/npm-cli.js");
        FileUtils.forceMkdirParent(npmCli);
        npmCli.createNewFile();

        File node = new File(tmpDir.getRoot(), "node/node");
        node.createNewFile();
        node.setExecutable(true);
        FileUtils.write(node, "#!/bin/sh\n[ \"$1\" = -n ] && echo 8.0.0 || touch package.json\n", "UTF-8");
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
