package com.vaadin.flow.mixedtest.ui;

import java.io.File;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.FileTestUtil;

import org.junit.Test;

public class BunUsedIT {

    @Test
    public void bunIsUsed() {
        File testPackage = new File(Constants.PACKAGE_LOCK_BUN);
        FileTestUtil.waitForFile(testPackage);
    }

}
