package com.vaadin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LicenseCheckTest {

    private static class LicenseFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            String path = file.toString();
            if (path.endsWith(".java")) {
                return FileVisitResult.SKIP_SIBLINGS;
            }
            if (path.endsWith("licenses.xml")) {
                checkLicenses(file);
            }
            return super.visitFile(file, attrs);
        }

        private void checkLicenses(Path file) {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(
                        new ByteArrayInputStream(Files.readAllBytes(file)));
                NodeList licenses = doc.getElementsByTagName("licenses");
                for (int i = 0; i < licenses.getLength(); i++) {
                    Node license = licenses.item(i);
                    Map<String, List<String>> restrictive = new HashMap<>();
                    if (!checkLicenses(license, restrictive)) {
                        Assert.fail(getErrorMessage(file, restrictive));
                    }
                }
            } catch (ParserConfigurationException | SAXException
                    | IOException exception) {
                Assert.fail("Cannot parse license file " + file);
            }
        }

        private String getErrorMessage(Path file,
                Map<String, List<String>> restrictiveLicenses) {
            StringBuilder builder = new StringBuilder("File ");
            builder.append(file).append(
                    " contains the following dependencies with only restrictive licenses: ");
            restrictiveLicenses.forEach((dependency, licenses) -> builder
                    .append("\n dependency '").append(dependency)
                    .append("' has licenses : ").append(licenses));
            return builder.toString();
        }

        private boolean checkLicenses(Node licenses,
                Map<String, List<String>> restrictiveLicenses) {
            NodeList children = licenses.getChildNodes();
            List<String> licenseNames = new ArrayList<>();
            boolean hasLicenses = false;

            String groupId = getTagContent(licenses.getParentNode(), "groupId");
            String artifactId = getTagContent(licenses.getParentNode(),
                    "artifactId");
            String dependency = groupId + ":" + artifactId;

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if ("license".equals(child.getNodeName())) {
                    hasLicenses = true;
                    handleLicense(restrictiveLicenses, licenseNames, dependency,
                            child);
                }
            }
            return !licenseNames.isEmpty() || !hasLicenses;
        }

        private void handleLicense(
                Map<String, List<String>> restrictiveLicenses,
                List<String> licenseNames, String dependency, Node child) {
            String name = getTagContent(child, "name");
            if (isRestrictiveLicense(name)) {
                List<String> licenses = restrictiveLicenses
                        .computeIfAbsent(dependency, key -> new ArrayList<>());
                licenses.add(name);
            } else {
                licenseNames.add(name);
            }
        }

        private boolean isRestrictiveLicense(String license) {
            return license.contains("GPL") && !license.contains("CDDL + GPL");
        }

        private String getTagContent(Node node, String tag) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (tag.equals(child.getNodeName())) {
                    return child.getTextContent();
                }
            }
            assert false;
            return null;
        }

    }

    @Test
    public void checkLicenses() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("/");
        File classes = new File(resource.toURI());
        File target = classes.getParentFile();
        File project = target.getParentFile();
        File parentProject = project.getParentFile();

        Files.walkFileTree(parentProject.toPath(), new LicenseFileVisitor());
    }
}
