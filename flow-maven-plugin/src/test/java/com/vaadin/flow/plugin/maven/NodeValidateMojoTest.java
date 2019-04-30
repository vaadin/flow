package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;

public class NodeValidateMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final NodeValidateMojo mojo = new NodeValidateMojo();
    private File projectFrontendResourcesDirectory;
    private File nodeModulesPath;

    @Before
    public void setup() throws Exception {
        File projectBase = temporaryFolder.getRoot();

        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getBasedir()).thenReturn(projectBase);

        projectFrontendResourcesDirectory = new File(projectBase,
                "flow_resources");

        Assert.assertTrue("Failed to create a test project resources",
                projectFrontendResourcesDirectory.mkdirs());
        Assert.assertTrue("Failed to create a test project file",
                new File(projectFrontendResourcesDirectory,
                        "test_project_resource.js").createNewFile());

        nodeModulesPath = new File(projectBase, "node_modules");

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeModulesPath", nodeModulesPath);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendResourcesDirectory", projectFrontendResourcesDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "jarResourcePathsToCopy", RESOURCES_FRONTEND_DEFAULT);
        ReflectionUtils.setVariableValueInObject(mojo, "includes", "**/*.js,**/*.css");
    }

    @Test
    public void should_copyProjectFrontendResources() {
        Assert.assertTrue("There should be no modules before the mojo is run",
                gatherFiles(nodeModulesPath).isEmpty());
        mojo.execute();

        Set<String> projectFrontendResources = Stream
                .of(projectFrontendResourcesDirectory.listFiles())
                .map(File::getName).collect(Collectors.toSet());
        List<File> filesInNodeModules = gatherFiles(nodeModulesPath);

        Assert.assertEquals(
                "All project resources should be copied into the node_modules",
                projectFrontendResources.size(), filesInNodeModules.size());

        filesInNodeModules.forEach(file -> Assert.assertTrue(String.format(
                "Expected the copied file '%s' to be in the project resources",
                file), projectFrontendResources.contains(file.getName())));
    }

    private List<File> gatherFiles(File root) {
        if (root.isFile()) {
            return Collections.singletonList(root);
        } else {
            File[] subdirectoryFiles = root.listFiles();
            if (subdirectoryFiles != null) {
                List<File> files = new ArrayList<>();
                for (File subdirectoryFile : subdirectoryFiles) {
                    files.addAll(gatherFiles(subdirectoryFile));
                }
                return files;
            }
            return Collections.emptyList();
        }
    }
}
