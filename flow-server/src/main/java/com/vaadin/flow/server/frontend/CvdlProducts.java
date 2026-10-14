/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.pro.licensechecker.Product;

import org.apache.commons.io.FileUtils;

import elemental.json.Json;
import elemental.json.JsonObject;

/** Utilities for commercial product handling. */
public class CvdlProducts {

    private static final String CVDL_PACKAGE_KEY = "cvdlName";

    /**
     * Returns product information if the given npm module refers to a Vaadin
     * commercial component.
     *
     * @param nodeModules
     *            the node modules folder
     * @param npmModule
     *            the name of the npm module to check
     */
    public static Product getProductIfCvdl(File nodeModules, String npmModule) {
        File packageJsonFile = new File(new File(nodeModules, npmModule),
                "package.json");
        if (!packageJsonFile.exists()) {
            return null;
        }

        try {
            JsonObject packageJson = Json.parse(FileUtils
                    .readFileToString(packageJsonFile, StandardCharsets.UTF_8));
            if (packageJson.hasKey(CVDL_PACKAGE_KEY)) {
                return new Product(packageJson.getString(CVDL_PACKAGE_KEY),
                        packageJson.getString("version"));
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to read package.json file " + packageJsonFile, e);
        }
    }

}
