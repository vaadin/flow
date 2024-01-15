package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

public class GenerateNpmBOMMojoTest {

    private String bomFilename;

    private File resourceOutputDirectory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GenerateNpmBOMMojo mojo;

    @Before
    public void setUp() throws Exception {
        this.mojo = new GenerateNpmBOMMojo();

        MavenProject project = Mockito.mock(MavenProject.class);
        File projectBase = temporaryFolder.getRoot();
        Mockito.when(project.getBasedir()).thenReturn(projectBase);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        bomFilename = new File(resourceOutputDirectory, "bom-npm.json")
                .getAbsolutePath();

        String manifestFilePath = "src/test/resources/package.json";
        // set Mojo properties
        ReflectionUtils.setVariableValueInObject(mojo, "outputFormat", "json");
        ReflectionUtils.setVariableValueInObject(mojo, "outputFilePath",
                bomFilename);
        ReflectionUtils.setVariableValueInObject(mojo, "packageManifest",
                manifestFilePath);
        ReflectionUtils.setVariableValueInObject(mojo, "specVersion", "1.4");
    }

    @Test
    public void shouldGenerateSBOM() throws Exception {
        Assert.assertFalse(Files.exists(Paths.get(bomFilename)));
        mojo.execute();
        Assert.assertTrue(Files.exists(Paths.get(bomFilename)));
    }

}
