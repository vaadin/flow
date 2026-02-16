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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevBundleUtilsTest {
    @TempDir
    Path temporaryFolder;

    @Test
    public void compileDevBundle_uncompileDevBundle_filesHasSameHash()
            throws IOException {
        File projectBase = temporaryFolder.toFile();
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

        assertTrue(
                new File(projectBase, "src/main/bundles/dev.bundle").exists(),
                "Compressed bundle should have been created");
        FileUtils.deleteDirectory(devFolder);

        assertFalse(devFolder.exists(), "Dev folder not deleted!");

        DevBundleUtils.unpackBundle(projectBase, devFolder);

        assertTrue(devFolder.exists(), "Dev folder not created!");
        assertTrue(configFolder.exists(), "Config folder not created!");
        assertTrue(stats.exists(), "stats file not created!");
        assertTrue(packages.exists(), "packages file not created!");

        assertEquals(statsHash,
                StringUtil.getHash(Files.readString(stats.toPath())));
        assertEquals(packagesHash,
                StringUtil.getHash(Files.readString(packages.toPath())));
    }
}
