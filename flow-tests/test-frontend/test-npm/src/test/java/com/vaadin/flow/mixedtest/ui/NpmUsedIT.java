package com.vaadin.flow.mixedtest.ui;

import java.io.File;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.FileTestUtil;

import org.junit.Test;

public class NpmUsedIT {

    @Test
    public void npmIsUsed() {
        File testPackage = new File(Constants.PACKAGE_LOCK_JSON);
        FileTestUtil.waitForFile(testPackage);
    }

}
