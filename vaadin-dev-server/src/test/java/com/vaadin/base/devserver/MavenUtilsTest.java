/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenUtilsTest {

    @TempDir
    File temporaryFolder;

    private File mavenFolder;

    @BeforeEach
    void setupPoms() throws Exception {
        URL mavenTestResourceDirectory = getClass().getResource("maven");
        this.mavenFolder = Path.of(mavenTestResourceDirectory.toURI()).toFile();
    }

    @Test
    void basicInformationForStandalonePom() throws Exception {
        File pomXml = getPomXml("pom-standalone.xml");
        Document parse = MavenUtils.parsePomFile(pomXml);
        assertEquals("this.group", MavenUtils.getGroupId(parse));
        assertEquals("this-the-artifact", MavenUtils.getArtifactId(parse));
    }

    @Test
    void basicInformationForPomWithParent() throws Exception {
        File pomXml = getPomXml("standard-multimodule/module1/pom.xml");
        Document parse = MavenUtils.parsePomFile(pomXml);
        assertEquals("this.group", MavenUtils.getGroupId(parse));
        assertEquals("this-the-artifact", MavenUtils.getArtifactId(parse));
    }

    @Test
    void detectsPomIsPartOfASimpleMultimoduleProject() throws Exception {
        File parent = getPomXml("standard-multimodule/pom.xml");
        assertEquals(parent, MavenUtils.getParentPomOfMultiModuleProject(
                getPomXml("standard-multimodule/module1/pom.xml")));
    }

    @Test
    void detectsPomWithRelativePathIsPartOfASimpleMultimoduleProject()
            throws Exception {
        File parent = getPomXml("standard-multimodule/pom.xml");
        assertEquals(parent, MavenUtils.getParentPomOfMultiModuleProject(
                getPomXml("standard-multimodule/module2/pom.xml")));
    }

    @Test
    void detectsPomIsPartOfAComplexMultimoduleProject() throws Exception {
        File parent = getPomXml("complex-multimodule/pom-parent.xml");
        File parentPomOfMultiModuleProject = MavenUtils
                .getParentPomOfMultiModuleProject(getPomXml(
                        "complex-multimodule/module1/pom-with-parent.xml"));
        assertEquals(parent, parentPomOfMultiModuleProject);
    }

    @Test
    void findsModulesInSimpleMultiModulePom() throws Exception {
        File pomXml = getPomXml("standard-multimodule/pom.xml");
        Document pom = MavenUtils.parsePomFile(pomXml);
        assertEquals(List.of("module1", "module2"),
                MavenUtils.getModuleFolders(pom));
    }

    @Test
    void findsModulesInComplexMultiModulePom() throws Exception {
        File pomXml = getPomXml("complex-multimodule/pom-parent.xml");
        Document pom = MavenUtils.parsePomFile(pomXml);
        assertEquals(List.of("module1", "module2"),
                MavenUtils.getModuleFolders(pom));
    }

    @Test
    void findsNoModulesInStandalonePom() throws Exception {
        File pomXml = getPomXml("pom-standalone.xml");
        Document pom = MavenUtils.parsePomFile(pomXml);
        assertEquals(List.of(), MavenUtils.getModuleFolders(pom));
    }

    private File getPomXml(String filename) throws IOException {
        return new File(mavenFolder, filename);
    }

}
