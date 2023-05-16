package com.vaadin.flow.mixedtest.ui;

import java.io.File;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.FileTestUtil;

import org.junit.Test;

public class PnpmUsedIT {

    @Test
    public void pnpmIsUsed() {
        File testPackage = new File(Constants.PACKAGE_LOCK_YAML);
        FileTestUtil.waitForFile(testPackage);
    }

}
