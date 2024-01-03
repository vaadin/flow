/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.util.Collections;

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
