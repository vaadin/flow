/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver.stats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
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

    @Test
    public void aiAgent() {
        assertEquals(Optional.empty(),
                ProjectHelpers.getAiAgent(Map.of("PATH", "/usr/bin")));
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_CLAUDE),
                ProjectHelpers.getAiAgent(Map.of("CLAUDECODE", "1")));
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_CODEX),
                ProjectHelpers.getAiAgent(Map.of("CODEX_THREAD_ID", "t-42")));
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_GEMINI),
                ProjectHelpers.getAiAgent(Map.of("GEMINI_CLI", "1")));
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_CURSOR),
                ProjectHelpers.getAiAgent(Map.of("CURSOR_AGENT", "1")));
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_OPENCODE),
                ProjectHelpers.getAiAgent(Map.of("OPENCODE", "1")));
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_OTHER),
                ProjectHelpers.getAiAgent(Map.of("AI_AGENT", "some-agent")));
        // Another agent that also sets CLAUDECODE is reported as itself
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_CURSOR),
                ProjectHelpers.getAiAgent(
                        Map.of("CURSOR_AGENT", "1", "CLAUDECODE", "1")));
        // Claude Code also sets the generic AI_AGENT variable
        assertEquals(Optional.of(StatisticsConstants.AI_AGENT_CLAUDE),
                ProjectHelpers.getAiAgent(
                        Map.of("CLAUDECODE", "1", "AI_AGENT", "claude-code")));
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
        String fromFile = IOUtils.toString(new FileInputStream(userFile),
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
