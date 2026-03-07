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
import java.nio.file.Files;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.StringUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdBundleUtilsTest {

    @TempDir
    File temporaryFolder;

    @Test
    void compressProdBundle_decompressProdBundle_filesHasSameHash()
            throws IOException {
        File projectBase = temporaryFolder;
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

        assertTrue(
                new File(projectBase, "src/main/bundles/prod.bundle").exists(),
                "Compressed bundle should have been created");
        FileUtils.deleteDirectory(prodFolder);

        assertFalse(prodFolder.exists(), "Prod folder not deleted!");

        ProdBundleUtils.unpackBundle(projectBase, prodFolder);

        assertTrue(prodFolder.exists(), "Prod folder not created!");
        assertTrue(configFolder.exists(), "Config folder not created!");
        assertTrue(stats.exists(), "stats file not created!");
        assertTrue(index.exists(), "packages file not created!");

        assertEquals(statsHash,
                StringUtil.getHash(Files.readString(stats.toPath())));
        assertEquals(indexHash,
                StringUtil.getHash(Files.readString(index.toPath())));
    }
}
