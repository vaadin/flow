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
        File testPackage = new File("node_modules/lit");
        FileTestUtil.waitForFile(testPackage);
        FileTestUtil.assertSymlink(testPackage,
                "pnpm should have been used to install frontend dependencies but "
                        + testPackage.getAbsolutePath()
                        + " is a not a symlink like it should be");
    }

}
