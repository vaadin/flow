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

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vaadin.base.devserver.MavenUtils;

/**
 * Helper methods for extracting and updating project statistics data.
 */
public class ProjectHelpers {

    /*
     * Avoid instantiation.
     */
    private ProjectHelpers() {
        // Utility class only
    }

    /**
     * Generates a unique pseudonymised hash string for the project in folder.
     * Uses either pom.xml or settings.gradle.
     *
     * @param projectFolder
     *            Project root folder. Should contain either pom.xml or
     *            settings.gradle.
     * @return Pseudonymised hash id of project or
     *         <code>DEFAULT_PROJECT_ID</code> if no valid project was found in
     *         the folder.
     */
    static String generateProjectId(File projectFolder) {
        Document pom = MavenUtils.parsePomFileFromFolder(projectFolder);
        if (pom != null) {
            // Maven project

            String groupId = MavenUtils.getGroupId(pom);
            String artifactId = MavenUtils.getArtifactId(pom);
            return "pom" + createHash(groupId + artifactId);
        }

        // Gradle project
        File gradleFile = new File(projectFolder, "settings.gradle");
        if (gradleFile.exists()) {
            try (Stream<String> stream = Files.lines(gradleFile.toPath())) {
                String projectName = stream
                        .filter(line -> line.contains("rootProject.name"))
                        .findFirst()
                        .orElse(StatisticsConstants.DEFAULT_PROJECT_ID);
                if (projectName.contains("=")) {
                    projectName = projectName
                            .substring(projectName.indexOf("=") + 1)
                            .replace('\'', ' ').trim();
                }
                return "gradle" + createHash(projectName);
            } catch (IOException e) {
                getLogger().debug("Failed to parse gradle project id from "
                        + gradleFile.getPath(), e);
            }
        }
        return createHash(StatisticsConstants.DEFAULT_PROJECT_ID);
    }

    /**
     * Creates a MD5 hash out from a string for pseudonymisation purposes.
     *
     * @param string
     *            String to hash
     * @return Hex encoded MD5 version of string or <code>MISSING_DATA</code>.
     */
    static String createHash(String string) {
        if (string != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(string.getBytes(StandardCharsets.UTF_8));
                byte[] digest = md.digest();
                return toHexString(digest);
            } catch (Exception e) {
                getLogger().debug("Missing hash algorithm", e);
            }
        }
        return StatisticsConstants.MISSING_DATA;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Get the source URL for the project.
     * <p>
     * Looks for comment in either pom.xml or or settings.gradle that points
     * back original source or repository of the project.
     *
     * @param projectFolder
     *            Project root folder. Should contain either pom.xml or
     *            settings.gradle.
     * @return URL of the project source or <code>MISSING_DATA</code>, if no
     *         valid URL was found.
     */
    static String getProjectSource(File projectFolder) {
        try {
            String projectSource = getMavenProjectSource(projectFolder);
            if (projectSource != null) {
                return projectSource;
            }
            projectSource = getGradleProjectSource(projectFolder);
            if (projectSource != null) {
                return projectSource;
            }
        } catch (Exception e) {
            getLogger().debug("Failed to parse project id from "
                    + projectFolder.toPath().toAbsolutePath(), e);
        }
        return StatisticsConstants.MISSING_DATA;
    }

    private static String getMavenProjectSource(File projectFolder)
            throws ParserConfigurationException, SAXException, IOException {
        Document pom = MavenUtils.parsePomFileFromFolder(projectFolder);
        if (pom == null) {
            return null;
        }
        NodeList nodeList = pom.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.COMMENT_NODE) {
                String comment = nodeList.item(i).getTextContent();
                String projectSource = findProjectSource(comment);
                if (projectSource != null) {
                    return projectSource;
                }
            }
        }

        return null;
    }

    private static String findProjectSource(String comment) {
        if (comment == null) {
            return null;
        }
        if (comment.contains(StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT)) {
            return comment.substring(comment
                    .indexOf(StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT)
                    + StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT.length())
                    .trim();
        } else if (comment.contains(StatisticsConstants.PROJECT_SOURCE_TEXT)) {
            return comment
                    .substring(comment
                            .indexOf(StatisticsConstants.PROJECT_SOURCE_TEXT)
                            + StatisticsConstants.PROJECT_SOURCE_TEXT.length())
                    .trim();
        }

        return null;
    }

    private static String getGradleProjectSource(File projectFolder)
            throws IOException {
        File gradleFile = new File(projectFolder, "settings.gradle");
        if (gradleFile.exists()) {
            try (Stream<String> stream = Files.lines(gradleFile.toPath())) {
                String comment = stream.filter(line -> line.contains(
                        StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT)
                        || line.contains(
                                StatisticsConstants.PROJECT_SOURCE_TEXT))
                        .findFirst().orElse(null);
                String projectSource = findProjectSource(comment);
                if (projectSource != null) {
                    return projectSource;
                }
            }
        }
        return null;

    }

    /**
     * Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the
     *         folder exists.
     */
    public static File resolveVaadinHomeDirectory() {
        String userHome = System
                .getProperty(StatisticsConstants.PROPERTY_USER_HOME);
        return new File(userHome, StatisticsConstants.VAADIN_FOLDER_NAME);
    }

    /**
     * Get usage statistics json file location.
     *
     * @return the location of statistics storage file.
     */
    static File resolveStatisticsStore() {

        File vaadinHome;
        try {
            vaadinHome = ProjectHelpers.resolveVaadinHomeDirectory();
        } catch (Exception e) {
            getLogger().debug("Failed to find .vaadin directory ", e);
            vaadinHome = null;
        }

        if (vaadinHome == null) {
            try {
                // Create a temp folder for data
                vaadinHome = File.createTempFile(
                        StatisticsConstants.VAADIN_FOLDER_NAME,
                        UUID.randomUUID().toString());
                Files.createDirectories(vaadinHome.toPath());
            } catch (IOException e) {
                getLogger().debug("Failed to create temp directory ", e);
                return null;
            }
        }
        return new File(vaadinHome, StatisticsConstants.STATISTICS_FILE_NAME);
    }

    /**
     * Get location for user key file.
     *
     * @return File containing the generated user id.
     */
    static File resolveUserKeyLocation() {
        File vaadinHome = resolveVaadinHomeDirectory();
        return new File(vaadinHome, StatisticsConstants.USER_KEY_FILE_NAME);
    }

    /**
     * Get Vaadin Pro key if available in the system.
     *
     * @return Vaadin Pro Key or null
     */
    static String getProKey() {
        // Use the local proKey if present
        ProKey proKey = ProKey.get();
        return proKey != null ? proKey.getKey() : null;
    }

    /**
     * Gets the generated user id.
     * <p>
     * Generates one if it does not exist.
     *
     * @return Generated user id, or null if unable to load or generate one.
     */
    public static String getUserKey() {
        File userKeyFile = resolveUserKeyLocation();
        UserKey localKey = new UserKey(userKeyFile);
        if (localKey.getKey() != null) {
            return localKey.getKey();
        }

        // Generate a new one if missing and store it
        localKey = new UserKey("user-" + UUID.randomUUID());
        try {
            localKey.toFile(userKeyFile);
            return localKey.getKey();
        } catch (IOException e) {
            getLogger().debug("Failed to write generated userKey", e);
        }

        // No point in returning a key if we haven't stored it
        return null;
    }

    private static Logger getLogger() {
        // Use the same logger that DevModeUsageStatistics uses
        return LoggerFactory.getLogger(DevModeUsageStatistics.class.getName());
    }
}
