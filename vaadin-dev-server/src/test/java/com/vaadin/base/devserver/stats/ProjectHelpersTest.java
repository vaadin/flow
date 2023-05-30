/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.vaadin.flow.testutil.TestUtils;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProjectHelpersTest {

    private static final String USER_HOME = "user.home";

    private String userHome;

    @Before
    public void setup() {
        userHome = System.getProperty(USER_HOME);
    }

    @After
    public void teardown() {
        if (userHome == null) {
            System.clearProperty(USER_HOME);
        } else {
            System.setProperty(USER_HOME, userHome);
        }
    }

    @Test
    public void readUserKey() throws IOException {
        System.setProperty(USER_HOME,
                TestUtils.getTestFolder("stats-data").toPath().toString());

        // Read from file
        String keyString = "user-ab641d2c-test-test-file-223cf1fa628e";
        String key = ProjectHelpers.getUserKey();
        assertEquals(keyString, key);

        // Try with non existent
        File tempDir = createTempDir();
        File vaadinHome = new File(tempDir, ".vaadin");
        vaadinHome.mkdir();

        System.setProperty(USER_HOME, tempDir.getAbsolutePath());
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
        System.setProperty(USER_HOME, createTempDir().getAbsolutePath());

        // Write file
        String userKey = ProjectHelpers.getUserKey();
        Assert.assertNotNull(userKey);

        // Check file
        File userFile = new File(System.getProperty(USER_HOME),
                ".vaadin/userKey");
        String fromFile = IOUtils.toString(new FileInputStream(userFile),
                StandardCharsets.UTF_8);
        Assert.assertEquals("{\"key\":\"" + userKey + "\"}", fromFile);

        Assert.assertEquals(userKey, ProjectHelpers.getUserKey());
    }

    @Test
    public void readProKey() {
        System.setProperty(USER_HOME,
                TestUtils.getTestFolder("stats-data").toPath().toString());

        // File is used by default
        String keyStringFile = "test@vaadin.com/pro-536e1234-test-test-file-f7a1ef311234";
        String keyFile = ProjectHelpers.getProKey();
        assertEquals(keyStringFile, "test@vaadin.com/" + keyFile);

        // Check system property works
        String keyStringProp = "test@vaadin.com/pro-536e1234-test-test-prop-f7a1ef311234";
        System.setProperty("vaadin.proKey", keyStringProp);
        String keyProp = ProjectHelpers.getProKey();
        assertEquals(keyStringProp, "test@vaadin.com/" + keyProp);
    }

}
