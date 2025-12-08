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
package com.vaadin.base.devserver.stats;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class ProjectHelpersTest {

    private String userHome;

    @Before
    public void saveUserHome() {
        userHome = System.getProperty("user.home");
    }

    @After
    public void restoreUserHome() {
        System.setProperty("user.home", userHome);
    }

    @Test
    public void readUserKey() throws IOException {
        System.setProperty("user.home",
                TestUtils.getTestFolder("stats-data").toPath().toString());

        // Read from file
        String keyString = "user-ab641d2c-test-test-file-223cf1fa628e";
        String key = ProjectHelpers.getUserKey();
        assertEquals(keyString, key);

        // Try with non existent
        File tempDir = createTempDir();
        File vaadinHome = new File(tempDir, ".vaadin");
        vaadinHome.mkdir();

        System.setProperty("user.home", tempDir.getAbsolutePath());
        String newKey = ProjectHelpers.getUserKey();
        assertNotNull(newKey);
        assertNotSame(keyString, newKey);
        File userKeyFile = new File(vaadinHome, "userKey");
        Assert.assertTrue("userKey should be created automatically",
                userKeyFile.exists());
    }

    private File createTempDir() throws IOException {
        File tempDir = Files.createTempDirectory("test-folder").toFile();
        tempDir.deleteOnExit();
        return tempDir;
    }

    @Test
    public void writeAndReadUserKey() throws IOException {
        System.setProperty("user.home", createTempDir().getAbsolutePath());

        // Write file
        String userKey = ProjectHelpers.getUserKey();
        Assert.assertNotNull(userKey);

        // Check file
        File userFile = new File(System.getProperty("user.home"),
                ".vaadin/userKey");
        String fromFile = Files.readString(userFile.toPath(),
                StandardCharsets.UTF_8);
        Assert.assertEquals("{\"key\":\"" + userKey + "\"}", fromFile);

        Assert.assertEquals(userKey, ProjectHelpers.getUserKey());
    }

    @Test
    public void readProKey() {
        System.setProperty("user.home",
                TestUtils.getTestFolder("stats-data").toPath().toString());

        // File is used by default
        String keyStringFile = "test@vaadin.com/pro-536e1234-test-test-file-f7a1ef311234";
        String keyFile = ProjectHelpers.getProKey();
        assertEquals(keyStringFile, "test@vaadin.com/" + keyFile);

        // Check system property works
        String originalProKeyProp = System.getProperty("vaadin.proKey");
        try {
            String keyStringProp = "test@vaadin.com/pro-536e1234-test-test-prop-f7a1ef311234";
            System.setProperty("vaadin.proKey", keyStringProp);
            String keyProp = ProjectHelpers.getProKey();
            assertEquals(keyStringProp, "test@vaadin.com/" + keyProp);
        } finally {
            if (originalProKeyProp == null) {
                System.clearProperty("vaadin.proKey");
            } else {
                System.setProperty("vaadin.proKey", originalProKeyProp);
            }
        }
    }

}
