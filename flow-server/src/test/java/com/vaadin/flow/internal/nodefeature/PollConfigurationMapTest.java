/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
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
