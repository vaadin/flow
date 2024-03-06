/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.installer;

import java.io.File;

/**
 * Handle extracting file archives.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
interface ArchiveExtractor {

    /**
     * Extract archive contents to given destination.
     *
     * @param archive
     *            archive file to extract
     * @param destinationDirectory
     *            destination directory to extract files to
     * @throws ArchiveExtractionException
     *             exception thrown for failure during extraction
     */
    void extract(File archive, File destinationDirectory)
            throws ArchiveExtractionException;
}
