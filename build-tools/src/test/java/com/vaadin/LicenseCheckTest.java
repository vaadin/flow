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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final Set<String> whitelist = new HashSet<>();
    static {
        whitelist.add("http://www.apache.org/licenses/LICENSE-2.0");
        whitelist.add("https://www.apache.org/licenses/LICENSE-2.0");
        whitelist.add("http://www.apache.org/licenses/LICENSE-2.0.txt");
        whitelist.add("https://www.apache.org/licenses/LICENSE-2.0.txt");

        whitelist.add("http://www.gnu.org/licenses/lgpl.html");
        whitelist.add("https://www.gnu.org/licenses/lgpl.html");
        whitelist.add("http://www.gnu.org/licenses/lgpl.txt");
        whitelist.add("https://www.gnu.org/licenses/lgpl.txt");

        whitelist.add("http://www.mozilla.org/MPL/2.0/");
        whitelist.add("https://www.mozilla.org/MPL/2.0/");

        whitelist.add("http://opensource.org/licenses/MIT");
        whitelist.add("http://www.opensource.org/licenses/mit-license.php");
        whitelist.add("https://opensource.org/license/mit/");

        whitelist.add("http://www.eclipse.org/legal/epl-v10.html");
        whitelist.add("https://www.eclipse.org/legal/epl-v10.html");

        whitelist.add("https://glassfish.dev.java.net/public/CDDLv1.0.html");
        whitelist.add(
                "https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html");

        whitelist.add(
                "http://www.w3.org/Consortium/Legal/copyright-software-19980720");

        whitelist.add("http://www.gwtproject.org/terms.html");
        whitelist.add("https://www.gwtproject.org/terms.html");

        /*
         * License names used by some projects that define their license to be
         * something like to http://projectdomain.com/license, for which the
         * contents might change without notice
         */
        whitelist.add("BSD");
        whitelist.add("The MIT License");
        whitelist.add("MIT");
        whitelist.add("Apache License, Version 2.0");
        whitelist.add("ICU License");
    }

    private static final List<String> excludeDirs = Arrays.asList(".git",
            "bower_components", "node", "node_modules", "src",
            "generated-sources", "classes", "test-classes");

    private static class LicenseFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            if (excludeDirs.stream().anyMatch(dir::endsWith)) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            String path = file.toString();
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
                    Map<String, List<String>> unsupported = new HashMap<>();
                    if (!checkLicenses(license, unsupported)) {
                        Assert.fail(getErrorMessage(file, unsupported));
                    }
                }
            } catch (ParserConfigurationException | SAXException
                    | IOException exception) {
                Assert.fail("Cannot parse license file " + file);
            }
        }

        private String getErrorMessage(Path file,
                Map<String, List<String>> unsupportedLicenses) {
            StringBuilder builder = new StringBuilder("File ");
            builder.append(file).append(
                    " contains the following dependencies with licenses that have not been whitelisted: ");
            unsupportedLicenses.forEach((dependency, licenses) -> builder
                    .append("\n dependency '").append(dependency)
                    .append("' has licenses : ").append(licenses));
            return builder.toString();
        }

        private boolean checkLicenses(Node licenses,
                Map<String, List<String>> unsupportedLicenses) {
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
                    handleLicense(unsupportedLicenses, licenseNames, dependency,
                            child);
                }
            }
            return !licenseNames.isEmpty() || !hasLicenses;
        }

        private void handleLicense(
                Map<String, List<String>> unsupportedLicenses,
                List<String> licenseNames, String dependency, Node child) {
            String name = getTagContent(child, "name");
            String url = getTagContent(child, "url");
            if (!whitelist.contains(url) && !whitelist.contains(name)) {
                List<String> licenses = unsupportedLicenses
                        .computeIfAbsent(dependency, key -> new ArrayList<>());
                licenses.add(name + ": " + url);
            } else {
                licenseNames.add(name);
            }
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
