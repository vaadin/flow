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
package com.vaadin.flow.internal;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.vaadin.open.OSUtils;

public class FileIOUtilsTest {

    @Test
    public void projectFolderOnWindows() throws Exception {
        Assume.assumeTrue(OSUtils.isWindows());

        URL url = new URL(
                "file:/C:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        Assert.assertEquals(
                new File("C:\\Users\\John Doe\\Downloads\\my-app (21)\\my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    public void projectFolderOnMacOrLinux() throws Exception {
        Assume.assumeFalse(OSUtils.isWindows());

        URL url = new URL(
                "file:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        Assert.assertEquals(
                new File("/Users/John Doe/Downloads/my-app (21)/my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    public void tempFilesAreTempFiles() {
        Assert.assertTrue(
                FileIOUtils.isProbablyTemporaryFile(new File("foo.txt~")));
        Assert.assertFalse(
                FileIOUtils.isProbablyTemporaryFile(new File("foo.txt")));
    }
}
