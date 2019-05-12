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
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_IMPORTS_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;

public class TaskUpdateWebpackTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdateWebpack webpackUpdater;
    private File webpackConfig;
    File baseDir;

    @Before
    public void setup() throws Exception {
        System.setProperty("user.dir", temporaryFolder.getRoot().getPath());

        baseDir = new File(getBaseDir());

        NodeUpdateTestUtil.createStubNode(true, true);

        webpackUpdater = new TaskUpdateWebpack(baseDir, new File(baseDir, "target/classes"), WEBPACK_CONFIG,
                new File(baseDir, FLOW_IMPORTS_FILE));

        webpackConfig = new File(baseDir, WEBPACK_CONFIG);
    }
    
    
    @After
    public void teardown() {
        webpackConfig.delete();
    }

    @Test
    public void should_CreateWebpackConfig() throws Exception {
        Assert.assertFalse(webpackConfig.exists());
        webpackUpdater.execute();

        Assert.assertTrue(webpackConfig.exists());
        assertWebpackConfigContent("frontend/generated-flow-imports.js", "target/classes");
    }

    @Test
    public void should_update_Webpack() throws Exception {
        webpackUpdater.execute();
        Assert.assertTrue(webpackConfig.exists());
        
        TaskUpdateWebpack newUpdater = new TaskUpdateWebpack(baseDir, new File(baseDir, "foo"), WEBPACK_CONFIG,
                new File(baseDir, "bar"));
        newUpdater.execute();
        
        assertWebpackConfigContent("bar", "foo");
    }

    private void assertWebpackConfigContent(String entryPoint, String outputFolder) throws IOException {
        
        List<String> webpackContents = Files.lines(webpackConfig.toPath()).collect(Collectors.toList());

        Assert.assertFalse(
                "webpack config should not contain Windows path separators",
                webpackContents.contains("\\\\"));

        verifyNoAbsolutePathsPresent(webpackContents);
        
        verifyUpdate(webpackContents, entryPoint, outputFolder);
    }
    
    
    private void verifyUpdate(List<String> webpackContents, String entryPoint, String outputFolder) {
        Assert.assertTrue(
                "webpack config should update fileNameOfTheFlowGeneratedMainEntryPoint",
                webpackContents.contains("fileNameOfTheFlowGeneratedMainEntryPoint = require('path').resolve(__dirname, '" + entryPoint + "');"));
        
        Assert.assertTrue(
                "webpack config should update fileNameOfTheFlowGeneratedMainEntryPoint",
                webpackContents.contains("mavenOutputFolderForFlowBundledFiles = require('path').resolve(__dirname, '" + outputFolder + "');"));
        
    }

    private void verifyNoAbsolutePathsPresent(List<String> webpackContents) {
        List<String> wrongLines = webpackContents.stream()
            // check the lines with slashes only
            .filter(line -> line.contains("/"))
            // trim the whitespaces
            .map(line -> line.replaceAll("\\s", ""))
            // check the equals ( a=something ) and object declarations ( {a: something} )
            .map(line -> {
                    int equalsSignPosition = line.indexOf("=");
                    int jsonPropertySignPosition = line.indexOf(":");
                    if (equalsSignPosition > 0) {
                        return line.substring(equalsSignPosition + 1);
                    } else if (jsonPropertySignPosition > 0) {
                        return line.substring(jsonPropertySignPosition + 1);
                    } else {
                        return null;
                    }
                })
            .filter(Objects::nonNull)
            // take the lines with strings only and trim the string start
            .filter(line -> line.startsWith("'") || line.startsWith("\"") || line.startsWith("`"))
            .map(line -> line.substring(1))
            .filter(line -> line.startsWith("/"))
            .collect(Collectors.toList());

        Assert.assertTrue(String.format(
                "Expected to have no lines that have a string starting with a slash in assignment. Incorrect lines: '%s'",
                wrongLines),
                wrongLines.isEmpty());
    }
}
