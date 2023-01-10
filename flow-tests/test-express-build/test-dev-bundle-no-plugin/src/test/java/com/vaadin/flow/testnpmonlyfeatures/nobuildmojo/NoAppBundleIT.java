package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NoAppBundleIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Override
    protected String getTestPath() {
        return "/run/com.vaadin.flow.testnpmonlyfeatures.nobuildmojo.MultipleNpmPackageAnnotationsView";
    }

    @Test
    public void noFrontendFilesCreated() {
        File baseDir = new File(System.getProperty("user.dir", "."));

        // shouldn't create a dev-bundle
        Assert.assertFalse("No dev-bundle should be created",
                new File(baseDir, "dev-bundle").exists());
        Assert.assertFalse("No node_modules should be created",
                new File(baseDir, "node_modules").exists());

        // These should not be generated either, but at the moment they are
        Assert.assertFalse("No package.json should be created",
                new File(baseDir, "package.json").exists());
        Assert.assertFalse("No vite generated should be created",
                new File(baseDir, "vite.generated.ts").exists());
        Assert.assertFalse("No vite config should be created",
                new File(baseDir, "vite.config.ts").exists());
        Assert.assertFalse("No types should be created",
                new File(baseDir, "types.d.ts").exists());
        Assert.assertFalse("No tsconfig should be created",
                new File(baseDir, "tsconfig.json").exists());
    }
}
