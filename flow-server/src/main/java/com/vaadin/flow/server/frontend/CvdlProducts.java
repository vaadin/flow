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
import java.nio.charset.StandardCharsets;

import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.LocalOfflineKey;
import com.vaadin.pro.licensechecker.LocalProKey;
import com.vaadin.pro.licensechecker.Product;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static boolean includeInFallbackBundle(String module,
            File nodeModules) {
        if (module.startsWith(".") || module.startsWith("Frontend/")) {
            // Project internal file
            return true;
        }

        String npmModule = getNpmModule(module);
        if (npmModule == null) {
            // Unclear when this would happen
            return true;
        }

        Product product = CvdlProducts.getProductIfCvdl(nodeModules, npmModule);
        if (product != null) {
            if (LocalProKey.get() == null && LocalOfflineKey.get() == null) {
                // No proKey, do not bother free users with a license check
                getLogger().debug(
                        "No pro key or offline key found. Dropping '{}' from the fallback bundle without asking for validation",
                        module);
                return false;
            } else {
                try {
                    LicenseChecker.checkLicense(product.getName(),
                            product.getVersion(), BuildType.PRODUCTION);
                    return true;
                } catch (Exception e) {
                    // Silently drop from the fallback bundle (it is a
                    // production build).
                    // Otherwise we would bother all free users with a license
                    // check
                    getLogger().debug(
                            "License check failed. Dropping '{}' from the fallback bundle",
                            module, e);
                    return false;
                }
            }
        }
        return true;
    }

    private static String getNpmModule(String module) {
        // npm modules are either @org/pkg or pkg
        String[] parts = module.split("/", -1);
        if (parts.length < 2) {
            // What would this be?
            return null;
        }
        if (parts[0].startsWith("@")) {
            return parts[0] + "/" + parts[1];
        } else {
            return parts[0];
        }

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CvdlProducts.class);
    }

}
