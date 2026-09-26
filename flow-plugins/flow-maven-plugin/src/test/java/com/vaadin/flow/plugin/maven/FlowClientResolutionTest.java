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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests how a build gets the Flow client whose frontend sources it compiles
 * into the application bundle. An application does not depend on the client, so
 * a build resolves it, pinned to the version of the Flow server the project
 * resolves.
 */
class FlowClientResolutionTest {

    @TempDir
    Path tempDir;

    private PrepareFrontendMojo mojo;
    private MavenProject project;
    private RepositorySystem repositorySystem;
    private RepositorySystemSession repositorySession;
    private File resolvedClient;

    @BeforeEach
    void setup() throws Exception {
        mojo = new PrepareFrontendMojo();
        project = new MavenProject();
        resolvedClient = tempDir.resolve("flow-client-1.2.3.jar").toFile();

        repositorySystem = mock(RepositorySystem.class);
        when(repositorySystem.resolveArtifact(any(), any()))
                .thenAnswer(invocation -> {
                    ArtifactRequest request = invocation.getArgument(1);
                    ArtifactResult result = new ArtifactResult(request);
                    result.setArtifact(
                            request.getArtifact().setFile(resolvedClient));
                    return result;
                });

        repositorySession = mock(RepositorySystemSession.class);
        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(repositorySession);

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "session", session);
        mojo.setRepositorySystem(repositorySystem);
    }

    @Test
    void should_resolveTheClient_ofTheVersionOfTheServer() throws Exception {
        project.setRemoteArtifactRepositories(
                List.of(new MavenArtifactRepository("releases",
                        "https://example.com/maven",
                        new DefaultRepositoryLayout(),
                        new ArtifactRepositoryPolicy(),
                        new ArtifactRepositoryPolicy())));
        project.setArtifacts(Set.of(artifact("com.vaadin", "flow-server",
                "1.2.3", tempDir.resolve("flow-server-1.2.3.jar").toFile())));

        assertTrue(mojo.getJarFiles().contains(resolvedClient),
                "The build should have resolved the Flow client");

        ArgumentCaptor<RepositorySystemSession> session = ArgumentCaptor
                .forClass(RepositorySystemSession.class);
        ArgumentCaptor<ArtifactRequest> request = ArgumentCaptor
                .forClass(ArtifactRequest.class);
        verify(repositorySystem).resolveArtifact(session.capture(),
                request.capture());
        assertEquals("com.vaadin:flow-client:jar:1.2.3",
                request.getValue().getArtifact().toString(),
                "The client should be resolved at the version of the server "
                        + "the project resolves");
        List<RemoteRepository> repositories = project
                .getRemoteProjectRepositories();
        assertFalse(repositories.isEmpty(),
                "The project of the test should have a repository to resolve "
                        + "from");
        assertEquals(repositories, request.getValue().getRepositories(),
                "The client should be resolved from the repositories of the "
                        + "project");
        assertEquals(repositorySession, session.getValue(),
                "The client should be resolved in the repository session of "
                        + "the build");
    }

    @Test
    void should_keepTheClientOfTheProject_whenItHasOne() throws Exception {
        File projectClient = tempDir.resolve("flow-client-4.5.6.jar").toFile();
        project.setArtifacts(Set.of(
                artifact("com.vaadin", "flow-server", "1.2.3",
                        tempDir.resolve("flow-server-1.2.3.jar").toFile()),
                artifact("com.vaadin", "flow-client", "4.5.6", projectClient)));

        Set<File> jarFiles = mojo.getJarFiles();

        assertTrue(jarFiles.contains(projectClient),
                "The client of the project should be the one a build uses");
        assertFalse(jarFiles.contains(resolvedClient),
                "A build should not add a second Flow client beside the one "
                        + "the project resolves");
        verifyNoInteractions(repositorySystem);
    }

    @Test
    void should_resolveNothing_withoutAServerToTakeTheVersionFrom()
            throws Exception {
        File dependency = tempDir.resolve("dependency-1.0.jar").toFile();
        project.setArtifacts(Set
                .of(artifact("com.example", "dependency", "1.0", dependency)));

        assertEquals(Set.of(dependency), mojo.getJarFiles(),
                "A project without the Flow server has no Flow version to "
                        + "resolve a client of");
        verifyNoInteractions(repositorySystem);
    }

    @Test
    void should_failTheBuild_whenTheClientCanNotBeResolved() throws Exception {
        project.setArtifacts(Set.of(artifact("com.vaadin", "flow-server",
                "1.2.3", tempDir.resolve("flow-server-1.2.3.jar").toFile())));
        doThrow(new ArtifactResolutionException(List.of()))
                .when(repositorySystem).resolveArtifact(any(), any());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> mojo.getJarFiles());

        assertTrue(
                exception.getMessage().contains("com.vaadin:flow-client:1.2.3"),
                "The build should name the client it can not resolve, was "
                        + exception.getMessage());
    }

    private static Artifact artifact(String groupId, String artifactId,
            String version, File file) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler();
        handler.setAddedToClasspath(true);
        Artifact artifact = new DefaultArtifact(groupId, artifactId, version,
                "compile", "jar", null, handler);
        artifact.setFile(file);
        return artifact;
    }

}
