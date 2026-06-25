/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
 * @since 24.10
 */
@Mojo(name = "download-license", requiresProject = false)
public class DownloadLicenseMojo extends AbstractMojo {

    private static final String PRODUCT_NAME = "vaadin-maven-download";
    private static final int TIMEOUT_SECONDS = 300; // 5 minutes

    @Parameter(defaultValue = "${mojoExecution}")
    MojoExecution mojoExecution;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
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
