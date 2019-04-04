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
import java.nio.charset.Charset;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.NodeUpdatePackages.WEBPACK_CONFIG;

public class NodeUpdatePackagesTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    NodeUpdatePackages node;

    File packageJson;
    File webpackConfig;

    @Before
    public void setup() throws Exception {

        File tmpRoot = temporaryFolder.getRoot();
        packageJson = new File(tmpRoot, PACKAGE_JSON);
        webpackConfig = new File(tmpRoot, WEBPACK_CONFIG);
        
        File modules = new File(tmpRoot, "node_modules");

        node = new NodeUpdatePackages(getAnnotationValuesExtractor(), 
                tmpRoot, WEBPACK_CONFIG, tmpRoot, modules, true);
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(packageJson);
        FileUtils.deleteQuietly(webpackConfig);
    }

    @Test
    public void executeNpm_packageJsonMissing() throws Exception {
        Assert.assertFalse(packageJson.exists());

        node.execute();

        assertPackageJsonContent();

        Assert.assertTrue(webpackConfig.exists());
    }

    @Test
    public void executeNpm_packageJsonExists() throws Exception {

        FileUtils.write(packageJson, "{}");
        long tsPackage1 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack1 = FileUtils.getFile(webpackConfig).lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        node.execute();
        long tsPackage2 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack2 = FileUtils.getFile(webpackConfig).lastModified();

        sleep(1000);
        node.execute();
        long tsPackage3 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack3 = FileUtils.getFile(webpackConfig).lastModified();

        Assert.assertTrue(tsPackage1 < tsPackage2);
        Assert.assertTrue(tsWebpack1 < tsWebpack2);
        Assert.assertTrue(tsPackage2 == tsPackage3);
        Assert.assertTrue(tsWebpack2 == tsWebpack3);

        assertPackageJsonContent();
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJsonObject = getPackageJson();

        JsonObject dependencies = packageJsonObject.getObject("dependencies");

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                dependencies.hasKey("@vaadin/vaadin-button"));
        Assert.assertTrue("Missing @webcomponents/webcomponentsjs package",
                dependencies.hasKey("@webcomponents/webcomponentsjs"));
        Assert.assertTrue("Missing @polymer/iron-icon package",
                dependencies.hasKey("@polymer/iron-icon"));

        JsonObject devDependencies = packageJsonObject
                .getObject("devDependencies");

        Assert.assertTrue("Missing webpack dev package",
                devDependencies.hasKey("webpack"));
        Assert.assertTrue("Missing webpack-cli dev package",
                devDependencies.hasKey("webpack-cli"));
        Assert.assertTrue("Missing webpack-dev-server dev package",
                devDependencies.hasKey("webpack-dev-server"));
        Assert.assertTrue(
                "Missing webpack-babel-multi-target-plugin dev package",
                devDependencies.hasKey("webpack-babel-multi-target-plugin"));
        Assert.assertTrue("Missing copy-webpack-plugin dev package",
                devDependencies.hasKey("copy-webpack-plugin"));
    }

    private JsonObject getPackageJson() throws IOException {
        if (packageJson.exists()) {
            return Json.parse(FileUtils.readFileToString(packageJson,
                    Charset.defaultCharset()));

        } else {
            return Json.createObject();
        }
    }
}
