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

public class GenerateSBOMMojoTest {

    private String bomFilename;

    private File resourceOutputDirectory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GenerateSBOMMojo mojo;

    @Before
    public void setUp() throws Exception {
        this.mojo = new GenerateSBOMMojo();

        MavenProject project = Mockito.mock(MavenProject.class);
        File projectBase = temporaryFolder.getRoot();
        Mockito.when(project.getBasedir()).thenReturn(projectBase);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        bomFilename = new File(resourceOutputDirectory, "bom.json")
                .getAbsolutePath();

        // set Mojo properties
        ReflectionUtils.setVariableValueInObject(mojo, "projectType",
                "library");
        ReflectionUtils.setVariableValueInObject(mojo, "schemaVersion", "1.4");
        ReflectionUtils.setVariableValueInObject(mojo, "includeBomSerialNumber",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeCompileScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeProvidedScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeRuntimeScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeTestScope",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "includeSystemScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeLicenseText",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "outputReactorProjects",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "excludeTypes",
                new String[0]);
        ReflectionUtils.setVariableValueInObject(mojo, "excludeArtifactId",
                new String[0]);
        ReflectionUtils.setVariableValueInObject(mojo, "excludeGroupId",
                new String[0]);
        ReflectionUtils.setVariableValueInObject(mojo, "outputFormat", "json");
        ReflectionUtils.setVariableValueInObject(mojo, "outputName", "bom");
        ReflectionUtils.setVariableValueInObject(mojo, "outputDirectory",
                resourceOutputDirectory.getAbsolutePath());
    }

    @Test
    public void shouldGenerateSBOM() throws Exception {
        Assert.assertFalse(Files.exists(Paths.get(bomFilename)));
        mojo.execute();
        Assert.assertTrue(Files.exists(Paths.get(bomFilename)));
    }

}
