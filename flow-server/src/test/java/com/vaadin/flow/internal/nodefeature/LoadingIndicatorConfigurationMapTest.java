/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.DEFAULT_THEME_APPLIED_DEFAULT;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.DEFAULT_THEME_APPLIED_KEY;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.FIRST_DELAY_DEFAULT;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.FIRST_DELAY_KEY;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.SECOND_DELAY_DEFAULT;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.SECOND_DELAY_KEY;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.THIRD_DELAY_DEFAULT;
import static com.vaadin.flow.shared.internal.LoadingIndicatorConstants.THIRD_DELAY_KEY;

import org.junit.Assert;
import org.junit.Test;

public class LoadingIndicatorConfigurationMapTest
        extends AbstractMapFeatureTest<LoadingIndicatorConfigurationMap> {
    private final LoadingIndicatorConfigurationMap map = createFeature();

    @Test
    public void defaults() {
        Assert.assertEquals(FIRST_DELAY_DEFAULT, map.getFirstDelay());
        Assert.assertEquals(SECOND_DELAY_DEFAULT, map.getSecondDelay());
        Assert.assertEquals(THIRD_DELAY_DEFAULT, map.getThirdDelay());
        Assert.assertEquals(DEFAULT_THEME_APPLIED_DEFAULT,
                map.isApplyDefaultTheme());
    }

    @Test
    public void setGetFirstDelay() {
        testInt(map, FIRST_DELAY_KEY, map::setFirstDelay, map::getFirstDelay);
    }

    @Test
    public void setGetSecondDelay() {
        testInt(map, SECOND_DELAY_KEY, map::setSecondDelay,
                map::getSecondDelay);
    }

    @Test
    public void setGetThirdDelay() {
        testInt(map, THIRD_DELAY_KEY, map::setThirdDelay, map::getThirdDelay);
    }

    @Test
    public void setGetDefaultThemeApplied() {
        testBoolean(map, DEFAULT_THEME_APPLIED_KEY, map::setApplyDefaultTheme,
                map::isApplyDefaultTheme);
    }
}
