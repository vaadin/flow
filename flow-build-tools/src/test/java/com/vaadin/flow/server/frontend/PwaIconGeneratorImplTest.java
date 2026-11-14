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
package com.vaadin.flow.server.frontend;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.PwaIconGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PwaIconGeneratorImplTest {

    @Test
    void generatorIsRegisteredAsService() {
        List<PwaIconGenerator> generators = ServiceLoader
                .load(PwaIconGenerator.class).stream()
                .map(ServiceLoader.Provider::get).toList();

        assertEquals(1, generators.size());
        assertInstanceOf(PwaIconGeneratorImpl.class, generators.get(0));
    }

    @Test
    void generateIcon_baseImageDrawnCenteredOnCornerColor(@TempDir Path dir)
            throws IOException {
        // A blue image whose top-left pixel is white, which is the fill color
        BufferedImage baseImage = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_ARGB);
        int blue = Color.BLUE.getRGB();
        int white = Color.WHITE.getRGB();
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                baseImage.setRGB(x, y, blue);
            }
        }
        baseImage.setRGB(0, 0, white);
        Path baseImageFile = dir.resolve("logo.png");
        ImageIO.write(baseImage, "png", baseImageFile.toFile());

        byte[] icon = new PwaIconGeneratorImpl()
                .generateIcon(baseImageFile.toUri().toURL(), 30, 20);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(icon));
        assertEquals(30, image.getWidth());
        assertEquals(20, image.getHeight());
        assertEquals(white, image.getRGB(0, 0));
        assertEquals(white, image.getRGB(29, 19));
        assertEquals(blue, image.getRGB(15, 10));
    }

    @Test
    void generateIcon_notAnImage_throws() {
        URL notAnImage = getClass().getResource(
                "/" + PwaIconGeneratorImplTest.class.getName().replace('.', '/')
                        + ".class");

        assertThrows(IOException.class, () -> new PwaIconGeneratorImpl()
                .generateIcon(notAnImage, 16, 16));
    }
}
