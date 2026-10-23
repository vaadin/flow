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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

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
    void generateIcon_pngOfRequestedSizeFilledWithBaseImageCorner()
            throws IOException {
        URL baseImageUrl = getClass()
                .getResource("/META-INF/resources/icons/icon.png");
        BufferedImage baseImage = ImageIO.read(baseImageUrl);

        byte[] icon = new PwaIconGeneratorImpl().generateIcon(baseImageUrl, 120,
                70);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(icon));
        assertEquals(120, image.getWidth());
        assertEquals(70, image.getHeight());
        assertEquals(baseImage.getRGB(0, 0), image.getRGB(0, 0));
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
