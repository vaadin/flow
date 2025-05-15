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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractNodeUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File importsFile;
    private TaskUpdateImports updater;

    private MockLogger logger;

    @Before
    public void setup() throws Exception {
        File tmpRoot = temporaryFolder.getRoot();

        logger = new MockLogger();

        File frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        File nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        importsFile = FrontendUtils.getFlowGeneratedImports(frontendDirectory);

        ClassFinder classFinder = getClassFinder();
        Options options = new MockOptions(classFinder, tmpRoot)
                .withFrontendDirectory(frontendDirectory)
                .withBuildDirectory(TARGET).withProductionMode(true)
                .withBundleBuild(true);
        updater = new TaskUpdateImports(getScanner(classFinder), options) {
            @Override
            Logger log() {
                return logger;
            }

        };

        assertTrue(nodeModulesPath.mkdirs());
        createExpectedImports(frontendDirectory, nodeModulesPath);
        assertTrue(
                new File(
                        new File(
                                new File(frontendDirectory,
                                        FrontendUtils.GENERATED),
                                FrontendUtils.JAR_RESOURCES_FOLDER),
                        "ExampleConnector.js").exists());
    }

    protected abstract FrontendDependenciesScanner getScanner(
            ClassFinder finder);

    @Test
    public void noChanges_generatedJsFileIsNotUpdated() throws Exception {
        updater.execute();
        long timestamp1 = importsFile.lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        updater.execute();
        long timestamp2 = importsFile.lastModified();

        Assert.assertEquals(timestamp1, timestamp2);
    }

    private void assertContains(String content, boolean contains,
            String... checks) {
        for (String importString : checks) {
            boolean result = content.contains(importString);
            String message = "\n  " + (contains ? "NOT " : "") + "FOUND '"
                    + importString + " IN: \n" + content;
            if (contains) {
                assertTrue(message, result);
            } else {
                assertFalse(message, result);
            }
        }
    }
}
