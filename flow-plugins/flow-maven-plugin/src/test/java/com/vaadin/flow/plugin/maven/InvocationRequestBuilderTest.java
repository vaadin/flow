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
