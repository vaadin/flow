package com.vaadin.hummingbird.namespace;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;

public class PollConfigurationNamespaceTest {

    private StateNode node = new StateNode(PollConfigurationNamespace.class);
    private final PollConfigurationNamespace namespace = new PollConfigurationNamespace(
            node);

    @Test
    public void setDefaultPollInterval() {
        Assert.assertEquals(-1, namespace.getPollInterval());
    }

    @Test
    public void setGetPollInterval() {
        namespace.setPollInterval(10);
        Assert.assertEquals(10, namespace.getPollInterval());
        Assert.assertEquals(10,
                namespace.get(PollConfigurationNamespace.POLL_INTERVAL_KEY));
        namespace.put(PollConfigurationNamespace.POLL_INTERVAL_KEY, 0);
        Assert.assertEquals(0, namespace.getPollInterval());
    }
}
