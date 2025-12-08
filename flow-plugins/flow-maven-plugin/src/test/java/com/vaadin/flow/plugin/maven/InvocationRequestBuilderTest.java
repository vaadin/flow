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

import java.util.List;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.junit.Assert;
import org.junit.Test;

public class InvocationRequestBuilderTest {

    private final String groupId = "group.id";
    private final String artifactId = "artifact.id";
    private final String version = "1.0.1";
    private final String goal = "goal";

    @Test
    public void createInvocationRequest() {
        InvocationRequestBuilder requestBuilder = new InvocationRequestBuilder();
        InvocationRequest request = requestBuilder.groupId(groupId)
                .artifactId(artifactId).version(version).goal(goal)
                .createInvocationRequest();
        List<String> goals = request.getGoals();
        Assert.assertEquals(1, goals.size());

        String expectedGoal = String.format("%s:%s:%s:%s", groupId, artifactId,
                version, goal);
        String actualGoal = goals.get(0);
        Assert.assertEquals(expectedGoal, actualGoal);
    }

}
