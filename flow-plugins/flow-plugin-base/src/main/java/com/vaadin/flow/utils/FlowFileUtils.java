/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

/**
 * Util methods for files manipulation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public final class FlowFileUtils {
    private FlowFileUtils() {
    }

    /**
     * A {@link FileUtils#forceMkdir(File)} wrapper that handles
     * {@link IOException}.
     *
     * @param directory
     *            the directory to create, must not be {@code null}
     * @throws UncheckedIOException
     *             if {@link FileUtils#forceMkdir(File)} throws
     *             {@link IOException}
     */
    public static void forceMkdir(File directory) {
        try {
            FileUtils.forceMkdir(Objects.requireNonNull(directory));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to create directory '%s'", directory),
                    e);
        }
    }

    /**
     * Converts {@link File} to {@link URL} wrapping checked exception.
     *
     * @param file
     *            file to convert
     * @return corresponding {@link URL}
     * @throws IllegalArgumentException
     *             when fails to convert
     */
    public static URL convertToUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    String.format("Failed to convert file '%s' to URL", file),
                    e);
        }
    }
}
