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

public class LoadingIndicatorConfigurationMapTest
        extends AbstractMapFeatureTest<LoadingIndicatorConfigurationMap> {
    private final LoadingIndicatorConfigurationMap map = createFeature();

    @Test
    public void defaults() {
        Assert.assertEquals(
                LoadingIndicatorConfigurationMap.FIRST_DELAY_DEFAULT,
                map.getFirstDelay());
        Assert.assertEquals(
                LoadingIndicatorConfigurationMap.SECOND_DELAY_DEFAULT,
                map.getSecondDelay());
        Assert.assertEquals(
                LoadingIndicatorConfigurationMap.THIRD_DELAY_DEFAULT,
                map.getThirdDelay());
        Assert.assertEquals(
                LoadingIndicatorConfigurationMap.DEFAULT_THEME_APPLIED_DEFAULT,
                map.isApplyDefaultTheme());
    }

    @Test
    public void setGetFirstDelay() {
        testInt(map, LoadingIndicatorConfigurationMap.FIRST_DELAY_KEY,
                map::setFirstDelay, map::getFirstDelay);
    }

    @Test
    public void setGetSecondDelay() {
        testInt(map, LoadingIndicatorConfigurationMap.SECOND_DELAY_KEY,
                map::setSecondDelay, map::getSecondDelay);
    }

    @Test
    public void setGetThirdDelay() {
        testInt(map, LoadingIndicatorConfigurationMap.THIRD_DELAY_KEY,
                map::setThirdDelay, map::getThirdDelay);
    }

    @Test
    public void setGetDefaultThemeApplied() {
        testBoolean(map,
                LoadingIndicatorConfigurationMap.DEFAULT_THEME_APPLIED_KEY,
                map::setApplyDefaultTheme, map::isApplyDefaultTheme);
    }
}
