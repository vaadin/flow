/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.internal.StringUtil;

public class DevBundleUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void compileDevBundle_uncompileDevBundle_filesHasSameHash()
            throws IOException {
        File projectBase = temporaryFolder.getRoot();
        File devFolder = new File(projectBase, "target/dev-bundle");
        devFolder.mkdirs();
        File configFolder = new File(devFolder, "config");
        configFolder.mkdir();
        File stats = new File(configFolder, "stats.json");
        File packages = new File(devFolder, "package.json");

        Files.write(stats.toPath(), Collections.singleton("{ \"stats\": 1 }"));
        Files.write(packages.toPath(),
                Collections.singleton("{ \"packages\": [] }"));

        String statsHash = StringUtil.getHash(Files.readString(stats.toPath()));
        String packagesHash = StringUtil
                .getHash(Files.readString(packages.toPath()));

        DevBundleUtils.compressBundle(projectBase, devFolder);

        Assert.assertTrue("Compressed bundle should have been created",
                new File(projectBase, "src/main/bundles/dev.bundle").exists());
        FileUtils.deleteDirectory(devFolder);

        Assert.assertFalse("Dev folder not deleted!", devFolder.exists());

        DevBundleUtils.unpackBundle(projectBase, devFolder);

        Assert.assertTrue("Dev folder not created!", devFolder.exists());
        Assert.assertTrue("Config folder not created!", configFolder.exists());
        Assert.assertTrue("stats file not created!", stats.exists());
        Assert.assertTrue("packages file not created!", packages.exists());

        Assert.assertEquals(statsHash,
                StringUtil.getHash(Files.readString(stats.toPath())));
        Assert.assertEquals(packagesHash,
                StringUtil.getHash(Files.readString(packages.toPath())));
    }
}