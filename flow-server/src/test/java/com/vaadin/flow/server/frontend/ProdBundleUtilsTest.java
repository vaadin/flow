/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.nio.file.Files;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.internal.StringUtil;

public class ProdBundleUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void compressProdBundle_decompressProdBundle_filesHasSameHash()
            throws IOException {
        File projectBase = temporaryFolder.getRoot();
        File prodFolder = new File(projectBase,
                "target/classes/META-INF/VAADIN");
        prodFolder.mkdirs();
        File configFolder = new File(prodFolder, "config");
        configFolder.mkdir();
        File stats = new File(configFolder, "stats.json");
        File index = new File(prodFolder, "index.html");

        Files.write(stats.toPath(), Collections.singleton("{ \"stats\": 1 }"));
        Files.write(index.toPath(), Collections.singleton("<!DOCTYPE html>"));

        String statsHash = StringUtil.getHash(Files.readString(stats.toPath()));
        String indexHash = StringUtil.getHash(Files.readString(index.toPath()));

        ProdBundleUtils.compressBundle(projectBase, prodFolder);

        Assert.assertTrue("Compressed bundle should have been created",
                new File(projectBase, "src/main/bundles/prod.bundle").exists());
        FileUtils.deleteDirectory(prodFolder);

        Assert.assertFalse("Prod folder not deleted!", prodFolder.exists());

        ProdBundleUtils.unpackBundle(projectBase, prodFolder);

        Assert.assertTrue("Prod folder not created!", prodFolder.exists());
        Assert.assertTrue("Config folder not created!", configFolder.exists());
        Assert.assertTrue("stats file not created!", stats.exists());
        Assert.assertTrue("packages file not created!", index.exists());

        Assert.assertEquals(statsHash,
                StringUtil.getHash(Files.readString(stats.toPath())));
        Assert.assertEquals(indexHash,
                StringUtil.getHash(Files.readString(index.toPath())));
    }
}
