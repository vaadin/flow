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
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.vaadin.open.OSUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileIOUtilsTest {

    @Test
    public void projectFolderOnWindows() throws Exception {
        assumeTrue(OSUtils.isWindows());

        URL url = new URL(
                "file:/C:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        assertEquals(
                new File("C:\\Users\\John Doe\\Downloads\\my-app (21)\\my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    public void projectFolderOnMacOrLinux() throws Exception {
        assumeFalse(OSUtils.isWindows());

        URL url = new URL(
                "file:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        assertEquals(new File("/Users/John Doe/Downloads/my-app (21)/my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    public void tempFilesAreTempFiles() {
        assertTrue(FileIOUtils.isProbablyTemporaryFile(new File("foo.txt~")));
        assertFalse(FileIOUtils.isProbablyTemporaryFile(new File("foo.txt")));
    }
}
