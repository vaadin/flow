package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.util.Collections;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;

/**
 * Builder class for {@link InvocationRequest} objects.
 */
public class InvocationRequestBuilder {

    private String groupId;
    private String artifactId;
    private String version;
    private String goal;

    public InvocationRequestBuilder() {
    }

    public InvocationRequestBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public InvocationRequestBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public InvocationRequestBuilder version(String version) {
        this.version = version;
        return this;
    }

    public InvocationRequestBuilder goal(String goal) {
        this.goal = goal;
        return this;
    }

    public InvocationRequest createInvocationRequest() {
        if (groupId == null) {
            throw new IllegalStateException(
                    "Cannot create invocation request without a groupId");
        }
        if (artifactId == null) {
            throw new IllegalStateException(
                    "Cannot create invocation request without a artifactId");
        }
        if (version == null) {
            throw new IllegalStateException(
                    "Cannot create invocation request without a version");
        }
        if (goal == null) {
            throw new IllegalStateException(
                    "Cannot create invocation request without a goal");
        }
        InvocationRequest request = new DefaultInvocationRequest();
        // needed for tests
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null) {
            request.setMavenHome(new File(mavenHome));
        }
        request.setGoals(Collections.singletonList(String.format("%s:%s:%s:%s",
                groupId, artifactId, version, goal)));
        return request;
    }

}
