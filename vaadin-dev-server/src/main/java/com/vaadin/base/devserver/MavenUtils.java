package com.vaadin.base.devserver;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
        return findChild(parent, nodeName).map(node -> node.getTextContent())
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
            groupId = findChild(pom.getDocumentElement(), "parent")
                    .map(parentNode -> getFirstElementTextByName(parentNode,
                            "groupId"))
                    .orElse(null);
        }
        return groupId;
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

}
