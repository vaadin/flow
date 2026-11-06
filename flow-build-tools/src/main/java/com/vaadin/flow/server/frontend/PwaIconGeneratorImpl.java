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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.vaadin.flow.internal.PwaIconGenerator;

/**
 * Generates PWA icons with AWT, at build time for the production build and at
 * runtime during development.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class PwaIconGeneratorImpl implements PwaIconGenerator {

    private static final String HEADLESS_PROPERTY = "java.awt.headless";

    @Override
    public byte[] generateIcon(URL baseImage, int width, int height)
            throws IOException {
        if (System.getProperty(HEADLESS_PROPERTY) == null) {
            // set headless mode if the property is not explicitly set
            System.setProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
        }
        BufferedImage image;
        try (InputStream inputStream = baseImage.openStream()) {
            image = ImageIO.read(inputStream);
        }
        if (image == null) {
            throw new IOException("Cannot read PWA icon from " + baseImage);
        }
        BufferedImage icon = drawIconImage(image, width, height);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(icon, "png", stream);
            return stream.toByteArray();
        } finally {
            icon.flush();
            image.flush();
        }
    }

    /**
     * Draws a resized version of the given base image centered on a new image
     * of the specified dimensions. The top-left pixel of the base image is used
     * as the background fill color. The image is scaled down to fit within the
     * target dimensions while preserving its aspect ratio; upscaling is not
     * performed.
     *
     * @param baseImage
     *            the source image to resize
     * @param targetWidth
     *            the width of the resulting image in pixels
     * @param targetHeight
     *            the height of the resulting image in pixels
     * @return a new {@link BufferedImage} with the resized icon drawn centered
     */
    static BufferedImage drawIconImage(BufferedImage baseImage, int targetWidth,
            int targetHeight) {
        // Pick top-left pixel as fill color if needed for image
        // resizing
        int bgColor = baseImage.getRGB(0, 0);

        BufferedImage bimage = new BufferedImage(targetWidth, targetHeight,
                BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D graphics = bimage.createGraphics();

        try {
            // fill bg with fill-color
            graphics.setBackground(new Color(bgColor, true));
            graphics.clearRect(0, 0, targetWidth, targetHeight);

            // calculate ratio (bigger ratio) for resize
            float ratio = (float) baseImage.getWidth()
                    / (float) targetWidth > (float) baseImage.getHeight()
                            / (float) targetHeight
                                    ? (float) baseImage.getWidth()
                                            / (float) targetWidth
                                    : (float) baseImage.getHeight()
                                            / (float) targetHeight;

            // Forbid upscaling of image
            ratio = ratio > 1.0f ? ratio : 1.0f;

            // calculate sizes with ratio
            int newWidth = Math.round(baseImage.getHeight() / ratio);
            int newHeight = Math.round(baseImage.getWidth() / ratio);

            // draw rescaled img in the center of created image
            graphics.drawImage(
                    baseImage.getScaledInstance(newWidth, newHeight,
                            Image.SCALE_SMOOTH),
                    (targetWidth - newWidth) / 2,
                    (targetHeight - newHeight) / 2, null);
        } finally {
            graphics.dispose();
        }
        return bimage;
    }
}
