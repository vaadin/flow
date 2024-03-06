/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileIOUtils {

    private FileIOUtils() {
        // Utils only
    }

    private static Logger log() {
        return LoggerFactory.getLogger(FileIOUtils.class);
    }

    /**
     * Try determining the project folder from the classpath.
     *
     * @return A file referring to the project folder or null if the folder
     *         could not be determined
     */
    public static File getProjectFolderFromClasspath() {
        try {
            URL url = FileIOUtils.class.getClassLoader().getResource(".");
            if (url != null && url.getProtocol().equals("file")) {
                return getProjectFolderFromClasspath(url);
            }
        } catch (Exception e) {
            log().warn("Unable to determine project folder using classpath", e);
        }
        return null;

    }

    static File getProjectFolderFromClasspath(URL rootFolder)
            throws URISyntaxException {
        // URI decodes the path so that e.g. " " works correctly
        // Path.of makes windows paths work correctly
        Path path = Path.of(rootFolder.toURI());
        if (path.endsWith(Path.of("target", "classes"))) {
            return path.getParent().getParent().toFile();
        }

        return null;
    }

}
