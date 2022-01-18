package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.shared.internal.PollConfigurationConstants;

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
        super.testInt(map, PollConfigurationConstants.POLL_INTERVAL_KEY,
                map::setPollInterval, map::getPollInterval);
    }
}
