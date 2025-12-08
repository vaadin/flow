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
package com.vaadin.flow.plugin.maven;

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
