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
