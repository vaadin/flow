/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadingIndicatorConfigurationMapTest
        extends AbstractMapFeatureTest<LoadingIndicatorConfigurationMap> {
    private final LoadingIndicatorConfigurationMap map = createFeature();

    @Test
    public void defaults() {
        assertEquals(LoadingIndicatorConfigurationMap.FIRST_DELAY_DEFAULT,
                map.getFirstDelay());
        assertEquals(LoadingIndicatorConfigurationMap.SECOND_DELAY_DEFAULT,
                map.getSecondDelay());
        assertEquals(LoadingIndicatorConfigurationMap.THIRD_DELAY_DEFAULT,
                map.getThirdDelay());
        assertEquals(
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
