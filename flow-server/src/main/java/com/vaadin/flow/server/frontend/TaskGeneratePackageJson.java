/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;

import elemental.json.JsonObject;

/**
 * Creates the <code>package.json</code> if missing.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskGeneratePackageJson extends NodeUpdater {

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param options
     *            build options
     */
    TaskGeneratePackageJson(Options options) {
        super(null, options);
    }

    @Override
    public void execute() {
        try {
            modified = false;
            JsonObject mainContent = getPackageJson();
            modified = updateDefaultDependencies(mainContent);
            if (modified) {
                if (!mainContent.hasKey("type")
                        || !mainContent.getString("type").equals("module")) {
                    mainContent.put("type", "module");
                    log().info(
                            """
                                    Adding package.json type as module to enable ES6 modules which is now required.
                                    With this change sources need to use 'import' instead of 'require' for imports.
                                    """);
                }
                writePackageFile(mainContent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
