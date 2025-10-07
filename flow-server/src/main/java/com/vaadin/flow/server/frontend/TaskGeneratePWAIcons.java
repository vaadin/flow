/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaIcon;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Generates necessary PWA icons.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskGeneratePWAIcons implements FallibleCommand {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TaskGeneratePWAIcons.class);
    private static final String HEADLESS_PROPERTY = "java.awt.headless";

    private final Path generatedIconsPath;
    private final PwaConfiguration pwaConfiguration;
    private final ClassFinder classFinder;

    public TaskGeneratePWAIcons(Options options,
            PwaConfiguration pwaConfiguration) {
        this.pwaConfiguration = pwaConfiguration;
        generatedIconsPath = options.getWebappResourcesDirectory().toPath()
                .resolve(Constants.VAADIN_PWA_ICONS);
        this.classFinder = options.getClassFinder();
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (!pwaConfiguration.isEnabled()) {
            return;
        }
        URL iconURL = findIcon(pwaConfiguration);
        if (iconURL == null) {
            LOGGER.warn(
                    "Skipping PWA icons generation because icon '{}' cannot be found in classpath",
                    pwaConfiguration.getIconPath());
            return;
        }

        String headless = System.getProperty(HEADLESS_PROPERTY);
        if (headless == null) {
            // set headless mode if the property is not explicitly set
            System.setProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
        }

        LOGGER.debug("Generating PWA icons from '{}'",
                pwaConfiguration.getIconPath());

        try {
            BufferedImage baseImage = loadBaseImage(iconURL);
            createGeneratedIconsFolder();

            ExecutorService executor = Executors.newFixedThreadPool(4);
            CompletableFuture<?>[] iconsGenerators = PwaRegistry
                    .getIconTemplates(pwaConfiguration.getIconPath()).stream()
                    .map(icon -> new InternalPwaIcon(icon, baseImage))
                    .map(this::generateIcon)
                    .map(task -> CompletableFuture.runAsync(task, executor))
                    .toArray(CompletableFuture[]::new);
            try {
                CompletableFuture.allOf(iconsGenerators).join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof UncheckedIOException uncheckedIOException) {
                    throw new ExecutionFailedException(
                            "PWA icons generation failed",
                            uncheckedIOException.getCause());
                }
                throw new ExecutionFailedException(
                        "PWA icons generation failed", cause);
            } catch (CancellationException ex) {
                throw new ExecutionFailedException(
                        "PWA icons generation failed", ex);
            } finally {
                executor.shutdown();
            }
        } finally {
            if (headless == null) {
                System.clearProperty(HEADLESS_PROPERTY);
            } else if (!headless.equals(Boolean.TRUE.toString())) {
                System.setProperty(HEADLESS_PROPERTY, headless);
            }
        }
        LOGGER.info("PWA icons generated");
    }

    private void createGeneratedIconsFolder() throws ExecutionFailedException {
        try {
            Path generatedPath = generatedIconsPath
                    .resolve(Path.of(pwaConfiguration.getIconPath().replace('/',
                            File.separatorChar)))
                    .getParent();
            Files.createDirectories(generatedPath);
        } catch (IOException e) {
            throw new ExecutionFailedException(
                    "Cannot create PWA generated icons folder "
                            + generatedIconsPath,
                    e);
        }
    }

    private static BufferedImage loadBaseImage(URL iconURL)
            throws ExecutionFailedException {
        BufferedImage baseImage;
        try (InputStream inputStream = iconURL.openStream()) {
            baseImage = ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new ExecutionFailedException(
                    "Cannot load PWA icon from " + iconURL, e);
        }
        if (baseImage == null) {
            throw new ExecutionFailedException(
                    "Cannot load PWA icon from " + iconURL);
        }
        return baseImage;
    }

    private URL findIcon(PwaConfiguration pwaConfiguration) {
        URL iconURL = classFinder.getResource(pwaConfiguration.getIconPath());
        if (iconURL == null) {
            iconURL = classFinder.getResource(
                    "META-INF/resources/" + pwaConfiguration.getIconPath());
        }
        if (iconURL == null) {
            iconURL = BootstrapHandler.class.getResource("default-logo.png");
            if (iconURL == null) {
                LOGGER.warn(
                        "PWA icon '{}' cannot be found in classpath, fallback to default icon.",
                        pwaConfiguration.getIconPath());
            }
        }
        return iconURL;
    }

    private Runnable generateIcon(InternalPwaIcon icon) {
        Path iconPath = generatedIconsPath.resolve(icon.getRelHref()
                .substring(1).replace('/', File.separatorChar));
        return () -> {
            try (OutputStream os = Files.newOutputStream(iconPath)) {
                icon.write(os);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private static class InternalPwaIcon extends PwaIcon {
        private final BufferedImage baseImage;

        public InternalPwaIcon(PwaIcon icon, BufferedImage baseImage) {
            super(icon);
            this.baseImage = baseImage;
        }

        @Override
        protected BufferedImage getBaseImage() {
            return baseImage;
        }

    }
}
