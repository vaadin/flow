package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

public class MavenUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void basicInformationForStandalonePom() throws IOException {
        File pomXml = getPomXml("pom-standalone.xml");
        Document parse = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals("this.group", MavenUtils.getGroupId(parse));
        Assert.assertEquals("this-the-artifact",
                MavenUtils.getArtifactId(parse));
    }

    @Test
    public void basicInformationForPomWithParent() throws IOException {
        File pomXml = getPomXml("pom-with-parent.xml");
        Document parse = MavenUtils.parsePomFile(pomXml);
        Assert.assertEquals("this.group", MavenUtils.getGroupId(parse));
        Assert.assertEquals("this-the-artifact",
                MavenUtils.getArtifactId(parse));
    }

    private File getPomXml(String filename) throws IOException {
        File pomXml = temporaryFolder.newFile(filename);
        FileUtils.write(pomXml,
                IOUtils.toString(getClass().getResource("maven/" + filename),
                        StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        return pomXml;
    }

}
