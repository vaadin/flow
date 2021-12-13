package com.vaadin.flow.mixedtest.ui;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class PnpmUsedIT {

    @Test
    public void pnpmIsUsed() {
        assertProjectFileExists("pnpm-lock.yaml",
                "npm should have been used to install frontend dependencies but no package-lock.json was found");
        assertProjectFileNotExists("package-lock.json",
                "npm should have been used to install frontend dependencies but a pnpm-lock.yaml was found");
    }

    private void assertProjectFileExists(String file, String errorMessage) {
        Assert.assertTrue(errorMessage, new File(file).exists());
    }

    private void assertProjectFileNotExists(String file, String errorMessage) {
        Assert.assertFalse(errorMessage, new File(file).exists());
    }
}
