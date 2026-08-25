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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the directory deletion helpers of {@link FrontendUtils}. The
 * remaining {@code node_modules} specific cases live in the
 * {@code flow-build-tools} tests, as they need an npm install to set up.
 */
class FrontendUtilsDeleteTest {

    @Test
    void deleteDirectory_removesContentsRecursively(@TempDir File dir)
            throws Exception {
        File directory = new File(dir, "node_modules");
        File nested = new File(directory, "a/b");
        assertTrue(nested.mkdirs());
        assertTrue(new File(nested, "file.txt").createNewFile());

        FrontendUtils.deleteDirectory(directory);

        assertFalse(directory.exists());
    }

    @Test
    void deleteDirectory_onlyRemovesDirectories(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "file.txt");
        assertTrue(file.createNewFile());

        FrontendUtils.deleteDirectory(file);
        FrontendUtils.deleteDirectory(new File(dir, "missing"));

        assertTrue(file.exists(), "A plain file should not be removed");
    }
}
