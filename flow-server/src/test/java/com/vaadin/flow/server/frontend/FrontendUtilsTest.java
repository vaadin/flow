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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubNode;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FrontendUtilsTest {

    public static final String DEFAULT_NODE = FrontendUtils.isWindows() ?
            "node\\node.exe" :
            "node/node";

    public static final String NPM_CLI_STRING = Stream
            .of("node", "node_modules", "npm", "bin", "npm-cli.js")
            .collect(Collectors.joining(File.separator));
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
        createStubNode(true, true);

        assertThat(FrontendUtils.getNodeExecutable(),
                containsString(DEFAULT_NODE));
        assertThat(FrontendUtils.getNpmExecutable().get(0),
                containsString(DEFAULT_NODE));
        assertThat(FrontendUtils.getNpmExecutable().get(1),
                containsString(NPM_CLI_STRING));
    }

    @Test
    public void should_useProjectNpmFirst() throws Exception {
        createStubNode(false, true);

        assertThat(FrontendUtils.getNodeExecutable(), containsString("node"));
        assertThat(FrontendUtils.getNodeExecutable(),
                not(containsString(DEFAULT_NODE)));
        assertThat(FrontendUtils.getNpmExecutable().get(0),
                containsString("node"));
        assertThat(FrontendUtils.getNpmExecutable().get(1),
                containsString(NPM_CLI_STRING));
    }

    @Test
    public void should_useSystemNode() {
        assertThat(FrontendUtils.getNodeExecutable(), containsString("node"));
        assertThat(FrontendUtils.getNodeExecutable(),
                not(containsString(DEFAULT_NODE)));
        assertThat(FrontendUtils.getNpmExecutable().get(0),
                containsString("npm"));
        assertThat(FrontendUtils.getNodeExecutable(),
                not(containsString(NPM_CLI_STRING)));
        assertEquals(1, FrontendUtils.getNpmExecutable().size());
    }
}
