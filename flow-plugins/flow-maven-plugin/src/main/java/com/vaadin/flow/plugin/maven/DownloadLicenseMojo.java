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
package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.LicenseChecker.DownloadOptions;
import com.vaadin.pro.licensechecker.LicenseException;
import com.vaadin.pro.licensechecker.LocalProKey;
import com.vaadin.pro.licensechecker.Product;

/**
 * Goal that downloads a Vaadin Pro license key by opening the browser and
 * waiting for the user to log in.
 * <p>
 * The downloaded license key is saved to the local file system
 * (~/.vaadin/proKey) and can be used for validating commercial Vaadin
 * components.
 *
 * @since 25.0
 */
@Mojo(name = "download-license", requiresProject = false)
public class DownloadLicenseMojo extends FlowModeAbstractMojo {

    private static final String PRODUCT_NAME = "vaadin-maven-download";
    private static final int TIMEOUT_SECONDS = 300; // 5 minutes

    @Override
    protected void executeInternal()
            throws MojoExecutionException, MojoFailureException {
        String version = getFlowVersion();

        // Check if we already have a proKey
        if (LocalProKey.get() != null) {
            getLog().info("A license key already exists at "
                    + LocalProKey.getLocation());
            getLog().info(
                    "Delete the existing key file if you want to download a new one.");
            return;
        }

        try {
            LicenseChecker.downloadLicense(new DownloadOptions(
                    new Product(PRODUCT_NAME, version), TIMEOUT_SECONDS));

            getLog().info("License key downloaded and saved successfully to "
                    + LocalProKey.getLocation());
        } catch (LicenseException e) {
            throw new MojoFailureException("Failed to download license key", e);
        }
    }

    /**
     * Gets the Flow version from the plugin version.
     */
    private String getFlowVersion() {
        if (mojoExecution != null) {
            return mojoExecution.getMojoDescriptor().getPluginDescriptor()
                    .getVersion();
        }
        // Fallback version when mojoExecution is not available
        return "0.0.1";
    }

}
