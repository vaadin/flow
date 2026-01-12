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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

public class MavenUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File mavenFolder;

    @Before
    public void setupPoms() throws Exception {
        URL mavenTestResourceDirectory = getClass().getResource("maven");
        this.mavenFolder = Path.of(mavenTestResourceDirectory.toURI()).toFile();
    }

    @Test
    public void basicInformationForStandalonePom() throws Exception {
        File pomXml = getPomXml("pom-standalone.xml");
        Document parse = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals("this.group", MavenUtils.getGroupId(parse));
        Assert.assertEquals("this-the-artifact",
                MavenUtils.getArtifactId(parse));
    }

    @Test
    public void basicInformationForPomWithParent() throws Exception {
        File pomXml = getPomXml("standard-multimodule/module1/pom.xml");
        Document parse = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals("this.group", MavenUtils.getGroupId(parse));
        Assert.assertEquals("this-the-artifact",
                MavenUtils.getArtifactId(parse));
    }

    @Test
    public void detectsPomIsPartOfASimpleMultimoduleProject() throws Exception {
        File parent = getPomXml("standard-multimodule/pom.xml");
        Assert.assertEquals(parent, MavenUtils.getParentPomOfMultiModuleProject(
                getPomXml("standard-multimodule/module1/pom.xml")));
    }

    @Test
    public void detectsPomWithRelativePathIsPartOfASimpleMultimoduleProject()
            throws Exception {
        File parent = getPomXml("standard-multimodule/pom.xml");
        Assert.assertEquals(parent, MavenUtils.getParentPomOfMultiModuleProject(
                getPomXml("standard-multimodule/module2/pom.xml")));
    }

    @Test
    public void detectsPomIsPartOfAComplexMultimoduleProject()
            throws Exception {
        File parent = getPomXml("complex-multimodule/pom-parent.xml");
        File parentPomOfMultiModuleProject = MavenUtils
                .getParentPomOfMultiModuleProject(getPomXml(
                        "complex-multimodule/module1/pom-with-parent.xml"));
        Assert.assertEquals(parent, parentPomOfMultiModuleProject);
    }

    @Test
    public void findsModulesInSimpleMultiModulePom() throws Exception {
        File pomXml = getPomXml("standard-multimodule/pom.xml");
        Document pom = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals(List.of("module1", "module2"),
                MavenUtils.getModuleFolders(pom));
    }

    @Test
    public void findsModulesInComplexMultiModulePom() throws Exception {
        File pomXml = getPomXml("complex-multimodule/pom-parent.xml");
        Document pom = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals(List.of("module1", "module2"),
                MavenUtils.getModuleFolders(pom));
    }

    @Test
    public void findsNoModulesInStandalonePom() throws Exception {
        File pomXml = getPomXml("pom-standalone.xml");
        Document pom = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals(List.of(), MavenUtils.getModuleFolders(pom));
    }

    private File getPomXml(String filename) throws IOException {
        return new File(mavenFolder, filename);
    }

}
