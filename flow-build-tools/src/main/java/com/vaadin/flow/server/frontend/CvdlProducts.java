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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.pro.licensechecker.Product;

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
     * @return product information if the npm module is a commercial component,
     *         or {@code null} otherwise
     */
    public static Product getProductIfCvdl(File nodeModules, String npmModule) {
        File packageJsonFile = new File(new File(nodeModules, npmModule),
                "package.json");
        if (!packageJsonFile.exists()) {
            return null;
        }

        try {
            JsonNode packageJson = JacksonUtils
                    .readTree(Files.readString(packageJsonFile.toPath()));
            if (packageJson.has(CVDL_PACKAGE_KEY)) {
                return new Product(packageJson.get(CVDL_PACKAGE_KEY).asString(),
                        packageJson.get("version").asString());
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to read package.json file " + packageJsonFile, e);
        }
    }

}
