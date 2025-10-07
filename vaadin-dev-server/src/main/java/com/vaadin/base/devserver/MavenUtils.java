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
package com.vaadin.base.devserver;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilities for Maven based projects.
 */
public class MavenUtils {

    private MavenUtils() {
        // Utils only
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(MavenUtils.class);
    }

    /**
     * Parses the pom file in the given folder.
     *
     * @param projectFolder
     *            the project folder
     * @return a parsed pom.xml if pom.xml exists or {@code null} if no pom.xml
     *         was found or it could not be parsed
     */
    public static Document parsePomFileFromFolder(File projectFolder) {
        return parsePomFile(new File(projectFolder, "pom.xml"));
    }

    /**
     * Parses the given pom file.
     *
     * @param pomFile
     *            the pom file
     * @return a parsed pom.xml if the pom.xml file exists or {@code null} if no
     *         pom.xml was found or it could not be parsed
     */
    public static Document parsePomFile(File pomFile) {
        if (!pomFile.exists()) {
            return null;
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);
            dbf.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(pomFile);
        } catch (Exception e) {
            getLogger().error("Unable to parse pom.xml in " + pomFile, e);
        }

        return null;
    }

    /**
     * Finds the text content of the first direct child node by given name.
     *
     * @param parent
     *            Parent element to search.
     * @param nodeName
     *            Name of the node to search for.
     * @return Text content of the first mach or null if not found.
     */
    static String getFirstElementTextByName(Node parent, String nodeName) {
        return findChild(parent, nodeName).map(Node::getTextContent)
                .orElse(null);
    }

    /**
     * Finds the group id for the given pom file.
     *
     * @param pom
     *            the parsed pom.xml
     * @return the groupId from the pom file
     */
    public static String getGroupId(Document pom) {
        String groupId = getFirstElementTextByName(pom.getDocumentElement(),
                "groupId");
        if (groupId == null) {
            groupId = findParentTag(pom)
                    .map(parentNode -> getFirstElementTextByName(parentNode,
                            "groupId"))
                    .orElse(null);
        }
        return groupId;
    }

    private static Optional<Node> findParentTag(Document pom) {
        return findChild(pom.getDocumentElement(), "parent");
    }

    /**
     * Finds the artifact id for the given pom file.
     *
     * @param pom
     *            the parsed pom.xml
     * @return the artifactId from the pom file
     */
    public static String getArtifactId(Document pom) {
        return getFirstElementTextByName(pom.getDocumentElement(),
                "artifactId");
    }

    private static Optional<String> getParentArtifactId(Document pom) {
        return findParentTag(pom)
                .flatMap(parentNode -> findChild(parentNode, "artifactId"))
                .map(Node::getTextContent);
    }

    private static Optional<String> getParentRelativePath(Document pom) {
        return findParentTag(pom)
                .flatMap(parentNode -> findChild(parentNode, "relativePath"))
                .map(Node::getTextContent);
    }

    private static Optional<Node> findChild(Node node, String tagname) {
        return findChildren(node, tagname).findFirst();
    }

    private static Stream<Node> findChildren(Node node, String tagname) {
        Builder<Node> builder = Stream.builder();
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if (childNode.getNodeName().equals(tagname)) {
                builder.add(childNode);
            }
        }

        return builder.build();
    }

    /**
     * Gets the parent pom location for the given pom file, if the given pom
     * file is part of a multi module project.
     *
     * @param pomFile
     *            the pom file
     * @return the location of the parent pom file or {@code null} if the given
     *         pom file does not have a parent inside the same multi module
     *         project
     */
    public static File getParentPomOfMultiModuleProject(File pomFile) {
        Document pom = parsePomFile(pomFile);
        if (pom == null) {
            return null;
        }
        Optional<String> parent = getParentArtifactId(pom);
        if (!parent.isPresent()) {
            return null;
        }

        File pomFolder = pomFile.getParentFile();
        File parentPomFile = getParentRelativePath(pom)
                .map(relativePath -> new File(pomFolder, relativePath))
                .map(relativePath -> {
                    if (!relativePath.isFile()) {
                        // relative path can refer to a folder
                        relativePath = new File(relativePath, "pom.xml");
                    }
                    return relativePath;
                }).orElse(new File(pomFolder.getParentFile(), "pom.xml"));

        Document parentFolderPom = parsePomFile(parentPomFile);
        if (parentFolderPom == null) {
            return null;
        }
        String parentFolderArtifactId = getArtifactId(parentFolderPom);

        if (Objects.equals(parent.get(), parentFolderArtifactId)) {
            try {
                return parentPomFile.getCanonicalFile();
            } catch (IOException e) {
                return parentPomFile;
            }
        }
        return null;

    }

    /**
     * Gets a list of the folders containing the sub modules for the given pom
     * file.
     *
     * @param pom
     *            the pom file containing sub modules
     * @return a list of folders for the sub modules
     */
    public static List<String> getModuleFolders(Document pom) {
        return findChild(pom.getDocumentElement(), "modules").stream()
                .flatMap(node -> findChildren(node, "module"))
                .map(Node::getTextContent)
                .map(possiblyFilename -> removeAfter(possiblyFilename, "/"))
                .toList();
    }

    /**
     * Removes the part of the given string that comes after the (last) instance
     * of the given delimiter.
     *
     * Returns the original string if it does not contain the delimiter.
     *
     * @param str
     *            the string to parse
     * @param delimiter
     *            the delimiter to look for
     * @return the modified string
     */
    private static String removeAfter(String str, String delimiter) {
        int i = str.lastIndexOf(delimiter);
        if (i != -1) {
            return str.substring(0, i);
        }
        return str;
    }

}
