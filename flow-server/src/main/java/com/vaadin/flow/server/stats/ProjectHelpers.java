/*
 * Copyright 2000-2021 Vaadin Ltd.
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

package com.vaadin.flow.server.stats;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper methods for extracting and updating project statistics data.
 * <p>
 * Not intended to be used outside.
 */
class ProjectHelpers {

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
    static String generateProjectId(String projectFolder) {
        Path projectPath = Paths.get(projectFolder);
        File pomFile = projectPath.resolve("pom.xml").toFile();

        // Maven project
        if (pomFile.exists()) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory
                        .newInstance();
                dbf.setFeature(
                        "http://xml.org/sax/features/external-general-entities",
                        false);
                dbf.setFeature(
                        "http://xml.org/sax/features/external-parameter-entities",
                        false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document pom = db.parse(pomFile);
                String groupId = getFirstElementTextByName(
                        pom.getDocumentElement(), "groupId");
                String artifactId = getFirstElementTextByName(
                        pom.getDocumentElement(), "artifactId");
                return "pom" + createHash(groupId + artifactId);
            } catch (SAXException | IOException
                    | ParserConfigurationException e) {
                getLogger().debug("Failed to parse maven project id from "
                        + pomFile.getPath(), e);
            }
        }

        // Gradle project
        Path gradleFile = projectPath.resolve("settings.gradle");
        if (gradleFile.toFile().exists()) {
            try (Stream<String> stream = Files.lines(gradleFile)) {
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
                        + gradleFile.toFile().getPath(), e);
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
                return Hex.encodeHexString(digest);
            } catch (Exception e) {
                getLogger().debug("Missing hash algorithm", e);
            }
        }
        return StatisticsConstants.MISSING_DATA;
    }

    /**
     * DOM helper to find the text content of the first direct child node by
     * given name.
     *
     * @param parent
     *            Parent element to search.
     * @param nodeName
     *            Name of the node to search for.
     * @return Text content of the first mach or null if not found.
     */
    static String getFirstElementTextByName(Element parent, String nodeName) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals(nodeName)) {
                return nodeList.item(i).getTextContent();
            }
        }
        return null;
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
    static String getProjectSource(String projectFolder) {
        Path projectPath = Paths.get(projectFolder);
        File pomFile = projectPath.resolve("pom.xml").toFile();

        // Try Maven project
        try {
            if (pomFile.exists()) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory
                        .newInstance();
                dbf.setFeature(
                        "http://xml.org/sax/features/external-general-entities",
                        false);
                dbf.setFeature(
                        "http://xml.org/sax/features/external-parameter-entities",
                        false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document pom = db.parse(pomFile);
                NodeList nodeList = pom.getDocumentElement().getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    if (nodeList.item(i).getNodeType() == Node.COMMENT_NODE) {
                        String comment = nodeList.item(i).getTextContent();
                        if (comment.contains(
                                StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT)) {
                            return comment.substring(comment.indexOf(
                                    StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT)
                                    + StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT
                                            .length())
                                    .trim();
                        }
                    }
                }
            }

            // Try Gradle project
            Path gradleFile = projectPath.resolve("settings.gradle");
            if (gradleFile.toFile().exists()) {
                try (Stream<String> stream = Files.lines(gradleFile)) {
                    String projectName = stream.filter(line -> line.contains(
                            StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT))
                            .findFirst().orElse(null);
                    if (projectName != null) {
                        return projectName.substring(projectName.indexOf(
                                StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT)
                                + StatisticsConstants.VAADIN_PROJECT_SOURCE_TEXT
                                        .length())
                                .trim();
                    }
                }
            }
        } catch (Exception e) {
            getLogger().debug("Failed to parse project id from "
                    + projectPath.toAbsolutePath(), e);
        }
        return StatisticsConstants.MISSING_DATA;
    }

    /**
     * Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the
     *         folder exists.
     */
    static File resolveVaadinHomeDirectory() {
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
                FileUtils.forceMkdir(vaadinHome);
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
     * Get operating system identifier from system.
     *
     * @return os.name system property or MISSING_DATA
     */
    static String getOperatingSystem() {
        String os = System.getProperty("os.name");
        return os == null ? StatisticsConstants.MISSING_DATA : os;
    }

    /**
     * Get operating JVM version identifier from system.
     *
     * @return os.name system property or MISSING_DATA
     */
    static String getJVMVersion() {
        String os = System.getProperty("java.vm.name");
        String ver = System.getProperty("java.specification.version");

        if (os == null && ver == null) {
            return StatisticsConstants.MISSING_DATA;
        }
        os = (os == null ? StatisticsConstants.MISSING_DATA : os);
        ver = (ver == null ? StatisticsConstants.MISSING_DATA : ver);
        return os + " / " + ver;
    }

    /**
     * Get Vaadin Pro key if available in the system, or generated id.
     *
     * @return Vaadin Pro Key or null
     */
    static String getProKey() {
        // Use the local proKey if present
        ProKey proKey = ProKey.get();
        return proKey != null ? proKey.getKey() : null;
    }

    /**
     * Get generated user id.
     *
     * @return Generated user id, or null if unable to load or generate one.
     */
    static String getUserKey() {
        File userKeyFile = resolveUserKeyLocation();
        if (userKeyFile.exists()) {
            try {
                ProKey localKey = ProKey.fromFile(userKeyFile);
                if (localKey != null && localKey.getKey() != null) {
                    return localKey.getKey();
                }
            } catch (IOException e) {
                getLogger().debug("Failed to load userKey", e);
            }
        }

        try {
            // Generate a new one if missing and store it
            ProKey localKey = new ProKey(StatisticsConstants.GENERATED_USERNAME,
                    "user-" + UUID.randomUUID());
            localKey.toFile(userKeyFile);
            return localKey.getKey();
        } catch (IOException e) {
            getLogger().debug("Failed to write generated userKey", e);
        }
        return null;
    }

    private static Logger getLogger() {
        // Use the same logger that DevModeUsageStatistics uses
        return LoggerFactory.getLogger(DevModeUsageStatistics.class.getName());
    }
}
