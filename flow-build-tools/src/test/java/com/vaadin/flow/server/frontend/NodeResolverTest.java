/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeResolver.ActiveNodeInstallation;
import com.vaadin.flow.testutil.FrontendStubs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Covers how {@link NodeResolver} treats an installation that is already in
 * {@code ~/.vaadin}. Installing a new version needs a real download and is
 * covered by {@code FrontendToolsTest} instead.
 */
class NodeResolverTest {

    private static final String VERSION = "v24.19.0";

    @TempDir
    File vaadinHome;

    @BeforeEach
    void requireShellStubs() {
        assumeFalse(FrontendUtils.isWindows(),
                "The node stub is a shell script, so it cannot be executed on Windows");
    }

    @Test
    void resolve_existingInstallation_isUsedWithoutInstalling()
            throws IOException {
        NodeInstallation installation = stubInstallation(VERSION);

        ActiveNodeInstallation active = resolve(VERSION);

        assertEquals(installation.getNodeExecutable().getAbsolutePath(),
                active.nodeExecutable());
        assertEquals(installation.getNpmCliScript().getAbsolutePath(),
                active.npmCliScript());
        assertEquals("24.19.0", active.nodeVersion());
    }

    private ActiveNodeInstallation resolve(String nodeVersion) {
        return new NodeResolver(vaadinHome.getAbsolutePath(), nodeVersion,
                vaadinHome.toURI(), true, List.of(), null).resolve();
    }

    private NodeInstallation stubInstallation(String version)
            throws IOException {
        return new NodeInstallation(FrontendStubs.createStubVersionedNode(
                version, vaadinHome.getAbsolutePath()));
    }
}
