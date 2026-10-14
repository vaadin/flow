/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AbstractConfigurationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    AbstractConfiguration configuration = new AbstractConfiguration() {
        @Override
        public boolean isProductionMode() {
            return false;
        }

        @Override
        public String getStringProperty(String name, String defaultValue) {
            return null;
        }

        @Override
        public boolean getBooleanProperty(String name, boolean defaultValue) {
            return false;
        }
    };

    @Test
    public void getProjectFolder_mavenProject_detected() throws IOException {
        assertProjectFolderDetected("pom.xml");
    }

    @Test
    public void getProjectFolder_gradleProject_detected() throws IOException {
        assertProjectFolderDetected("build.gradle");
    }

    @Test
    public void getProjectFolder_gradleKotlinProject_detected()
            throws IOException {
        assertProjectFolderDetected("build.gradle.kts");
    }

    @Test
    public void getProjectFolder_unknownProject_throws() throws IOException {
        withTemporaryUserDir(() -> {
            IllegalStateException exception = Assert.assertThrows(
                    IllegalStateException.class,
                    configuration::getProjectFolder);
            Assert.assertTrue(exception.getMessage().contains(
                    "Failed to determine project directory for dev mode"));
            Assert.assertTrue(exception.getMessage()
                    .contains(temporaryFolder.getRoot().getAbsolutePath()));
        });
    }

    private void assertProjectFolderDetected(String projectFile)
            throws IOException {
        temporaryFolder.newFile(projectFile);
        withTemporaryUserDir(() -> {
            File projectFolder = configuration.getProjectFolder();
            Assert.assertEquals(temporaryFolder.getRoot(), projectFolder);
        });
    }

    private void withTemporaryUserDir(Runnable test) throws IOException {
        String userDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir",
                    temporaryFolder.getRoot().getAbsolutePath());
            test.run();
        } finally {
            if (userDir != null) {
                System.setProperty("user.dir", userDir);
            } else {
                System.clearProperty("user.dir");
            }
        }
    }

}
