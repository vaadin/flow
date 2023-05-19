package com.vaadin.flow.internal.nodefeature;

import org.junit.Assert;
import org.junit.Test;

public class PollConfigurationMapTest
        extends AbstractMapFeatureTest<PollConfigurationMap> {

    private final PollConfigurationMap map = createFeature();

    @Test
    public void setDefaultPollInterval() {
        Assert.assertEquals(-1, map.getPollInterval());
    }

    @Test
    public void setGetPollInterval() {
        super.testInt(map, PollConfigurationMap.POLL_INTERVAL_KEY,
                map::setPollInterval, map::getPollInterval);
    }
}
