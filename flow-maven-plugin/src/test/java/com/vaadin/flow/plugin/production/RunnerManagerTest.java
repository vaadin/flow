/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.plugin.production;

import java.io.File;
import java.util.Collections;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.common.RunnerManager;

import static org.junit.Assert.assertTrue;

public class RunnerManagerTest {

    private final ProxyConfig proxyConfig = new ProxyConfig(
            Collections.emptyList());
    private final String nodeVersion = "v8.11.1";
    private final String yarnVersion = "v1.6.0";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder donwloadDirectory = new TemporaryFolder();

    @Test
    public void getLocalRunnerManagerWithoutInstalling()
            throws TaskRunnerException {
        File nonExistingFile = new File("desNotExist");
        new RunnerManager(donwloadDirectory.getRoot(), proxyConfig,
                nonExistingFile, nonExistingFile);

        assertTrue("In the local mode, no file is downloaded!",
                donwloadDirectory.getRoot().list().length == 0);
    }

    @Test
    public void getRunnerManagerInstalling() {
        try {
            RunnerManager runnerManager = new RunnerManager(
                    donwloadDirectory.getRoot(), proxyConfig, nodeVersion,
                    yarnVersion);
            // in the case that node and yarn are installed correctly, the
            // download directory should not be empty
            assertTrue(
                    "In the installation mode, node and yarn(files) should be downloaded!",
                    donwloadDirectory.getRoot().list().length > 0);
        } catch (IllegalStateException e) {
            // skip: it was not possible to install node and yarn.
        }

    }
}
