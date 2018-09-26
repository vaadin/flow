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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.common.RunnerManager;

import static org.junit.Assert.assertEquals;

public class RunnerManagerTest {

    private final ProxyConfig proxyConfig = new ProxyConfig(
            Collections.emptyList());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder downloadDirectory = new TemporaryFolder();

    @Test
    public void getLocalRunnerManagerWithoutInstalling() {
        File nonExistingFile = new File("desNotExist");
        new RunnerManager(downloadDirectory.getRoot(), proxyConfig,
                nonExistingFile, nonExistingFile);

        assertEquals("In the local mode, no file should be downloaded", 0,
                downloadDirectory.getRoot().list().length);
    }
}
