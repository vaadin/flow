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
package com.vaadin.flow.server;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PwaIconTest {

    private static final int DEFAULT_BLUE = 0x0d72f8;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "My App|MA", "my app|MA",
            "Vaadin Flow Application|VF", "StarPass|SP", "Inventory|IN",
            "iPhone|IP", "X|X", "Café-Bar 2|CB", "' ** '|''" })
    void getInitials(String appName, String initials) {
        assertEquals(initials, PwaIcon.getInitials(appName));
    }

    @ParameterizedTest
    @CsvSource({ "#ffffff,0d72f8", "#FFF,0d72f8", "red,0d72f8",
            "#e0552e,e0552e", "#0a8,00aa88" })
    void getInitialsBackground(String themeColor, String rgb) {
        assertEquals(Integer.parseInt(rgb, 16),
                PwaIcon.getInitialsBackground(themeColor).getRGB() & 0xffffff);
    }

    @Test
    void drawDefaultBaseImage_initialsOnThemeColor() throws IOException {
        BufferedImage image = PwaIcon.drawDefaultBaseImage("My App", "#ffffff");

        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
        // The background fades to the theme color at the bottom
        assertEquals(DEFAULT_BLUE, image.getRGB(0, 511) & 0xffffff);
        // The initials are drawn in white across the middle
        long whitePixels = 0;
        for (int x = 0; x < 512; x++) {
            if ((image.getRGB(x, 256) & 0xffffff) == 0xffffff) {
                whitePixels++;
            }
        }
        assertTrue(whitePixels > 50,
                "Expected white initials in the middle, found " + whitePixels
                        + " white pixels");
    }

    @Test
    void drawDefaultBaseImage_noInitials_bundledLogo() throws IOException {
        BufferedImage image = PwaIcon.drawDefaultBaseImage("***", "#e0552e");

        // The bundled Vaadin logo, not a square in the theme color
        assertEquals(512, image.getWidth());
        assertNotEquals(0xe0552e, image.getRGB(0, 511) & 0xffffff);
    }
}
