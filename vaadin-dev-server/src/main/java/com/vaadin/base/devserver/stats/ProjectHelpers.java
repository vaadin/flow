/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver.stats;

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vaadin.base.devserver.MavenUtils;

/**
 * Helper methods for extracting and updating project statistics data.
 *
 * @since 24.4
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
     * Uses either pom.xml or the Gradle settings script.
     *
     * @param projectFolder
     *            Project root folder. Should contain either pom.xml or
     *            settings.gradle(.kts).
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
        File gradleFile = findGradleSettingsFile(projectFolder);
        if (gradleFile != null) {
            try (Stream<String> stream = Files.lines(gradleFile.toPath())) {
                String projectName = stream
                        .filter(line -> line.contains("rootProject.name"))
                        .findFirst()
                        .orElse(StatisticsConstants.DEFAULT_PROJECT_ID);
                if (projectName.contains("=")) {
                    projectName = projectName
                            .substring(projectName.indexOf("=") + 1)
                            .replace('\'', ' ').replace('"', ' ').trim();
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
     * Looks for comment in either pom.xml or the Gradle settings script that
     * points back original source or repository of the project.
     *
     * @param projectFolder
     *            Project root folder. Should contain either pom.xml or
     *            settings.gradle(.kts).
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
        File gradleFile = findGradleSettingsFile(projectFolder);
        if (gradleFile != null) {
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

    private static File findGradleSettingsFile(File projectFolder) {
        return Stream.of("settings.gradle", "settings.gradle.kts")
                .map(name -> new File(projectFolder, name)).filter(File::isFile)
                .findFirst().orElse(null);
    }

    /**
     * Gets the build tool of the project in the folder.
     *
     * @param projectFolder
     *            Project root folder
     * @return <code>BUILD_TOOL_MAVEN</code> if the folder has a pom.xml,
     *         <code>BUILD_TOOL_GRADLE</code> if it has a Gradle build or
     *         settings script, or <code>MISSING_DATA</code> otherwise.
     */
    static String getBuildTool(File projectFolder) {
        if (new File(projectFolder, "pom.xml").isFile()) {
            return StatisticsConstants.BUILD_TOOL_MAVEN;
        }
        boolean gradle = findGradleSettingsFile(projectFolder) != null
                || Stream.of("build.gradle", "build.gradle.kts").anyMatch(
                        name -> new File(projectFolder, name).isFile());
        return gradle ? StatisticsConstants.BUILD_TOOL_GRADLE
                : StatisticsConstants.MISSING_DATA;
    }

    /**
     * Detects the AI coding agent that started this process, based on the
     * environment variables that the agents set for the commands they run. Only
     * the presence of the variables is checked, not their values.
     * <p>
     * {@code CLAUDECODE} is checked after the other agent specific variables
     * because other agents may also set it, for compatibility with tooling
     * written for Claude Code.
     *
     * @param environment
     *            the environment variables of the process
     * @return the identifier of the detected agent, <code>AI_AGENT_OTHER</code>
     *         for an agent that only sets the generic {@code AI_AGENT}
     *         variable, or an empty optional if no agent was detected
     */
    static Optional<String> getAiAgent(Map<String, String> environment) {
        Predicate<String> isSet = environment::containsKey;
        if (Stream.of("CODEX_SANDBOX", "CODEX_CI", "CODEX_THREAD_ID")
                .anyMatch(isSet)) {
            return Optional.of(StatisticsConstants.AI_AGENT_CODEX);
        }
        if (isSet.test("GEMINI_CLI")) {
            return Optional.of(StatisticsConstants.AI_AGENT_GEMINI);
        }
        if (isSet.test("CURSOR_AGENT")) {
            return Optional.of(StatisticsConstants.AI_AGENT_CURSOR);
        }
        if (isSet.test("OPENCODE")) {
            return Optional.of(StatisticsConstants.AI_AGENT_OPENCODE);
        }
        if (isSet.test("CLAUDECODE")) {
            return Optional.of(StatisticsConstants.AI_AGENT_CLAUDE);
        }
        // Generic variable set by, among others, GitHub Copilot agent terminals
        // in VS Code, OpenHands and pi. Claude Code sets it too, but is
        // detected above from CLAUDECODE
        if (isSet.test("AI_AGENT")) {
            return Optional.of(StatisticsConstants.AI_AGENT_OTHER);
        }
        return Optional.empty();
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
