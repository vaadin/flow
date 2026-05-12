/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import java.io.File;

import com.vaadin.flow.testutil.FileTestUtil;

import org.junit.Test;

public class PnpmUsedIT {

    @Test
    public void pnpmIsUsed() {
        // The previous check asserted that node_modules/lit is a symlink
        // (pnpm isolated-mode signature). Since FrontendTools now forces
        // --config.node-linker=hoisted to make transitive deps reachable
        // at the project root, pnpm produces a flat layout and that
        // symlink no longer exists. Presence of pnpm-lock.yaml is the
        // remaining reliable signal that pnpm (not npm) ran the install.
        File testPackage = new File("pnpm-lock.yaml");
        FileTestUtil.waitForFile(testPackage);
    }

}
