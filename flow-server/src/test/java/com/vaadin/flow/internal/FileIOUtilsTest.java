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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class FileIOUtilsTest {

    // TEMP: dropped projectFolderOnWindows to exercise the test-removed
    // summary line. Restore before merging.

    @Test
    void projectFolderOnMacOrLinux() throws Exception {
        assumeFalse(OSUtils.isWindows());

        // TEMP: long sleep to push total run time above the 110% reference
        // threshold so the time-budget warning fires. Remove before merging.
        Thread.sleep(17 * 60 * 1000L);

        URL url = new URL(
                "file:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        assertEquals(new File("/Users/John Doe/Downloads/my-app (21)/my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    void tempFilesAreTempFiles() {
        // TEMP: assertion intentionally flipped to produce one failure for the
        // wrap-comment demo. Revert before merging.
        assertFalse(FileIOUtils.isProbablyTemporaryFile(new File("foo.txt~")));
        assertFalse(FileIOUtils.isProbablyTemporaryFile(new File("foo.txt")));
    }
}
