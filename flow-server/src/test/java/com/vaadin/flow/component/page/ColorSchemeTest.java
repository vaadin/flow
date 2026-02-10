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
package com.vaadin.flow.component.page;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ColorSchemeTest {

    @Test
    public void getValue_returnsCorrectValue() {
        Assertions.assertEquals("light", ColorScheme.Value.LIGHT.getValue());
        Assertions.assertEquals("dark", ColorScheme.Value.DARK.getValue());
        Assertions.assertEquals("light dark",
                ColorScheme.Value.LIGHT_DARK.getValue());
        Assertions.assertEquals("dark light",
                ColorScheme.Value.DARK_LIGHT.getValue());
        Assertions.assertEquals("normal", ColorScheme.Value.NORMAL.getValue());
        Assertions.assertEquals("light dark",
                ColorScheme.Value.SYSTEM.getValue());
    }

    @Test
    public void getThemeValue_singleValue_returnsUnchanged() {
        Assertions.assertEquals("light",
                ColorScheme.Value.LIGHT.getThemeValue());
        Assertions.assertEquals("dark", ColorScheme.Value.DARK.getThemeValue());
        Assertions.assertEquals("normal",
                ColorScheme.Value.NORMAL.getThemeValue());
    }

    @Test
    public void getThemeValue_multiValue_replacesSpaceWithHyphen() {
        Assertions.assertEquals("light-dark",
                ColorScheme.Value.LIGHT_DARK.getThemeValue());
        Assertions.assertEquals("dark-light",
                ColorScheme.Value.DARK_LIGHT.getThemeValue());
        Assertions.assertEquals("light-dark",
                ColorScheme.Value.SYSTEM.getThemeValue());
    }

    @Test
    public void fromString_validValues_returnsCorrectEnum() {
        Assertions.assertEquals(ColorScheme.Value.LIGHT,
                ColorScheme.Value.fromString("light"));
        Assertions.assertEquals(ColorScheme.Value.DARK,
                ColorScheme.Value.fromString("dark"));
        Assertions.assertEquals(ColorScheme.Value.LIGHT_DARK,
                ColorScheme.Value.fromString("light dark"));
        Assertions.assertEquals(ColorScheme.Value.DARK_LIGHT,
                ColorScheme.Value.fromString("dark light"));
        Assertions.assertEquals(ColorScheme.Value.NORMAL,
                ColorScheme.Value.fromString("normal"));
    }

    @Test
    public void fromString_nullOrEmpty_returnsNormal() {
        Assertions.assertEquals(ColorScheme.Value.NORMAL,
                ColorScheme.Value.fromString(null));
        Assertions.assertEquals(ColorScheme.Value.NORMAL,
                ColorScheme.Value.fromString(""));
    }

    @Test
    public void fromString_unrecognizedValue_returnsNormal() {
        Assertions.assertEquals(ColorScheme.Value.NORMAL,
                ColorScheme.Value.fromString("invalid"));
        Assertions.assertEquals(ColorScheme.Value.NORMAL,
                ColorScheme.Value.fromString("light-dark"));
    }

    @Test
    public void fromString_lightDark_returnsLightDarkNotSystem() {
        // Ensure backward compatibility: parsing "light dark" returns
        // LIGHT_DARK
        Assertions.assertEquals(ColorScheme.Value.LIGHT_DARK,
                ColorScheme.Value.fromString("light dark"));
        // SYSTEM and LIGHT_DARK should be functionally equivalent
        Assertions.assertEquals(ColorScheme.Value.LIGHT_DARK.getValue(),
                ColorScheme.Value.SYSTEM.getValue());
    }
}
