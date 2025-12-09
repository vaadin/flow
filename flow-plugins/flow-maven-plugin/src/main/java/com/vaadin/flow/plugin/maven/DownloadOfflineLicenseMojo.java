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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import com.vaadin.pro.licensechecker.LocalOfflineKey;
import com.vaadin.pro.licensechecker.MachineId;
import com.vaadin.pro.licensechecker.OfflineKeyValidator;

/**
 * Goal that provides the URL to download a Vaadin offline license key.
 * <p>
 * This command displays a machine-specific URL that can be used to manually
 * download an offline license key. The offline license is tied to this
 * machine's hardware ID and must be saved manually to the file system
 * (~/.vaadin/offlineKey).
 * <p>
 * Unlike the online license (proKey), offline licenses work without internet
 * connectivity and are suitable for CI/CD environments and offline development.
 *
 * @since 25.0
 */
@Mojo(name = "download-offline-license", requiresProject = false)
public class DownloadOfflineLicenseMojo extends FlowModeAbstractMojo {

    @Override
    protected void executeInternal()
            throws MojoExecutionException, MojoFailureException {
        try {
            MachineId machineId = new MachineId();
            String offlineUrl = OfflineKeyValidator.getOfflineUrl(machineId);

            // Get the primary offline key location (first in the priority list)
            File[] locations = LocalOfflineKey.getLocations();
            String locationPath = locations.length > 0
                    ? locations[0].getAbsolutePath()
                    : "~/.vaadin/offlineKeyV2";

            getLog().info("========================================");
            getLog().info("Vaadin Offline License Download");
            getLog().info("========================================");
            getLog().info("");
            getLog().info(
                    "To download an offline license for this machine, visit:");
            getLog().info("");
            getLog().info("  " + offlineUrl);
            getLog().info("");
            getLog().info(
                    "The offline license will be tied to this machine's hardware ID.");
            getLog().info("After downloading, save the license file to:");
            getLog().info("  " + locationPath);
            getLog().info("");
            getLog().info(
                    "For CI/CD build servers, you can download a server license key");
            getLog().info("that works on any machine from:");
            getLog().info("  https://vaadin.com/myaccount/licenses");
            getLog().info("");
        } catch (Exception e) {
            throw new MojoFailureException(
                    "Failed to generate offline license URL", e);
        }
    }

}
