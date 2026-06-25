/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;

/**
 * Builder class for {@link InvocationRequest} objects.
 */
class InvocationRequestBuilder {

    private String groupId;
    private String artifactId;
    private String version;
    private String goal;

    InvocationRequestBuilder() {
    }

    InvocationRequestBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    InvocationRequestBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    InvocationRequestBuilder version(String version) {
        this.version = version;
        return this;
    }

    InvocationRequestBuilder goal(String goal) {
        this.goal = goal;
        return this;
    }

    InvocationRequest createInvocationRequest() {
        Objects.requireNonNull(groupId, "groupId is required");
        Objects.requireNonNull(artifactId, "artifactId is required");
        Objects.requireNonNull(version, "version is required");
        Objects.requireNonNull(goal, "goal is required");
        return new DefaultInvocationRequest()
                .setGoals(Collections.singletonList(String.format("%s:%s:%s:%s",
                        groupId, artifactId, version, goal)));
    }

}
