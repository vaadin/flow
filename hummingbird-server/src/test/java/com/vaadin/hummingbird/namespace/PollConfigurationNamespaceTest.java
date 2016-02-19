package com.vaadin.hummingbird.namespace;

import org.junit.Assert;
import org.junit.Test;

public class PollConfigurationNamespaceTest
        extends AbstractMapNamespaceTest<PollConfigurationNamespace> {

    private final PollConfigurationNamespace namespace = createNamespace();

    @Test
    public void setDefaultPollInterval() {
        Assert.assertEquals(-1, namespace.getPollInterval());
    }

    @Test
    public void setGetPollInterval() {
        super.testInt(namespace, PollConfigurationNamespace.POLL_INTERVAL_KEY,
                namespace::setPollInterval, namespace::getPollInterval);
    }
}
