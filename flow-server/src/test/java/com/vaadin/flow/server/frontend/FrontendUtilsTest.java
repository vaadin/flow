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
package com.vaadin.flow.server.frontend;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FrontendUtilsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        System.setProperty("user.dir", tmpDir.getRoot().getAbsolutePath());
    }

    @Test
    public void should_useProjectNodeFirst() throws Exception {
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
        FileUtils.write(node, "#!/bin/sh\necho 8.0.0\n", "UTF-8");

        assertThat(FrontendUtils.getNodeExecutable(), Matchers.containsString("node/node"));
        assertThat(FrontendUtils.getNpmExecutable().get(0), Matchers.containsString("node/node"));
        assertThat(FrontendUtils.getNpmExecutable().get(1), Matchers.containsString("node/node_modules/npm/bin/npm-cli.js"));
    }

    @Test
    public void should_useProjectNpmFirst() throws Exception {
        File npmCli = new File(tmpDir.getRoot(), "node/node_modules/npm/bin/npm-cli.js");
        FileUtils.forceMkdirParent(npmCli);
        npmCli.createNewFile();

        assertThat(FrontendUtils.getNodeExecutable(), Matchers.containsString("node"));
        assertThat(FrontendUtils.getNpmExecutable().get(0), Matchers.containsString("node"));
        assertThat(FrontendUtils.getNpmExecutable().get(1), Matchers.containsString("node/node_modules/npm/bin/npm-cli.js"));
    }

    @Test
    public void should_useSystemNode() throws Exception {
        assertThat(FrontendUtils.getNodeExecutable(), Matchers.containsString("node"));
        assertThat(FrontendUtils.getNpmExecutable().get(0), Matchers.containsString("npm"));
        assertEquals(1 , FrontendUtils.getNpmExecutable().size());
    }
}
